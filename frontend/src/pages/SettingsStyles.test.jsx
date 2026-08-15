import React from "react";
import { render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { darkTheme, lightTheme } from "../styles/theme";
import {
  IntegrationDetail,
  IntegrationName,
  IntegrationRow,
  SecondaryButton,
} from "./SettingsStyles";

// The integration styles once referenced theme keys that don't exist
// (theme.text, theme.cardBg), so the colors silently fell through and
// "Strava / Not connected" rendered near-invisible in dark mode.
describe.each([
  ["dark", darkTheme],
  ["light", lightTheme],
])("Strava integration styles (%s theme)", (_name, theme) => {
  const renderWithTheme = (ui) => render(<ThemeProvider theme={theme}>{ui}</ThemeProvider>);

  it("colors the service name with the theme's primary text", () => {
    renderWithTheme(<IntegrationName>Strava</IntegrationName>);
    expect(screen.getByText("Strava")).toHaveStyle(`color: ${theme.textPrimary}`);
  });

  it("colors the status line with the theme's secondary text", () => {
    renderWithTheme(<IntegrationDetail>Not connected</IntegrationDetail>);
    expect(screen.getByText("Not connected")).toHaveStyle(`color: ${theme.textSecondary}`);
  });

  it("gives the row the theme's card background", () => {
    renderWithTheme(<IntegrationRow data-testid="row" />);
    expect(screen.getByTestId("row")).toHaveStyle(`background: ${theme.bgCard}`);
  });

  it("colors the secondary button's label with the theme's primary text", () => {
    renderWithTheme(<SecondaryButton>Disconnect</SecondaryButton>);
    expect(screen.getByText("Disconnect")).toHaveStyle(`color: ${theme.textPrimary}`);
  });
});
