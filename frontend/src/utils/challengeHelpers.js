/**
 * Get the emoji icon for a challenge based on its type and activity type.
 * @param {string} challengeType
 * @param {string} activityType
 * @returns {string} Emoji icon
 */
export const getIconForType = (challengeType, activityType) => {
  if (activityType === "RUN") return "🏃";
  if (activityType === "RIDE") return "🚴";
  if (activityType === "SWIM") return "🏊";
  if (challengeType === "DISTANCE") return "📍";
  if (challengeType === "TIME") return "⏱️";
  if (challengeType === "ELEVATION") return "⛰️";
  if (challengeType === "ACTIVITIES_COUNT") return "📅";
  return "🏆";
};

/**
 * Get the unit label for a challenge type.
 * @param {string} challengeType
 * @returns {string} Unit label (e.g., "km", "hours", "m")
 */
export const formatUnit = (challengeType) => {
  switch (challengeType) {
    case "DISTANCE":
      return "km";
    case "TIME":
      return "hours";
    case "ELEVATION":
      return "m";
    case "ACTIVITIES_COUNT":
      return "activities";
    default:
      return "";
  }
};

/**
 * Calculate the number of days left until a given end date.
 * @param {string} endDate - ISO date string
 * @returns {number} Days remaining (0 if already passed)
 */
export const getDaysLeft = (endDate) => {
  const end = new Date(endDate);
  const now = new Date();
  const diff = Math.ceil((end - now) / (1000 * 60 * 60 * 24));
  return diff > 0 ? diff : 0;
};

/**
 * Get a color for an avatar based on index.
 * @param {number} index
 * @returns {string} Hex color
 */
export const getAvatarColor = (index) => {
  const colors = ["#5c6bc0", "#26a69a", "#ef5350", "#7e57c2", "#42a5f5"];
  return colors[index % colors.length];
};
