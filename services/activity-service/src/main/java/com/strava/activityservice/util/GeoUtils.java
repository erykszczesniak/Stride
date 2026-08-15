package com.strava.activityservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Polyline decoding, ported from main's GeoUtils (the subset the activity
 * write path needs for GPS-stream alignment validation).
 */
@Slf4j
public final class GeoUtils {

    private static final ObjectMapper JSON = new ObjectMapper();

    private GeoUtils() {
    }

    /**
     * Decode any supported polyline format into [lat, lng] pairs.
     * Activities can have their route stored in two ways:
     *   1. Legacy: a JSON-stringified [[lat,lng],...] array (manual entries)
     *   2. Standard: a Google Encoded Polyline string (imported GPX/TCX/FIT)
     * Returns an empty list for null, blank, or unparseable input rather than
     * throwing — callers should handle missing GPS gracefully.
     */
    public static List<double[]> decodePolyline(String route) {
        if (route == null || route.isBlank()) return List.of();

        String trimmed = route.trim();

        if (trimmed.startsWith("[")) {
            return decodeJsonArray(trimmed);
        }
        return decodeEncodedPolyline(trimmed);
    }

    private static List<double[]> decodeJsonArray(String json) {
        try {
            double[][] parsed = JSON.readValue(json, double[][].class);
            List<double[]> points = new ArrayList<>(parsed.length);
            for (double[] pair : parsed) {
                if (pair != null && pair.length >= 2) {
                    points.add(new double[]{pair[0], pair[1]});
                }
            }
            return points;
        } catch (Exception e) {
            log.warn("Failed to decode JSON polyline: {}", e.getMessage());
            return List.of();
        }
    }

    private static List<double[]> decodeEncodedPolyline(String encoded) {
        List<double[]> points = new ArrayList<>();
        int index = 0;
        int lat = 0;
        int lng = 0;
        int len = encoded.length();

        try {
            while (index < len) {
                int shift = 0;
                int result = 0;
                int b;
                do {
                    if (index >= len) return points; // truncated mid-pair, bail out gracefully
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

                shift = 0;
                result = 0;
                do {
                    if (index >= len) return points; // truncated mid-pair, bail out gracefully
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

                points.add(new double[]{lat / 1e5, lng / 1e5});
            }
        } catch (Exception e) {
            log.warn("Failed to decode encoded polyline: {}", e.getMessage());
        }
        return points;
    }
}
