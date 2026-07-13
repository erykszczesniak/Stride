import { elevationDomain } from "./ElevationProfile";

describe("elevationDomain", () => {
  it("hugs the altitude band instead of anchoring at zero", () => {
    const [low, high] = elevationDomain([87, 90, 94, 97]);

    expect(low).toBeGreaterThan(80);
    expect(low).toBeLessThanOrEqual(86);
    expect(high).toBeGreaterThanOrEqual(98);
    expect(high).toBeLessThan(104);
  });

  it("keeps a minimum breathing room on flat profiles", () => {
    const [low, high] = elevationDomain([90, 90, 90]);

    expect(low).toBe(88);
    expect(high).toBe(92);
  });

  it("degrades to an empty band without data", () => {
    expect(elevationDomain([])).toEqual([0, 0]);
    expect(elevationDomain(null)).toEqual([0, 0]);
  });
});
