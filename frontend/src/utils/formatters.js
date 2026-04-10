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
 * Get the unit label for a formatted duration value.
 * @param {number} seconds - Duration in seconds
 * @returns {string} Unit label ("hours" or "min")
 */
export const formatDurationUnit = (seconds) => {
  if (!seconds) return "hours";
  const hours = Math.floor(seconds / 3600);
  if (hours > 0) return "hours";
  return "min";
};

/**
 * Format a distance in km to a string with 2 decimal places.
 * @param {number} km - Distance in kilometers
 * @returns {string} Formatted distance (e.g., "5.23 km")
 */
export const formatDistance = (km) => {
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

/**
 * Whether the given activity type should display pace instead of speed.
 */
export const isPaceActivity = (activityType) => PACE_ACTIVITY_TYPES.has(activityType);

/**
 * Format pace as min:sec /km from distance and duration.
 * @param {number} distanceKm
 * @param {number} durationSeconds
 * @returns {string} Formatted pace (e.g., "5:30 /km")
 */
export const formatPace = (distanceKm, durationSeconds) => {
  if (!distanceKm || !durationSeconds || distanceKm <= 0) return "—";
  const secondsPerKm = durationSeconds / distanceKm;
  const minutes = Math.floor(secondsPerKm / 60);
  const seconds = Math.round(secondsPerKm % 60);
  return `${minutes}:${seconds.toString().padStart(2, "0")} /km`;
};

/**
 * Format an ISO date string to a locale date string.
 * @param {string} dateString - ISO date string
 * @returns {string} Formatted date (e.g., "Jan 15, 2025")
 */
export const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
};

/**
 * Get a relative time string from an ISO date string.
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

  if (diffMins < 1) return "Just now";
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  if (diffDays < 30) return `${diffDays} days ago`;
  return date.toLocaleDateString();
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
