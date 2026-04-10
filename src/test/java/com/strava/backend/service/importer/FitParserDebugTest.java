package com.strava.backend.service.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.io.FileInputStream;

/**
 * Debug-only test that runs the FIT parser against a real file from disk.
 * Enabled by setting -Dfit.debug.file=/path/to/file.fit on the command line.
 */
class FitParserDebugTest {

    @Test
    @EnabledIfSystemProperty(named = "fit.debug.file", matches = ".+")
    void parseRealFitFile() throws Exception {
        String path = System.getProperty("fit.debug.file");
        File file = new File(path);
        System.out.println("=== Parsing FIT file: " + path + " (" + file.length() + " bytes) ===");

        FitParser parser = new FitParser();
        try (FileInputStream fis = new FileInputStream(file)) {
            ParsedActivity parsed = parser.parse(fis);

            System.out.println("Title:           " + parsed.getTitle());
            System.out.println("ActivityType:    " + parsed.getActivityType());
            System.out.println("Distance (km):   " + parsed.getDistance());
            System.out.println("Duration (s):    " + parsed.getDuration());
            System.out.println("Elevation gain:  " + parsed.getElevationGain());
            System.out.println("Calories:        " + parsed.getCalories());
            System.out.println("Avg HR:          " + parsed.getAverageHeartRate());
            System.out.println("Max HR:          " + parsed.getMaxHeartRate());
            System.out.println("Avg cadence:     " + parsed.getAverageCadence());
            System.out.println("Avg power:       " + parsed.getAveragePower());
            System.out.println("Max speed:       " + parsed.getMaxSpeed());
            System.out.println("Start time:      " + parsed.getStartTime());
            System.out.println("Start location:  " + parsed.getStartLocation());
            System.out.println("End location:    " + parsed.getEndLocation());
            System.out.println("GPS points:      " + (parsed.getGpsPoints() == null ? 0 : parsed.getGpsPoints().size()));

            if (parsed.getGpsPoints() != null && !parsed.getGpsPoints().isEmpty()) {
                GpsPoint first = parsed.getGpsPoints().get(0);
                GpsPoint last = parsed.getGpsPoints().get(parsed.getGpsPoints().size() - 1);
                System.out.println("First GPS:       " + first.getLatitude() + ", " + first.getLongitude()
                        + " ele=" + first.getElevation() + " hr=" + first.getHeartRate()
                        + " time=" + first.getTime());
                System.out.println("Last GPS:        " + last.getLatitude() + ", " + last.getLongitude()
                        + " ele=" + last.getElevation() + " hr=" + last.getHeartRate()
                        + " time=" + last.getTime());
            }
        }
    }
}
