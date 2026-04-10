import {
  formatDuration,
  formatDurationValue,
  formatDurationUnit,
  formatDistance,
  isPaceActivity,
  formatPace,
  formatDate,
  getInitials,
} from "./formatters";

describe("formatDuration", () => {
  test("returns '0m' for 0, null, undefined", () => {
    expect(formatDuration(0)).toBe("0m");
    expect(formatDuration(null)).toBe("0m");
    expect(formatDuration(undefined)).toBe("0m");
  });

  test("formats seconds under an hour as minutes only", () => {
    expect(formatDuration(60)).toBe("1m");
    expect(formatDuration(2700)).toBe("45m");
  });

  test("formats seconds over an hour as hours and minutes", () => {
    expect(formatDuration(3600)).toBe("1h 0m");
    expect(formatDuration(9000)).toBe("2h 30m");
  });
});

describe("formatDurationValue", () => {
  test("returns '0' for falsy input", () => {
    expect(formatDurationValue(0)).toBe("0");
    expect(formatDurationValue(null)).toBe("0");
  });

  test("returns minutes when under an hour", () => {
    expect(formatDurationValue(1800)).toBe("30");
  });

  test("returns hours when over an hour", () => {
    expect(formatDurationValue(7200)).toBe("2");
  });
});

describe("formatDurationUnit", () => {
  test("defaults to 'hours' for falsy input", () => {
    expect(formatDurationUnit(0)).toBe("hours");
    expect(formatDurationUnit(null)).toBe("hours");
  });

  test("returns 'min' under an hour", () => {
    expect(formatDurationUnit(1800)).toBe("min");
  });

  test("returns 'hours' at or above an hour", () => {
    expect(formatDurationUnit(3600)).toBe("hours");
  });
});

describe("formatDistance", () => {
  test("formats a number with 2 decimal places and km suffix", () => {
    expect(formatDistance(5)).toBe("5.00 km");
    expect(formatDistance(5.234)).toBe("5.23 km");
  });

  test("handles null and undefined as 0 km", () => {
    expect(formatDistance(null)).toBe("0.00 km");
    expect(formatDistance(undefined)).toBe("0.00 km");
  });
});

describe("isPaceActivity", () => {
  test("returns true for RUN", () => {
    expect(isPaceActivity("RUN")).toBe(true);
  });

  test("returns true for VIRTUAL_RUN, WALK, HIKE, TRAIL_RUN", () => {
    expect(isPaceActivity("VIRTUAL_RUN")).toBe(true);
    expect(isPaceActivity("WALK")).toBe(true);
    expect(isPaceActivity("HIKE")).toBe(true);
    expect(isPaceActivity("TRAIL_RUN")).toBe(true);
  });

  test("returns false for RIDE", () => {
    expect(isPaceActivity("RIDE")).toBe(false);
  });

  test("returns false for SWIM, WORKOUT, unknown types", () => {
    expect(isPaceActivity("SWIM")).toBe(false);
    expect(isPaceActivity("WORKOUT")).toBe(false);
    expect(isPaceActivity("UNKNOWN")).toBe(false);
    expect(isPaceActivity(undefined)).toBe(false);
    expect(isPaceActivity(null)).toBe(false);
  });
});

describe("formatPace", () => {
  test("formats exact 5:00 pace (5 km in 1500s)", () => {
    expect(formatPace(5, 1500)).toBe("5:00 /km");
  });

  test("formats same pace for larger distance (10 km in 3000s)", () => {
    expect(formatPace(10, 3000)).toBe("5:00 /km");
  });

  test("formats an uneven pace (5 km in 1234s ~ 4:07)", () => {
    // 1234 / 5 = 246.8 s/km -> 4 minutes, ~46.8 s -> rounds to 47
    expect(formatPace(5, 1234)).toBe("4:07 /km");
  });

  test("pads seconds with leading zero", () => {
    // 5 km in 1500s + 15s = 1515s -> 303 s/km -> 5:03
    expect(formatPace(5, 1515)).toBe("5:03 /km");
  });

  test("returns em dash for zero distance", () => {
    expect(formatPace(0, 100)).toBe("—");
  });

  test("returns em dash for null distance", () => {
    expect(formatPace(null, 100)).toBe("—");
  });

  test("returns em dash for undefined distance", () => {
    expect(formatPace(undefined, 100)).toBe("—");
  });

  test("returns em dash for zero duration", () => {
    expect(formatPace(5, 0)).toBe("—");
  });

  test("returns em dash for null duration", () => {
    expect(formatPace(5, null)).toBe("—");
  });

  test("returns em dash for negative distance", () => {
    expect(formatPace(-1, 100)).toBe("—");
  });
});

describe("formatDate", () => {
  test("formats an ISO date string to en-US short format", () => {
    expect(formatDate("2025-01-15T12:00:00Z")).toMatch(/Jan 1[45], 2025/);
  });
});

describe("getInitials", () => {
  test("returns uppercase initials from first and last name", () => {
    expect(getInitials("John", "Smith")).toBe("JS");
  });

  test("handles missing last name", () => {
    expect(getInitials("John", "")).toBe("J");
    expect(getInitials("John", undefined)).toBe("J");
  });

  test("handles missing first name", () => {
    expect(getInitials("", "Smith")).toBe("S");
    expect(getInitials(undefined, "Smith")).toBe("S");
  });

  test("handles both missing", () => {
    expect(getInitials(undefined, undefined)).toBe("");
    expect(getInitials("", "")).toBe("");
  });
});
