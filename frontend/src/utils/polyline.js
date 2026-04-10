/**
 * Polyline coordinate parsing utilities.
 *
 * Activities in this app may store their route in two different formats:
 *  1. Legacy: a JSON-stringified array of [lat, lng] pairs
 *     e.g. "[[52.2297,21.0122],[52.2250,21.0155],...]"
 *  2. Standard: a Google Encoded Polyline string (compact, ~10x smaller)
 *     e.g. "_hu}Hwkk_CwBoFsDcG..."
 *
 * Imported GPX/TCX/FIT activities use the encoded format. Activities created
 * via the manual form historically use the JSON format. Both must render.
 */

/**
 * Decode a Google Encoded Polyline string into an array of [lat, lng] pairs.
 * Algorithm: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
export const decodeEncodedPolyline = (encoded) => {
  if (typeof encoded !== "string" || encoded.length === 0) return [];

  const points = [];
  let index = 0;
  let lat = 0;
  let lng = 0;

  while (index < encoded.length) {
    let shift = 0;
    let result = 0;
    let b;
    do {
      b = encoded.charCodeAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    const dlat = result & 1 ? ~(result >> 1) : result >> 1;
    lat += dlat;

    shift = 0;
    result = 0;
    do {
      b = encoded.charCodeAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20);
    const dlng = result & 1 ? ~(result >> 1) : result >> 1;
    lng += dlng;

    points.push([lat / 1e5, lng / 1e5]);
  }

  return points;
};

/**
 * Parse any supported route format into an array of [lat, lng] pairs.
 * Accepts the value of Activity.routePolyline (string) or an already-parsed array.
 * Returns [] if input is missing or unparseable.
 */
export const parseRouteCoordinates = (route) => {
  if (!route) return [];
  if (Array.isArray(route)) return route;
  if (typeof route !== "string") return [];

  const trimmed = route.trim();
  if (trimmed.length === 0) return [];

  // Legacy JSON-array format
  if (trimmed.startsWith("[")) {
    try {
      const parsed = JSON.parse(trimmed);
      return Array.isArray(parsed) ? parsed : [];
    } catch (e) {
      return [];
    }
  }

  // Otherwise treat as Google Encoded Polyline
  try {
    return decodeEncodedPolyline(trimmed);
  } catch (e) {
    return [];
  }
};
