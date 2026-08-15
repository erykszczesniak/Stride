import React from "react";
import { render, screen } from "@testing-library/react";
import WeeklyVolume, {
  VOLUME_CHART_MARGIN,
  formatWeekRange,
  toWeeklyVolumeData,
} from "./WeeklyVolume";

beforeAll(() => {
  global.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
});

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key) => key }),
}));

const volumes = [
  { weekStart: "2026-06-29T00:00:00Z", swimKm: 1.5, bikeKm: 40.2, runKm: 12 },
  { weekStart: "2026-07-06T00:00:00Z", swimKm: 0, bikeKm: 67.2, runKm: 19.5 },
];

describe("formatWeekRange", () => {
  it("renders a same-month week as day-day.month", () => {
    expect(formatWeekRange("2026-05-04T00:00:00")).toBe("4-11.05");
  });

  it("spells out both months when the week crosses one", () => {
    expect(formatWeekRange("2026-06-29T00:00:00")).toBe("29.06-6.07");
  });
});

describe("toWeeklyVolumeData", () => {
  it("maps pipeline weeks to week-range rows", () => {
    const data = toWeeklyVolumeData(volumes);

    expect(data).toHaveLength(2);
    expect(data[0]).toMatchObject({ swim: 1.5, bike: 40.2, run: 12 });
    expect(data[0].week).toBe(formatWeekRange(volumes[0].weekStart));
  });

  it("defaults missing sports to zero and handles null input", () => {
    expect(toWeeklyVolumeData([{ weekStart: "2026-07-06T00:00:00Z" }])[0]).toMatchObject({
      swim: 0,
      bike: 0,
      run: 0,
    });
    expect(toWeeklyVolumeData(null)).toEqual([]);
  });
});

describe("WeeklyVolume", () => {
  it("renders the title and the three-sport legend", () => {
    render(<WeeklyVolume weeklyVolumes={volumes} />);

    expect(screen.getByText("dashboard.weeklyVolume.title")).toBeInTheDocument();
    expect(screen.getByText("dashboard.weeklyVolume.swim")).toBeInTheDocument();
    expect(screen.getByText("dashboard.weeklyVolume.bike")).toBeInTheDocument();
    expect(screen.getByText("dashboard.weeklyVolume.run")).toBeInTheDocument();
  });

  it("renders nothing without pipeline data", () => {
    const { container } = render(<WeeklyVolume weeklyVolumes={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("keeps the Y-axis labels inside the chart", () => {
    // A negative left margin shifted the plot over the Y-axis labels and
    // clipped them (e.g. "120 km" rendered as "20 km").
    expect(VOLUME_CHART_MARGIN.left).toBeGreaterThanOrEqual(0);
  });
});
