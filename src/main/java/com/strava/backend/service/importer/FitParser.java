package com.strava.backend.service.importer;

import com.strava.backend.enums.ActivityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic FIT file parser supporting Garmin/ANT+ binary format.
 * Handles the most common record types: file_id, session, record (GPS trackpoints).
 */
@Slf4j
@Component
public class FitParser implements ActivityFileParser {

    // FIT epoch starts at 1989-12-31T00:00:00Z
    private static final long FIT_EPOCH_OFFSET = 631065600L;

    // Global Message Numbers
    private static final int MESG_SESSION = 18;
    private static final int MESG_RECORD = 20;

    // Common field definition numbers for record messages
    private static final int FIELD_TIMESTAMP = 253;
    private static final int FIELD_POSITION_LAT = 0;
    private static final int FIELD_POSITION_LONG = 1;
    private static final int FIELD_ALTITUDE = 2;
    private static final int FIELD_HEART_RATE = 3;
    private static final int FIELD_CADENCE = 4;
    private static final int FIELD_DISTANCE = 5;
    private static final int FIELD_SPEED = 6;
    private static final int FIELD_POWER = 7;

    // Session fields
    private static final int SESSION_SPORT = 5;
    private static final int SESSION_TOTAL_ELAPSED_TIME = 7;
    private static final int SESSION_TOTAL_DISTANCE = 9;
    private static final int SESSION_TOTAL_CALORIES = 11;
    private static final int SESSION_AVG_HEART_RATE = 16;
    private static final int SESSION_MAX_HEART_RATE = 17;
    private static final int SESSION_AVG_CADENCE = 18;
    private static final int SESSION_AVG_POWER = 20;
    private static final int SESSION_TOTAL_ASCENT = 22;
    private static final int SESSION_MAX_SPEED = 14;

    @Override
    public ParsedActivity parse(InputStream input) throws Exception {
        DataInputStream dis = new DataInputStream(input);

        // Parse FIT file header. Per the FIT SDK, multi-byte fields in the header
        // (profile_version, data_size, header CRC) are encoded LITTLE-ENDIAN.
        int headerSize = dis.readUnsignedByte();
        int protocolVersion = dis.readUnsignedByte();
        int profileVersion = readUint16LE(dis);
        long dataSize = readUint32LE(dis);
        byte[] dataType = new byte[4];
        dis.readFully(dataType);
        String signature = new String(dataType);

        if (!".FIT".equals(signature)) {
            throw new IllegalArgumentException("Invalid FIT file signature: " + signature);
        }

        // Skip remaining header bytes (CRC if header is 14 bytes)
        if (headerSize > 12) {
            dis.skipBytes(headerSize - 12);
        }

        // Parse data records
        Map<Integer, List<FieldDef>> localDefinitions = new HashMap<>();
        List<GpsPoint> points = new ArrayList<>();
        Map<String, Object> sessionData = new HashMap<>();
        long bytesRead = 0;

        while (bytesRead < dataSize) {
            int recordHeader = dis.readUnsignedByte();
            bytesRead++;

            boolean isCompressedTimestamp = (recordHeader & 0x80) != 0;
            int localMessageType;
            boolean isDefinition;
            boolean hasDeveloperFields;

            if (isCompressedTimestamp) {
                // Compressed timestamp header layout:
                //   bit 7  = 1
                //   bits 5-6 = local message type (0-3 only)
                //   bits 0-4 = time offset (5 bits)
                localMessageType = (recordHeader >> 5) & 0x03;
                isDefinition = false;
                hasDeveloperFields = false;
            } else {
                // Normal record header:
                //   bit 7  = 0
                //   bit 6  = definition flag
                //   bit 5  = developer data flag
                //   bit 4  = reserved
                //   bits 0-3 = local message type
                isDefinition = (recordHeader & 0x40) != 0;
                hasDeveloperFields = (recordHeader & 0x20) != 0;
                localMessageType = recordHeader & 0x0F;
            }

            log.debug("FIT record at byte {}: header=0x{} def={} devFields={} compressedTs={} localType={}",
                    bytesRead - 1, String.format("%02x", recordHeader),
                    isDefinition, hasDeveloperFields, isCompressedTimestamp, localMessageType);

            if (isDefinition) {
                bytesRead += parseDefinitionMessage(dis, localMessageType, localDefinitions, hasDeveloperFields);
            } else {
                List<FieldDef> fields = localDefinitions.get(localMessageType);
                if (fields == null) {
                    log.warn("FIT parser: no definition for local message type {} at byte {}, aborting",
                            localMessageType, bytesRead - 1);
                    break;
                }
                bytesRead += parseDataMessage(dis, fields, points, sessionData);
            }
        }

        return buildParsedActivity(points, sessionData);
    }

