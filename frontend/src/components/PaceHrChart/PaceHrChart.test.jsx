import React from "react";
import { render, screen } from "@testing-library/react";
import PaceHrChart, { formatPaceTick, formatPaceValue } from "./PaceHrChart";

// recharts' ResponsiveContainer needs ResizeObserver, which jsdom lacks.
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

jest.mock("../../context/UnitsContext", () => ({
  useUnits: () => ({ convertDistance: (km) => km, distanceUnit: "km" }),
}));

describe("formatPaceValue", () => {
  it("formats per the pipeline contract units", () => {
    expect(formatPaceValue("run", 284)).toBe("4:44 /km");
    expect(formatPaceValue("swim", 140)).toBe("2:20 /100m");
    expect(formatPaceValue("bike", 22.63)).toBe("22.6 km/h");
    expect(formatPaceValue("run", null)).toBe("");
  });

  it("keeps axis ticks compact", () => {
    expect(formatPaceTick("run", 284)).toBe("4:44");
    expect(formatPaceTick("bike", 22.63)).toBe("23");
  });
});

describe("PaceHrChart", () => {
  const detail = {
    activity: { discipline: "run" },
    paceSeries: [280, 290, 300, 285],
    heartRateSeries: [150, 160, 165, 158],
  };

  it("renders the section when the pipeline has series", () => {
    render(<PaceHrChart detail={detail} totalDistanceKm={6} />);
    expect(screen.getByText("activities.details.paceHr.title")).toBeInTheDocument();
  });

  it("renders nothing without pipeline data or with empty series", () => {
    const { container } = render(<PaceHrChart detail={null} totalDistanceKm={6} />);
    expect(container).toBeEmptyDOMElement();

    const { container: zeros } = render(
      <PaceHrChart
        detail={{ activity: { discipline: "run" }, paceSeries: [0, 0], heartRateSeries: [0, 0] }}
        totalDistanceKm={6}
      />,
    );
    expect(zeros).toBeEmptyDOMElement();
  });
});
