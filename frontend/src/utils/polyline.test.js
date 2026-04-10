import { decodeEncodedPolyline, parseRouteCoordinates } from "./polyline";

describe("decodeEncodedPolyline", () => {
  test("returns [] for empty string", () => {
    expect(decodeEncodedPolyline("")).toEqual([]);
  });

  test("returns [] for null", () => {
    expect(decodeEncodedPolyline(null)).toEqual([]);
  });

  test("returns [] for undefined", () => {
    expect(decodeEncodedPolyline(undefined)).toEqual([]);
  });

  test("returns [] for non-string values", () => {
    expect(decodeEncodedPolyline(123)).toEqual([]);
    expect(decodeEncodedPolyline({})).toEqual([]);
    expect(decodeEncodedPolyline([])).toEqual([]);
  });

  test("decodes the canonical Google polyline test vector", () => {
    // https://developers.google.com/maps/documentation/utilities/polylinealgorithm
    const encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
    const result = decodeEncodedPolyline(encoded);

    expect(result).toHaveLength(3);

    expect(result[0][0]).toBeCloseTo(38.5, 4);
    expect(result[0][1]).toBeCloseTo(-120.2, 4);

    expect(result[1][0]).toBeCloseTo(40.7, 4);
    expect(result[1][1]).toBeCloseTo(-120.95, 4);

    expect(result[2][0]).toBeCloseTo(43.252, 4);
    expect(result[2][1]).toBeCloseTo(-126.453, 4);
  });

  test("decodes a single point polyline", () => {
    const result = decodeEncodedPolyline("_p~iF~ps|U");
    expect(result).toHaveLength(1);
    expect(result[0][0]).toBeCloseTo(38.5, 4);
    expect(result[0][1]).toBeCloseTo(-120.2, 4);
  });
});

describe("parseRouteCoordinates", () => {
  test("returns [] for null", () => {
    expect(parseRouteCoordinates(null)).toEqual([]);
  });

  test("returns [] for undefined", () => {
    expect(parseRouteCoordinates(undefined)).toEqual([]);
  });

  test("returns [] for empty string", () => {
    expect(parseRouteCoordinates("")).toEqual([]);
  });

  test("returns [] for whitespace-only string", () => {
    expect(parseRouteCoordinates("   ")).toEqual([]);
  });

  test("returns already-parsed array as-is", () => {
    const coords = [[1, 2], [3, 4]];
    expect(parseRouteCoordinates(coords)).toBe(coords);
  });

  test("returns empty array as-is", () => {
    const coords = [];
    expect(parseRouteCoordinates(coords)).toBe(coords);
  });

  test("parses JSON-array-string format", () => {
    const result = parseRouteCoordinates("[[52.2,21.0],[52.3,21.1]]");
    expect(result).toEqual([[52.2, 21.0], [52.3, 21.1]]);
  });

  test("decodes Google Encoded Polyline format", () => {
    const result = parseRouteCoordinates("_p~iF~ps|U");
    expect(result).toHaveLength(1);
    expect(result[0][0]).toBeCloseTo(38.5, 4);
    expect(result[0][1]).toBeCloseTo(-120.2, 4);
  });

  test("returns [] for malformed JSON starting with '[' (does not throw)", () => {
    // Starts with '[' so it takes the JSON branch; the JSON is invalid and should fall through to [].
    expect(() => parseRouteCoordinates("[not valid json")).not.toThrow();
    expect(parseRouteCoordinates("[not valid json")).toEqual([]);
  });

  test("does not throw on arbitrary invalid strings", () => {
    // Any other input takes the encoded-polyline branch; even gibberish should not throw,
    // it will return whatever the decoder produces (may be garbage, but no exception).
    expect(() => parseRouteCoordinates("not valid json [")).not.toThrow();
  });

  test("returns the parsed JSON array when it contains objects (documented behavior)", () => {
    // The function returns whatever JSON.parse produces as long as it's an Array.
    // It does not validate that every element is a [lat, lng] pair.
    expect(parseRouteCoordinates('[{"not": "coords"}]')).toEqual([{ not: "coords" }]);
  });

  test("returns [] for numeric input", () => {
    expect(parseRouteCoordinates(42)).toEqual([]);
  });
});
