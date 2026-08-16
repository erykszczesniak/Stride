package com.strava.pipeline.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Polyline + haversine helpers, ported verbatim from main's GeoUtils (the
 * subset the pipeline facade needs: distance between points, decoding both
 * stored route formats, re-encoding legacy JSON routes).
 */
@Slf4j
public final class GeoUtils {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * Haversine formula to calculate distance between two GPS coordinates.
     * Returns distance in meters.
     */
    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c * 1000.0;
    }

    /**
     * Encode [lat, lng] pairs into a Google Encoded Polyline string.
     * (Main's version takes GpsPoint objects; the algorithm is identical.)
     */
    public static String encodePolyline(List<double[]> points) {
        StringBuilder encoded = new StringBuilder();
        int prevLat = 0;
        int prevLng = 0;

        for (double[] point : points) {
            int lat = (int) Math.round(point[0] * 1e5);
            int lng = (int) Math.round(point[1] * 1e5);

            encodeValue(lat - prevLat, encoded);
            encodeValue(lng - prevLng, encoded);

            prevLat = lat;
            prevLng = lng;
        }
        return encoded.toString();
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

    private static void encodeValue(int value, StringBuilder encoded) {
        int v = value < 0 ? ~(value << 1) : (value << 1);
        while (v >= 0x20) {
            encoded.append((char) ((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        encoded.append((char) (v + 63));
    }
}
