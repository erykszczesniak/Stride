package com.strava.activityservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Parsing of the JSON-stringified GPS stream columns into the primitive
 * arrays the response DTOs ship. Null/blank/malformed input yields null —
 * the frontend degrades gracefully (no elevation chart, uniform-pace splits).
 */
@Slf4j
public final class StreamJson {

    private static final ObjectMapper JSON = new ObjectMapper();

    private StreamJson() {
    }

    /** Null entries in the JSON (unknown altitude at that point) are coerced to 0. */
    public static double[] parseElevationProfile(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            Double[] boxed = JSON.readValue(json, Double[].class);
            double[] result = new double[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                result[i] = boxed[i] != null ? boxed[i] : 0.0;
            }
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse elevation profile JSON: {}", e.getMessage());
            return null;
        }
    }

    public static long[] parsePointTimestamps(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            Long[] boxed = JSON.readValue(json, Long[].class);
            long[] result = new long[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                result[i] = boxed[i] != null ? boxed[i] : 0L;
            }
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse point timestamps JSON: {}", e.getMessage());
            return null;
        }
    }
}
