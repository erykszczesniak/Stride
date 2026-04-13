import i18n from "../i18n";

/**
 * Format a duration in seconds to a human-readable string.
 * @param {number} seconds - Duration in seconds
 * @returns {string} Formatted duration (e.g., "2h 30m", "45m", "0m")
 */
export const formatDuration = (seconds) => {
  if (!seconds) return "0m";
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
};

/**
 * Format a duration in seconds to just the numeric value (hours or minutes).
 * Used for stats cards where the unit is displayed separately.
 * @param {number} seconds - Duration in seconds
 * @returns {string} Numeric value
 */
export const formatDurationValue = (seconds) => {
  if (!seconds) return "0";
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours > 0) return `${hours}`;
  return `${minutes}`;
};

/**
 * Get the unit label for a formatted duration value (translated).
 * @param {number} seconds - Duration in seconds
 * @returns {string} Unit label ("hours" or "min")
 */
export const formatDurationUnit = (seconds) => {
  if (!seconds) return i18n.t("common.hours");
  const hours = Math.floor(seconds / 3600);
  if (hours > 0) return i18n.t("common.hours");
  return i18n.t("common.minutes");
};

/**
 * Format a distance in km to a string with 2 decimal places.
 * When called with a units context, converts to the appropriate unit.
 * @param {number} km - Distance in kilometers
 * @param {object} [unitsCtx] - Optional units context from useUnits()
 * @returns {string} Formatted distance (e.g., "5.23 km" or "3.25 mi")
 */
export const formatDistance = (km, unitsCtx) => {
  if (unitsCtx) {
    const value = unitsCtx.convertDistance(km || 0);
    return `${value.toFixed(2)} ${unitsCtx.distanceUnit}`;
  }
  return `${(km || 0).toFixed(2)} km`;
};

/**
 * Activity types where pace (min/km) is more meaningful than speed (km/h).
 */
const PACE_ACTIVITY_TYPES = new Set([
  "RUN",
  "VIRTUAL_RUN",
  "WALK",
  "HIKE",
  "TRAIL_RUN",
]);

const SWIM_ACTIVITY_TYPES = new Set(["SWIM"]);

/**
 * Whether the given activity type should display pace instead of speed.
 * Includes running-style activities and swimming.
 */
export const isPaceActivity = (activityType) =>
  PACE_ACTIVITY_TYPES.has(activityType) || SWIM_ACTIVITY_TYPES.has(activityType);

/**
 * Whether the given activity type is a swim activity.
 */
export const isSwimActivity = (activityType) => SWIM_ACTIVITY_TYPES.has(activityType);

/**
 * Format pace as min:sec /km from distance and duration.
 * When called with a units context, uses sport-aware formatting.
 * @param {number} distanceKm
 * @param {number} durationSeconds
 * @param {object} [unitsCtx] - Optional units context from useUnits()
 * @param {string} [activityType] - Activity type for sport-aware formatting
 * @returns {string} Formatted pace (e.g., "5:30 /km")
 */
export const formatPace = (distanceKm, durationSeconds, unitsCtx, activityType) => {
  if (!distanceKm || !durationSeconds || distanceKm <= 0) return "\u2014";
  if (unitsCtx && activityType) {
    return unitsCtx.formatPaceFromActivity(distanceKm, durationSeconds, activityType);
  }
  const secondsPerKm = durationSeconds / distanceKm;
  const minutes = Math.floor(secondsPerKm / 60);
  const seconds = Math.round(secondsPerKm % 60);
  return `${minutes}:${seconds.toString().padStart(2, "0")} /km`;
};

/**
 * Format an ISO date string to a locale date string using the active i18n language.
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date (e.g., "Jan 15, 2025")
 */
export const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString(i18n.language, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
};

/**
 * Get a relative time string from an ISO date string, translated to the active language.
 * @param {string} dateString - ISO date string
 * @returns {string} Relative time (e.g., "5m ago", "2h ago", "3d ago")
 */
export const getTimeAgo = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return i18n.t("time.justNow");
  if (diffMins < 60) return i18n.t("time.minutesAgo", { count: diffMins });
  if (diffHours < 24) return i18n.t("time.hoursAgo", { count: diffHours });
  if (diffDays < 7) return i18n.t("time.daysAgo", { count: diffDays });
  if (diffDays < 30) return i18n.t("time.daysAgoLong", { count: diffDays });
  return date.toLocaleDateString(i18n.language);
};

/**
 * Get initials from first and last name.
 * @param {string} firstName
 * @param {string} lastName
 * @returns {string} Initials (e.g., "JS")
 */
export const getInitials = (firstName, lastName) => {
  return `${(firstName || "")[0] || ""}${(lastName || "")[0] || ""}`.toUpperCase();
};
