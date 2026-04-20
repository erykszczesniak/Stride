import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from "react";
import SettingsService from "../api/SettingsService";

const KM_TO_MI = 0.621371;
const M_TO_FT = 3.28084;
const KG_TO_LBS = 2.20462;
const CM_TO_IN = 0.393701;

const PACE_ACTIVITY_TYPES = new Set([
  "RUN",
  "VIRTUAL_RUN",
  "WALK",
  "HIKE",
  "TRAIL_RUN",
]);

const SWIM_ACTIVITY_TYPES = new Set(["SWIM"]);

const UnitsContext = createContext(null);

export const UnitsProvider = ({ children }) => {
  const [units, setUnits] = useState(() => {
    const prefs = SettingsService.getPreferences();
    return prefs.units || "metric";
  });

  const isImperial = units === "imperial";

  // Listen for localStorage changes (from Settings page save)
  useEffect(() => {
    const handleStorage = () => {
      const prefs = SettingsService.getPreferences();
      setUnits(prefs.units || "metric");
    };
    window.addEventListener("storage", handleStorage);
    return () => window.removeEventListener("storage", handleStorage);
  }, []);

  const updateUnits = useCallback((newUnits) => {
    setUnits(newUnits);
  }, []);

  const convertDistance = useCallback((km) => {
    if (km == null) return 0;
    return isImperial ? km * KM_TO_MI : km;
  }, [isImperial]);

  const convertDistanceToMetric = useCallback((value) => {
    if (value == null) return 0;
    return isImperial ? value / KM_TO_MI : value;
  }, [isImperial]);

  const convertElevation = useCallback((meters) => {
    if (meters == null) return 0;
    return isImperial ? meters * M_TO_FT : meters;
  }, [isImperial]);

  const convertElevationToMetric = useCallback((value) => {
    if (value == null) return 0;
    return isImperial ? value / M_TO_FT : value;
  }, [isImperial]);

  const convertWeight = useCallback((kg) => {
    if (kg == null) return 0;
    return isImperial ? kg * KG_TO_LBS : kg;
  }, [isImperial]);

  const convertWeightToMetric = useCallback((value) => {
    if (value == null) return 0;
    return isImperial ? value / KG_TO_LBS : value;
  }, [isImperial]);

  const convertHeight = useCallback((cm) => {
    if (cm == null) return 0;
    return isImperial ? cm * CM_TO_IN : cm;
  }, [isImperial]);

  const convertHeightToMetric = useCallback((value) => {
    if (value == null) return 0;
    return isImperial ? value / CM_TO_IN : value;
  }, [isImperial]);

  const convertSpeed = useCallback((kmh) => {
    if (kmh == null) return 0;
    return isImperial ? kmh * KM_TO_MI : kmh;
  }, [isImperial]);

  const distanceUnit = isImperial ? "mi" : "km";
  const elevationUnit = isImperial ? "ft" : "m";
  const weightUnit = isImperial ? "lbs" : "kg";
  const heightUnit = isImperial ? "in" : "cm";
  const speedUnit = isImperial ? "mph" : "km/h";

  /**
   * Format speed or pace for an activity type.
   * @param {number} speedKmh - Speed in km/h
   * @param {string} activityType - e.g. "RUN", "SWIM", "RIDE"
   * @returns {string} Formatted string
   */
  const formatSpeed = useCallback((speedKmh, activityType) => {
    if (!speedKmh || speedKmh <= 0) return "\u2014";

    if (SWIM_ACTIVITY_TYPES.has(activityType)) {
      // Swim: pace in min/100m or min/100yd
      // speedKmh -> km per hour -> seconds per km = 3600 / speedKmh
      // seconds per 100m = secondsPerKm / 10
      const secondsPerKm = 3600 / speedKmh;
      let secondsPerUnit;
      let unitLabel;
      if (isImperial) {
        // 1 km = 1093.61 yd, so seconds per 100yd = secondsPerKm / 10.9361
        secondsPerUnit = secondsPerKm / 10.9361;
        unitLabel = "/100yd";
      } else {
        secondsPerUnit = secondsPerKm / 10;
        unitLabel = "/100m";
      }
      const minutes = Math.floor(secondsPerUnit / 60);
      const seconds = Math.round(secondsPerUnit % 60);
      return `${minutes}:${seconds.toString().padStart(2, "0")} ${unitLabel}`;
    }

    if (PACE_ACTIVITY_TYPES.has(activityType)) {
      // Running pace: min/km or min/mi
      const secondsPerKm = 3600 / speedKmh;
      const secondsPerUnit = isImperial ? secondsPerKm / KM_TO_MI : secondsPerKm;
      const minutes = Math.floor(secondsPerUnit / 60);
      const seconds = Math.round(secondsPerUnit % 60);
      const unitLabel = isImperial ? "/mi" : "/km";
      return `${minutes}:${seconds.toString().padStart(2, "0")} ${unitLabel}`;
    }

    // Speed: km/h or mph
    const speed = isImperial ? speedKmh * KM_TO_MI : speedKmh;
    const unitLabel = isImperial ? "mph" : "km/h";
    return `${speed.toFixed(1)} ${unitLabel}`;
  }, [isImperial]);

  /**
   * Format pace/speed from raw distance and duration.
   * @param {number} distanceKm - Distance in km
   * @param {number} durationSeconds - Duration in seconds
   * @param {string} activityType - e.g. "RUN", "SWIM", "RIDE"
   * @returns {string} Formatted string
   */
  const formatPaceFromActivity = useCallback((distanceKm, durationSeconds, activityType) => {
    if (!distanceKm || !durationSeconds || distanceKm <= 0 || durationSeconds <= 0) return "\u2014";

    if (SWIM_ACTIVITY_TYPES.has(activityType)) {
      // Swim pace: min/100m or min/100yd
      let secondsPerUnit;
      let unitLabel;
      if (isImperial) {
        // Convert km to yards: 1 km = 1093.61 yd
        const distanceYd = distanceKm * 1093.61;
        secondsPerUnit = durationSeconds / (distanceYd / 100);
        unitLabel = "/100yd";
      } else {
        // 1 km = 10 * 100m
        secondsPerUnit = durationSeconds / (distanceKm * 10);
        unitLabel = "/100m";
      }
      const minutes = Math.floor(secondsPerUnit / 60);
      const seconds = Math.round(secondsPerUnit % 60);
      return `${minutes}:${seconds.toString().padStart(2, "0")} ${unitLabel}`;
    }

    if (PACE_ACTIVITY_TYPES.has(activityType)) {
      // Running pace: min/km or min/mi
      let secondsPerUnit;
      let unitLabel;
      if (isImperial) {
        const distanceMi = distanceKm * KM_TO_MI;
        secondsPerUnit = durationSeconds / distanceMi;
        unitLabel = "/mi";
      } else {
        secondsPerUnit = durationSeconds / distanceKm;
        unitLabel = "/km";
      }
      const minutes = Math.floor(secondsPerUnit / 60);
      const seconds = Math.round(secondsPerUnit % 60);
      return `${minutes}:${seconds.toString().padStart(2, "0")} ${unitLabel}`;
    }

    // Speed: km/h or mph
    const speedKmh = distanceKm / (durationSeconds / 3600);
    const speed = isImperial ? speedKmh * KM_TO_MI : speedKmh;
    const unitLabel = isImperial ? "mph" : "km/h";
    return `${speed.toFixed(1)} ${unitLabel}`;
  }, [isImperial]);

  const value = useMemo(() => ({
    units,
    isImperial,
    updateUnits,
    convertDistance,
    convertDistanceToMetric,
    convertElevation,
    convertElevationToMetric,
    convertWeight,
    convertWeightToMetric,
    convertHeight,
    convertHeightToMetric,
    convertSpeed,
    distanceUnit,
    elevationUnit,
    weightUnit,
    heightUnit,
    speedUnit,
    formatSpeed,
    formatPaceFromActivity,
  }), [
    units,
    isImperial,
    updateUnits,
    convertDistance,
    convertDistanceToMetric,
    convertElevation,
    convertElevationToMetric,
    convertWeight,
    convertWeightToMetric,
    convertHeight,
    convertHeightToMetric,
    convertSpeed,
    distanceUnit,
    elevationUnit,
    weightUnit,
    heightUnit,
    speedUnit,
    formatSpeed,
    formatPaceFromActivity,
  ]);

  return (
    <UnitsContext.Provider value={value}>
      {children}
    </UnitsContext.Provider>
  );
};

export const useUnits = () => {
  const context = useContext(UnitsContext);
  if (!context) {
    throw new Error("useUnits must be used within a UnitsProvider");
  }
  return context;
};

export default UnitsContext;
