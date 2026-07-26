import React from "react";
import { render, screen } from "@testing-library/react";
import { UnitsProvider, useUnits } from "./UnitsContext";

let mockUnits = "metric";

jest.mock("../api/SettingsService", () => ({
  __esModule: true,
  default: {
    getPreferences: () => ({ units: mockUnits }),
  },
}));

const PaceProbe = ({ distanceKm, durationSeconds, activityType }) => {
  const { formatPaceFromActivity } = useUnits();
  return <span data-testid="pace">{formatPaceFromActivity(distanceKm, durationSeconds, activityType)}</span>;
};

const renderPace = (distanceKm, durationSeconds, activityType) => {
  render(
    <UnitsProvider>
      <PaceProbe distanceKm={distanceKm} durationSeconds={durationSeconds} activityType={activityType} />
    </UnitsProvider>,
  );
  return screen.getByTestId("pace").textContent;
};

describe("UnitsContext formatPaceFromActivity", () => {
  describe("metric", () => {
    beforeEach(() => {
      mockUnits = "metric";
    });

    it("formats run pace as min per km", () => {
      expect(renderPace(10, 3000, "RUN")).toBe("5:00 /km");
    });

    it("formats walk and hike as pace", () => {
      expect(renderPace(5, 3000, "WALK")).toBe("10:00 /km");
    });

    it("formats swim pace per 100m", () => {
      expect(renderPace(1, 1500, "SWIM")).toBe("2:30 /100m");
    });

    it("formats ride as speed in km/h", () => {
      expect(renderPace(35, 5400, "RIDE")).toBe("23.3 km/h");
    });

    it("returns a dash for missing data", () => {
      expect(renderPace(0, 3000, "RUN")).toBe("—");
    });
  });

  describe("imperial", () => {
    beforeEach(() => {
      mockUnits = "imperial";
    });

    it("formats run pace as min per mile", () => {
      expect(renderPace(10, 3000, "RUN")).toBe("8:03 /mi");
    });

    it("formats swim pace per 100yd", () => {
      expect(renderPace(1, 1500, "SWIM")).toBe("2:17 /100yd");
    });

    it("formats ride as speed in mph", () => {
      expect(renderPace(35, 5400, "RIDE")).toBe("14.5 mph");
    });
  });
});