    @Override
    public boolean supports(String fileExtension) {
        return "fit".equalsIgnoreCase(fileExtension);
    }

    private long parseDefinitionMessage(DataInputStream dis, int localMessageType,
                                        Map<Integer, List<FieldDef>> definitions,
                                        boolean hasDeveloperFields) throws Exception {
        long bytes = 0;
        dis.readUnsignedByte(); // reserved
        bytes++;

        int architecture = dis.readUnsignedByte(); // 0 = little-endian, 1 = big-endian
        bytes++;

        int globalMessageNumber = architecture == 0
                ? readUint16LE(dis) : readUint16(dis);
        bytes += 2;

        int numFields = dis.readUnsignedByte();
        bytes++;

        List<FieldDef> fields = new ArrayList<>();
        for (int i = 0; i < numFields; i++) {
            int fieldDefNum = dis.readUnsignedByte();
            int fieldSize = dis.readUnsignedByte();
            int baseType = dis.readUnsignedByte();
            fields.add(new FieldDef(fieldDefNum, fieldSize, baseType, globalMessageNumber, architecture));
            bytes += 3;
        }

        // Developer field definitions follow if the dev-fields bit was set in the record header.
        // We can't decode developer fields meaningfully (their semantics live in developer_data_id /
        // field_description messages), but we still need to consume their bytes from the stream
        // AND track their sizes so subsequent data messages stay aligned.
        if (hasDeveloperFields) {
            int numDevFields = dis.readUnsignedByte();
            bytes++;
            for (int i = 0; i < numDevFields; i++) {
                int devFieldDefNum = dis.readUnsignedByte();
                int devFieldSize = dis.readUnsignedByte();
                int devFieldDataIdx = dis.readUnsignedByte();
                // Store as a placeholder field with a synthetic high definition number so we
                // never collide with normal field def numbers. We use baseType = byte array so
                // decodeField returns null and parseDataMessage just consumes the bytes.
                fields.add(new FieldDef(1000 + devFieldDefNum, devFieldSize, 0x0D, globalMessageNumber, architecture));
                bytes += 3;
                // touch idx to silence unused warning
                if (devFieldDataIdx < 0) {
                    log.trace("dev field data idx: {}", devFieldDataIdx);
                }
            }
        }

        log.debug("FIT def msg: localType={} globalMsg={} fields={} devFields={}",
                localMessageType, globalMessageNumber, numFields, hasDeveloperFields);

        definitions.put(localMessageType, fields);
        return bytes;
    }

    private long parseDataMessage(DataInputStream dis, List<FieldDef> fields,
                                  List<GpsPoint> points, Map<String, Object> sessionData) throws Exception {
        long bytes = 0;
        Map<Integer, Object> fieldValues = new HashMap<>();
        int globalMessageNumber = fields.isEmpty() ? -1 : fields.getFirst().globalMessageNumber;

        for (FieldDef field : fields) {
            byte[] data = new byte[field.size];
            dis.readFully(data);
            bytes += field.size;

            Object value = decodeField(data, field);
            if (value != null) {
                fieldValues.put(field.defNum, value);
            }
        }

        if (globalMessageNumber == MESG_RECORD) {
            processRecordMessage(fieldValues, points);
        } else if (globalMessageNumber == MESG_SESSION) {
            processSessionMessage(fieldValues, sessionData);
        }

        return bytes;
    }

    private Object decodeField(byte[] data, FieldDef field) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(field.architecture == 0 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        // The high bit of the base type byte is the endian flag; the low 5 bits identify the type.
        int baseType = field.baseType & 0x1F;
        try {
            return switch (baseType) {
                case 0 -> {
                    int v = buf.get() & 0xFF;
                    yield v == 0xFF ? null : v;             // enum, invalid = 0xFF
                }
                case 1 -> {
                    int v = buf.get();
                    yield v == 0x7F ? null : v;             // sint8, invalid = 0x7F
                }
                case 2 -> {
                    int v = buf.get() & 0xFF;
                    yield v == 0xFF ? null : v;             // uint8, invalid = 0xFF
                }
                case 3 -> {
                    int v = buf.getShort();
                    yield v == 0x7FFF ? null : v;           // sint16, invalid = 0x7FFF
                }
                case 4 -> {
                    int v = buf.getShort() & 0xFFFF;
                    yield v == 0xFFFF ? null : v;           // uint16, invalid = 0xFFFF
                }
                case 5 -> {
                    int v = buf.getInt();
                    yield v == 0x7FFFFFFF ? null : v;       // sint32, invalid = 0x7FFFFFFF
                }
                case 6 -> {
                    long v = buf.getInt() & 0xFFFFFFFFL;
                    yield v == 0xFFFFFFFFL ? null : v;      // uint32, invalid = 0xFFFFFFFF
                }
                case 10 -> {
                    int v = buf.get() & 0xFF;
                    yield v == 0 ? null : v;                // uint8z, invalid = 0
                }
                case 11 -> {
                    int v = buf.getShort() & 0xFFFF;
                    yield v == 0 ? null : v;                // uint16z, invalid = 0
                }
                case 12 -> {
                    long v = buf.getInt() & 0xFFFFFFFFL;
                    yield v == 0L ? null : v;               // uint32z, invalid = 0
                }
                default -> null; // string, byte array, float, etc. — bytes already consumed
            };
        } catch (Exception e) {
            return null;
        }
    }

