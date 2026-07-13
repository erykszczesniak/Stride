import React from "react";
import { render, screen } from "@testing-library/react";
import TrainingLoadChart, { toTrainingLoadData } from "./TrainingLoadChart";

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

const ctl = [
  { date: "2026-06-29T00:00:00Z", value: 7.6 },
  { date: "2026-07-06T00:00:00Z", value: 8.4 },
];
const atl = [
  { date: "2026-06-29T00:00:00Z", value: 30.9 },
  { date: "2026-07-06T00:00:00Z", value: 26.2 },
];

describe("toTrainingLoadData", () => {
  it("merges the two point series into rounded rows", () => {
    const data = toTrainingLoadData(ctl, atl);

    expect(data).toHaveLength(2);
    expect(data[0]).toMatchObject({ ctl: 8, atl: 31 });
    expect(typeof data[0].date).toBe("string");
  });

  it("handles a missing series and null input", () => {
    expect(toTrainingLoadData(ctl, null)[0].atl).toBeUndefined();
    expect(toTrainingLoadData(null, null)).toEqual([]);
  });
});

describe("TrainingLoadChart", () => {
  it("renders the title with data", () => {
    render(<TrainingLoadChart ctl={ctl} atl={atl} />);
    expect(screen.getByText("dashboard.trainingLoad.title")).toBeInTheDocument();
  });

  it("renders nothing without enough points", () => {
    const { container } = render(<TrainingLoadChart ctl={[ctl[0]]} atl={null} />);
    expect(container).toBeEmptyDOMElement();
  });
});
