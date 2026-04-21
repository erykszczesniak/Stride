package com.strava.backend.service.importer;

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
public class GpxParser implements ActivityFileParser {

    @Override
    public ParsedActivity parse(InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(input);
        doc.getDocumentElement().normalize();

        String title = extractText(doc, "name");
        String description = extractText(doc, "desc");

        List<GpsPoint> points = new ArrayList<>();
        NodeList trackpoints = doc.getElementsByTagName("trkpt");
        if (trackpoints.getLength() == 0) {
            trackpoints = doc.getElementsByTagName("rtept"); // route points fallback
        }

        for (int i = 0; i < trackpoints.getLength(); i++) {
            Element trkpt = (Element) trackpoints.item(i);
            GpsPoint point = parseTrackpoint(trkpt);
            if (point != null) {
                points.add(point);
            }
        }

        return buildParsedActivity(title, description, points);
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

    @Override
    public boolean supports(String fileExtension) {
        return "gpx".equalsIgnoreCase(fileExtension);
    }

    private GpsPoint parseTrackpoint(Element trkpt) {
        try {
            double lat = Double.parseDouble(trkpt.getAttribute("lat"));
            double lon = Double.parseDouble(trkpt.getAttribute("lon"));

            GpsPoint.GpsPointBuilder pointBuilder = GpsPoint.builder()
                    .latitude(lat)
                    .longitude(lon);

            String eleText = getChildText(trkpt, "ele");
            if (eleText != null) {
                pointBuilder.elevation(Double.parseDouble(eleText));
            }

            String timeText = getChildText(trkpt, "time");
            if (timeText != null) {
                pointBuilder.time(parseTime(timeText));
            }

            // Parse Garmin TrackPointExtension (heart rate, cadence)
            NodeList extensions = trkpt.getElementsByTagName("extensions");
            if (extensions.getLength() > 0) {
                Element ext = (Element) extensions.item(0);
                String hr = getDeepChildText(ext, "hr");
                if (hr != null) {
                    pointBuilder.heartRate(Integer.parseInt(hr));
                }
                String cad = getDeepChildText(ext, "cad");
                if (cad != null) {
                    pointBuilder.cadence(Integer.parseInt(cad));
                }
                String power = getDeepChildText(ext, "power");
                if (power != null) {
                    pointBuilder.power(Integer.parseInt(power));
                }
            }

            return pointBuilder.build();
        } catch (NumberFormatException e) {
            log.warn("Skipping invalid GPX trackpoint: {}", e.getMessage());
            return null;
        }
    }

    private ParsedActivity buildParsedActivity(String title, String description, List<GpsPoint> points) {
        ParsedActivity.ParsedActivityBuilder activityBuilder = ParsedActivity.builder()
                .title(title != null ? title : "Imported GPX Activity")
                .description(description)
                .gpsPoints(points)
                .elevations(extractElevations(points))
                .pointTimestampsEpochSec(extractTimestamps(points));

        if (!points.isEmpty()) {
            GpsPoint first = points.getFirst();
            GpsPoint last = points.getLast();

            activityBuilder.startLocation(formatLocation(first));
            activityBuilder.endLocation(formatLocation(last));

            if (first.getTime() != null) {
                activityBuilder.startTime(first.getTime());
            }

            if (first.getTime() != null && last.getTime() != null) {
                long seconds = java.time.Duration.between(first.getTime(), last.getTime()).getSeconds();
                activityBuilder.duration((int) seconds);
            }

            double distanceKm = GeoUtils.totalDistanceKm(points);
            activityBuilder.distance(Math.round(distanceKm * 100.0) / 100.0);
            activityBuilder.elevationGain(GeoUtils.totalElevationGain(points));

            double maxSpeed = GeoUtils.maxSpeedKmh(points);
            if (maxSpeed > 0) {
                activityBuilder.maxSpeed(Math.round(maxSpeed * 10.0) / 10.0);
            }

            // Heart rate stats
            List<Integer> heartRates = points.stream()
                    .map(GpsPoint::getHeartRate)
                    .filter(hr -> hr != null && hr > 0)
                    .toList();
            if (!heartRates.isEmpty()) {
                activityBuilder.averageHeartRate((int) heartRates.stream().mapToInt(Integer::intValue).average().orElse(0));
                activityBuilder.maxHeartRate(heartRates.stream().mapToInt(Integer::intValue).max().orElse(0));
            }

            // Cadence stats
            List<Integer> cadences = points.stream()
                    .map(GpsPoint::getCadence)
                    .filter(c -> c != null && c > 0)
                    .toList();
            if (!cadences.isEmpty()) {
                activityBuilder.averageCadence((int) cadences.stream().mapToInt(Integer::intValue).average().orElse(0));
            }

            // Power stats
            List<Integer> powers = points.stream()
                    .map(GpsPoint::getPower)
                    .filter(p -> p != null && p > 0)
                    .toList();
            if (!powers.isEmpty()) {
                activityBuilder.averagePower((int) powers.stream().mapToInt(Integer::intValue).average().orElse(0));
            }
        }

        return activityBuilder.build();
    }

    private String extractText(Document doc, String tagName) {
        // Try metadata first, then track
        NodeList metadataList = doc.getElementsByTagName("metadata");
        if (metadataList.getLength() > 0) {
            String text = getChildText((Element) metadataList.item(0), tagName);
            if (text != null) return text;
        }
        NodeList trkList = doc.getElementsByTagName("trk");
        if (trkList.getLength() > 0) {
            return getChildText((Element) trkList.item(0), tagName);
        }
        return null;
    }

    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null && !text.isBlank() ? text.trim() : null;
        }
        return null;
    }

    private String getDeepChildText(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                String nodeName = child.getNodeName();
                // Match "hr", "gpxtpx:hr", etc.
                if (nodeName.equals(localName) || nodeName.endsWith(":" + localName)) {
                    String text = child.getTextContent();
                    return text != null && !text.isBlank() ? text.trim() : null;
                }
                // Recurse into extension elements
                String found = getDeepChildText(child, localName);
                if (found != null) return found;
            }
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
                log.warn("Unable to parse GPX time: {}", text);
                return null;
            }
        }
    }

    private String formatLocation(GpsPoint point) {
        return String.format("%.4f, %.4f", point.getLatitude(), point.getLongitude());
    }
}