    private void processRecordMessage(Map<Integer, Object> fields, List<GpsPoint> points) {
        GpsPoint.GpsPointBuilder builder = GpsPoint.builder();
        boolean hasPosition = false;

        Object latObj = fields.get(FIELD_POSITION_LAT);
        Object lonObj = fields.get(FIELD_POSITION_LONG);
        if (latObj != null && lonObj != null) {
            double lat = semicirclesToDegrees(((Number) latObj).longValue());
            double lon = semicirclesToDegrees(((Number) lonObj).longValue());
            if (lat != 0 && lon != 0 && Math.abs(lat) <= 90 && Math.abs(lon) <= 180) {
                builder.latitude(lat).longitude(lon);
                hasPosition = true;
            }
        }

        Object timestamp = fields.get(FIELD_TIMESTAMP);
        if (timestamp != null) {
            long ts = ((Number) timestamp).longValue();
            builder.time(fitTimestampToLocalDateTime(ts));
        }

        Object altitude = fields.get(FIELD_ALTITUDE);
        if (altitude != null) {
            builder.elevation(((Number) altitude).doubleValue() / 5.0 - 500.0);
        }

        Object hr = fields.get(FIELD_HEART_RATE);
        if (hr != null) builder.heartRate(((Number) hr).intValue());

        Object cad = fields.get(FIELD_CADENCE);
        if (cad != null) builder.cadence(((Number) cad).intValue());

        Object power = fields.get(FIELD_POWER);
        if (power != null) builder.power(((Number) power).intValue());

        Object dist = fields.get(FIELD_DISTANCE);
        if (dist != null) builder.distanceMeters(((Number) dist).doubleValue() / 100.0);

        if (hasPosition) {
            points.add(builder.build());
        }
    }

    private void processSessionMessage(Map<Integer, Object> fields, Map<String, Object> sessionData) {
        Object sport = fields.get(SESSION_SPORT);
        if (sport != null) sessionData.put("sport", ((Number) sport).intValue());

        Object elapsed = fields.get(SESSION_TOTAL_ELAPSED_TIME);
        if (elapsed != null) sessionData.put("totalTime", ((Number) elapsed).doubleValue() / 1000.0);

        Object dist = fields.get(SESSION_TOTAL_DISTANCE);
        if (dist != null) sessionData.put("totalDistance", ((Number) dist).doubleValue() / 100000.0);

        Object cal = fields.get(SESSION_TOTAL_CALORIES);
        if (cal != null) sessionData.put("calories", ((Number) cal).intValue());

        Object avgHr = fields.get(SESSION_AVG_HEART_RATE);
        if (avgHr != null) sessionData.put("avgHr", ((Number) avgHr).intValue());

        Object maxHr = fields.get(SESSION_MAX_HEART_RATE);
        if (maxHr != null) sessionData.put("maxHr", ((Number) maxHr).intValue());

        Object avgCad = fields.get(SESSION_AVG_CADENCE);
        if (avgCad != null) sessionData.put("avgCadence", ((Number) avgCad).intValue());

        Object avgPow = fields.get(SESSION_AVG_POWER);
        if (avgPow != null) sessionData.put("avgPower", ((Number) avgPow).intValue());

        Object ascent = fields.get(SESSION_TOTAL_ASCENT);
        if (ascent != null) sessionData.put("totalAscent", ((Number) ascent).intValue());

        Object maxSpd = fields.get(SESSION_MAX_SPEED);
        if (maxSpd != null) sessionData.put("maxSpeed", ((Number) maxSpd).doubleValue() / 1000.0 * 3.6);
    }

