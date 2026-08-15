import React from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import ActivityCard from "./ActivityCard";

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key, options) => (options?.count != null ? `${key}:${options.count}` : key) }),
}));

jest.mock("../../i18n", () => ({
  t: (key, options) => (options?.count != null ? `${key}:${options.count}` : key),
}));

jest.mock("../../context/UnitsContext", () => ({
  useUnits: () => ({
    convertDistance: (km) => km,
    distanceUnit: "km",
    convertElevation: (m) => m,
    elevationUnit: "m",
  }),
}));

jest.mock("../ActivityMap/ActivityMap", () => () => (
  <div data-testid="activity-map" />
));

const HOUR = 3600000;

const baseActivity = (overrides = {}) => ({
  id: 1,
  title: "Interval Training",
  activityType: "RUN",
  distance: 6,
  duration: 1800,
  likesCount: 2,
  commentsCount: 0,
  likedByCurrentUser: false,
  user: { id: 1, firstName: "Pan", lastName: "Szczesniak" },
  startTime: new Date(Date.now() - 7 * HOUR).toISOString(),
  createdAt: new Date(Date.now() - 72 * HOUR).toISOString(),
  ...overrides,
});

const renderCard = (activity) =>
  render(
    <MemoryRouter>
      <ActivityCard activity={activity} />
    </MemoryRouter>,
  );

describe("ActivityCard", () => {
  it("shows map and photo as a side-by-side media row when both exist", () => {
    renderCard(baseActivity({ routePolyline: "abc", photos: ["/uploads/a.jpg"] }));

    expect(screen.getByTestId("activity-map")).toBeInTheDocument();
    expect(screen.getByAltText("Interval Training")).toBeInTheDocument();
  });

  it("keeps the full-width cover photo when there is no route", () => {
    renderCard(baseActivity({ photos: ["/uploads/a.jpg"] }));

    expect(screen.queryByTestId("activity-map")).not.toBeInTheDocument();
    expect(screen.getByAltText("Interval Training")).toBeInTheDocument();
  });

  it("keeps the full-width map when there are no photos", () => {
    renderCard(baseActivity({ routePolyline: "abc" }));

    expect(screen.getByTestId("activity-map")).toBeInTheDocument();
    expect(screen.queryByAltText("Interval Training")).not.toBeInTheDocument();
  });

  it("dates the card from the workout start time, not the record creation", () => {
    renderCard(baseActivity());

    // 7 hours ago (startTime), not 3 days ago (createdAt).
    expect(screen.getByText("time.hoursAgo:7")).toBeInTheDocument();
  });
});
