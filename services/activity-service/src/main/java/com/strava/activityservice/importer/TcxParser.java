package com.strava.activityservice.importer;

import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.util.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TcxParser implements ActivityFileParser {

    @Override
    public ParsedActivity parse(InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();

        NodeList activityNodes = doc.getElementsByTagName("Activity");
        if (activityNodes.getLength() == 0) {
            throw new IllegalArgumentException("No Activity element found in TCX file");
        }

        Element activityElement = (Element) activityNodes.item(0);
        String sport = activityElement.getAttribute("Sport");
        ActivityType activityType = mapSportToActivityType(sport);

        // TCX activities can carry a free-text description in <Notes> (right after <Id>).
        // Garmin Training Center, Strava export and most third-party apps put the
        // workout name and notes here. Use it as the activity description.
        String notes = getDirectChildText(activityElement, "Notes");

        // Aggregate data from all laps
        int totalCalories = 0;
        double totalDistanceMeters = 0;
        double totalTimeSeconds = 0;
        int maxHr = 0;
        List<Integer> avgHrs = new ArrayList<>();
        List<GpsPoint> allPoints = new ArrayList<>();

        NodeList laps = activityElement.getElementsByTagName("Lap");
        for (int i = 0; i < laps.getLength(); i++) {
            Element lap = (Element) laps.item(i);

            String timeStr = getDirectChildText(lap, "TotalTimeSeconds");
            if (timeStr != null) totalTimeSeconds += Double.parseDouble(timeStr);

            String distStr = getDirectChildText(lap, "DistanceMeters");
            if (distStr != null) totalDistanceMeters += Double.parseDouble(distStr);

            String calStr = getDirectChildText(lap, "Calories");
            if (calStr != null) totalCalories += Integer.parseInt(calStr);

            String maxHrStr = getNestedText(lap, "MaximumHeartRateBpm", "Value");
            if (maxHrStr != null) {
                int hr = Integer.parseInt(maxHrStr);
                if (hr > maxHr) maxHr = hr;
            }

            String avgHrStr = getNestedText(lap, "AverageHeartRateBpm", "Value");
            if (avgHrStr != null) avgHrs.add(Integer.parseInt(avgHrStr));

            // Parse trackpoints
            NodeList trackpoints = lap.getElementsByTagName("Trackpoint");
            for (int j = 0; j < trackpoints.getLength(); j++) {
                GpsPoint point = parseTrackpoint((Element) trackpoints.item(j));
                if (point != null) {
                    allPoints.add(point);
                }
            }
        }

        ParsedActivity.ParsedActivityBuilder activityBuilder = ParsedActivity.builder()
                .activityType(activityType)
                .gpsPoints(allPoints)
                .elevations(extractElevations(allPoints))
                .pointTimestampsEpochSec(extractTimestamps(allPoints))
                .duration((int) totalTimeSeconds)
                .calories(totalCalories > 0 ? totalCalories : null);

        // Use explicit distance from TCX if available, otherwise calculate from GPS
        if (totalDistanceMeters > 0) {
            activityBuilder.distance(Math.round((totalDistanceMeters / 1000.0) * 100.0) / 100.0);
        } else if (!allPoints.isEmpty()) {
            double distKm = GeoUtils.totalDistanceKm(allPoints);
            activityBuilder.distance(Math.round(distKm * 100.0) / 100.0);
        }

        if (maxHr > 0) activityBuilder.maxHeartRate(maxHr);
        if (!avgHrs.isEmpty()) {
            activityBuilder.averageHeartRate((int) avgHrs.stream().mapToInt(Integer::intValue).average().orElse(0));
        }

        if (!allPoints.isEmpty()) {
            GpsPoint first = allPoints.getFirst();
            GpsPoint last = allPoints.getLast();

            if (first.getTime() != null) activityBuilder.startTime(first.getTime());
            activityBuilder.startLocation(formatLocation(first));
            activityBuilder.endLocation(formatLocation(last));
            activityBuilder.elevationGain(GeoUtils.totalElevationGain(allPoints));

            double maxSpeed = GeoUtils.maxSpeedKmh(allPoints);
            if (maxSpeed > 0) activityBuilder.maxSpeed(Math.round(maxSpeed * 10.0) / 10.0);

            // Cadence from trackpoints
            List<Integer> cadences = allPoints.stream()
                    .map(GpsPoint::getCadence)
                    .filter(c -> c != null && c > 0)
                    .toList();
            if (!cadences.isEmpty()) {
                activityBuilder.averageCadence((int) cadences.stream().mapToInt(Integer::intValue).average().orElse(0));
            }

            // Power from trackpoints
            List<Integer> powers = allPoints.stream()
                    .map(GpsPoint::getPower)
                    .filter(p -> p != null && p > 0)
                    .toList();
            if (!powers.isEmpty()) {
                activityBuilder.averagePower((int) powers.stream().mapToInt(Integer::intValue).average().orElse(0));
            }
        }

        // Title preference order:
        //   1. First non-blank line of <Notes>, capped at 100 chars (so a notes block
        //      that starts with the workout name like "5x1km intervals" becomes the title)
        //   2. "Imported {sport}" fallback
        // Description: the full <Notes> text (may be null)
        String title = null;
        if (notes != null && !notes.isBlank()) {
            String firstLine = notes.split("\\R", 2)[0].trim();
            if (!firstLine.isEmpty()) {
                title = firstLine.length() > 100 ? firstLine.substring(0, 100) : firstLine;
            }
        }
        if (title == null) {
            title = "Imported " + (sport != null && !sport.isEmpty() ? sport : "Activity");
        }
        activityBuilder.title(title);
        if (notes != null && !notes.isBlank()) {
            activityBuilder.description(notes.trim());
        }

        return activityBuilder.build();
    }

    @Override
    public boolean supports(String fileExtension) {
        return "tcx".equalsIgnoreCase(fileExtension);
    }

    /** Extract per-point elevations aligned with the supplied GPS points list. */
    private List<Double> extractElevations(List<GpsPoint> points) {
        List<Double> elevations = new ArrayList<>(points.size());
        boolean anyPresent = false;
        for (GpsPoint p : points) {
            Double e = p.getElevation();
            elevations.add(e);
            if (e != null) anyPresent = true;
        }
        return anyPresent ? elevations : new ArrayList<>();
    }

    /**
     * Extract per-point epoch-seconds timestamps aligned with the supplied GPS points.
     * Returns an empty list if ANY point is missing a time — a partial series would
     * produce misleading splits, so we treat it as absent and let the frontend fall
     * back to uniform-pace computation.
     */
    private List<Long> extractTimestamps(List<GpsPoint> points) {
        List<Long> timestamps = new ArrayList<>(points.size());
        for (GpsPoint p : points) {
            LocalDateTime t = p.getTime();
            if (t == null) {
                return new ArrayList<>();
            }
            timestamps.add(t.toEpochSecond(ZoneOffset.UTC));
        }
        return timestamps;
    }

    private GpsPoint parseTrackpoint(Element trackpoint) {
        GpsPoint.GpsPointBuilder pointBuilder = GpsPoint.builder();
        boolean hasData = false;

        String timeStr = getDirectChildText(trackpoint, "Time");
        if (timeStr != null) {
            LocalDateTime time = parseTime(timeStr);
            if (time != null) {
                pointBuilder.time(time);
                hasData = true;
            }
        }

        NodeList positionNodes = trackpoint.getElementsByTagName("Position");
        if (positionNodes.getLength() > 0) {
            Element position = (Element) positionNodes.item(0);
            String lat = getDirectChildText(position, "LatitudeDegrees");
            String lon = getDirectChildText(position, "LongitudeDegrees");
            if (lat != null && lon != null) {
                pointBuilder.latitude(Double.parseDouble(lat));
                pointBuilder.longitude(Double.parseDouble(lon));
                hasData = true;
            }
        }

        String altitude = getDirectChildText(trackpoint, "AltitudeMeters");
        if (altitude != null) pointBuilder.elevation(Double.parseDouble(altitude));

        String distance = getDirectChildText(trackpoint, "DistanceMeters");
        if (distance != null) pointBuilder.distanceMeters(Double.parseDouble(distance));

        String hrStr = getNestedText(trackpoint, "HeartRateBpm", "Value");
        if (hrStr != null) pointBuilder.heartRate(Integer.parseInt(hrStr));

        String cadStr = getDirectChildText(trackpoint, "Cadence");
        if (cadStr != null) pointBuilder.cadence(Integer.parseInt(cadStr));

        // Power in extensions
        NodeList extensions = trackpoint.getElementsByTagName("Extensions");
        if (extensions.getLength() > 0) {
            Element ext = (Element) extensions.item(0);
            NodeList powerNodes = ext.getElementsByTagName("Watts");
            if (powerNodes.getLength() == 0) {
                powerNodes = ext.getElementsByTagName("ns3:Watts");
            }
            if (powerNodes.getLength() > 0) {
                pointBuilder.power(Integer.parseInt(powerNodes.item(0).getTextContent().trim()));
            }
        }

        return hasData ? pointBuilder.build() : null;
    }

    private ActivityType mapSportToActivityType(String sport) {
        if (sport == null) return null;
        return switch (sport.toLowerCase()) {
            case "running" -> ActivityType.RUN;
            case "biking", "cycling" -> ActivityType.RIDE;
            case "swimming" -> ActivityType.SWIM;
            case "hiking" -> ActivityType.HIKE;
            case "walking" -> ActivityType.WALK;
            default -> ActivityType.OTHER;
        };
    }

    private String getDirectChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null && !text.isBlank() ? text.trim() : null;
        }
        return null;
    }

    private String getNestedText(Element parent, String outerTag, String innerTag) {
        NodeList outer = parent.getElementsByTagName(outerTag);
        if (outer.getLength() > 0) {
            return getDirectChildText((Element) outer.item(0), innerTag);
        }
        return null;
    }

    private LocalDateTime parseTime(String text) {
        try {
            return ZonedDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e2) {
                log.warn("Unable to parse TCX time: {}", text);
                return null;
            }
        }
    }

    private String formatLocation(GpsPoint point) {
        if (point.getLatitude() == 0 && point.getLongitude() == 0) return null;
        return String.format("%.4f, %.4f", point.getLatitude(), point.getLongitude());
    }
}