    private ParsedActivity buildParsedActivity(List<GpsPoint> points, Map<String, Object> sessionData) {
        ParsedActivity.ParsedActivityBuilder builder = ParsedActivity.builder()
                .gpsPoints(points)
                .title("Imported FIT Activity");

        // Use session data if available
        if (sessionData.containsKey("sport")) {
            builder.activityType(mapFitSport((int) sessionData.get("sport")));
        }
        if (sessionData.containsKey("totalTime")) {
            builder.duration((int) Math.round((double) sessionData.get("totalTime")));
        }
        if (sessionData.containsKey("totalDistance")) {
            builder.distance(Math.round((double) sessionData.get("totalDistance") * 100.0) / 100.0);
        }
        if (sessionData.containsKey("calories")) {
            builder.calories((int) sessionData.get("calories"));
        }
        if (sessionData.containsKey("avgHr")) {
            builder.averageHeartRate((int) sessionData.get("avgHr"));
        }
        if (sessionData.containsKey("maxHr")) {
            builder.maxHeartRate((int) sessionData.get("maxHr"));
        }
        if (sessionData.containsKey("avgCadence")) {
            builder.averageCadence((int) sessionData.get("avgCadence"));
        }
        if (sessionData.containsKey("avgPower")) {
            builder.averagePower((int) sessionData.get("avgPower"));
        }
        if (sessionData.containsKey("totalAscent")) {
            builder.elevationGain((int) sessionData.get("totalAscent"));
        }
        if (sessionData.containsKey("maxSpeed")) {
            builder.maxSpeed(Math.round((double) sessionData.get("maxSpeed") * 10.0) / 10.0);
        }

        // Fill gaps from GPS points if session data is missing
        if (!points.isEmpty()) {
            GpsPoint first = points.getFirst();
            GpsPoint last = points.getLast();

            if (first.getTime() != null) builder.startTime(first.getTime());
            builder.startLocation(String.format("%.4f, %.4f", first.getLatitude(), first.getLongitude()));
            builder.endLocation(String.format("%.4f, %.4f", last.getLatitude(), last.getLongitude()));

            if (!sessionData.containsKey("totalDistance")) {
                double dist = GeoUtils.totalDistanceKm(points);
                builder.distance(Math.round(dist * 100.0) / 100.0);
            }
            if (!sessionData.containsKey("totalAscent")) {
                builder.elevationGain(GeoUtils.totalElevationGain(points));
            }
            if (!sessionData.containsKey("totalTime") && first.getTime() != null && last.getTime() != null) {
                long seconds = java.time.Duration.between(first.getTime(), last.getTime()).getSeconds();
                builder.duration((int) seconds);
            }
        }

        return builder.build();
    }

    private ActivityType mapFitSport(int sport) {
        return switch (sport) {
            case 0 -> ActivityType.OTHER;        // generic
            case 1 -> ActivityType.RUN;
            case 2 -> ActivityType.RIDE;
            case 5 -> ActivityType.SWIM;
            case 6 -> ActivityType.RIDE;          // fitness_equipment
            case 10 -> ActivityType.WALK;
            case 11 -> ActivityType.HIKE;
            case 12 -> ActivityType.ALPINE_SKI;
            case 13 -> ActivityType.SNOWBOARD;
            case 15 -> ActivityType.ROWING;
            case 17 -> ActivityType.KAYAKING;
            case 19 -> ActivityType.SURFING;
            case 20 -> ActivityType.YOGA;
            case 37 -> ActivityType.ROCK_CLIMBING;
            default -> ActivityType.OTHER;
        };
    }

    private double semicirclesToDegrees(long semicircles) {
        return semicircles * (180.0 / Math.pow(2, 31));
    }

    private LocalDateTime fitTimestampToLocalDateTime(long fitTimestamp) {
        long unixTimestamp = fitTimestamp + FIT_EPOCH_OFFSET;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), ZoneId.systemDefault());
    }

    private int readUint16(DataInputStream dis) throws Exception {
        return (dis.readUnsignedByte() << 8) | dis.readUnsignedByte();
    }

    private int readUint16LE(DataInputStream dis) throws Exception {
        return dis.readUnsignedByte() | (dis.readUnsignedByte() << 8);
    }

    private long readUint32(DataInputStream dis) throws Exception {
        return ((long) dis.readUnsignedByte() << 24)
                | ((long) dis.readUnsignedByte() << 16)
                | ((long) dis.readUnsignedByte() << 8)
                | dis.readUnsignedByte();
    }

    private long readUint32LE(DataInputStream dis) throws Exception {
        return (long) dis.readUnsignedByte()
                | ((long) dis.readUnsignedByte() << 8)
                | ((long) dis.readUnsignedByte() << 16)
                | ((long) dis.readUnsignedByte() << 24);
    }

    private record FieldDef(int defNum, int size, int baseType, int globalMessageNumber, int architecture) {
    }
}
