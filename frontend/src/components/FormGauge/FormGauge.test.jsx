import React from "react";
import { render, screen } from "@testing-library/react";
import FormGauge, { formStateFor } from "./FormGauge";

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key) => key }),
}));

describe("formStateFor", () => {
  it("uses the iOS dial thresholds", () => {
    expect(formStateFor(6)).toBe("fresh");
    expect(formStateFor(5)).toBe("neutral");
    expect(formStateFor(-10)).toBe("neutral");
    expect(formStateFor(-11)).toBe("fatigued");
  });
});

describe("FormGauge", () => {
  it("renders nothing without form data", () => {
    render(<FormGauge form={null} />);
    expect(screen.queryByTestId("form-gauge")).not.toBeInTheDocument();
  });

  it("shows the three numbers with a signed TSB", () => {
    render(<FormGauge form={{ ctl: 8, atl: 31, tsb: -23 }} />);

    expect(screen.getByText("8")).toBeInTheDocument();
    expect(screen.getByText("31")).toBeInTheDocument();
    expect(screen.getByText("-23")).toBeInTheDocument();
    expect(screen.getByText("dashboard.form.fatigued")).toBeInTheDocument();
  });

  it("prefixes positive TSB with a plus and reads fresh", () => {
    render(<FormGauge form={{ ctl: 45, atl: 38, tsb: 7 }} />);

    expect(screen.getByText("+7")).toBeInTheDocument();
    expect(screen.getByText("dashboard.form.fresh")).toBeInTheDocument();
  });

  it("centers the scale labels below the arc ends", () => {
    render(<FormGauge form={{ ctl: 7, atl: 4, tsb: 3 }} />);

    const low = screen.getByText("-30");
    const high = screen.getByText("+30");

    // Centered on the arc endpoints (x=30 / x=170), clearly below the
    // arc's bottom edge (y=95) so they don't touch the rounded caps
    expect(low).toHaveAttribute("text-anchor", "middle");
    expect(high).toHaveAttribute("text-anchor", "middle");
    expect(low).toHaveAttribute("x", "30");
    expect(high).toHaveAttribute("x", "170");
    expect(Number(low.getAttribute("y"))).toBeGreaterThan(104);
    expect(Number(high.getAttribute("y"))).toBeGreaterThan(104);
  });
});
