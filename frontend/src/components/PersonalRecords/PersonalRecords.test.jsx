import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";

jest.mock("../../api/PersonalRecordService", () => ({
  __esModule: true,
  default: {
    getMyPersonalRecords: jest.fn(),
    getUserPersonalRecords: jest.fn(),
    recalculateAllPRs: jest.fn(),
  },
}));

import PersonalRecordService from "../../api/PersonalRecordService";
import PersonalRecords from "./PersonalRecords";

const renderWithRouter = (ui) =>
  render(<MemoryRouter>{ui}</MemoryRouter>);

const samplePR = (overrides = {}) => ({
  id: 1,
  activityId: 100,
  activityType: "RUN",
  distanceType: "FIVE_KM",
  distance: 5,
  distanceDisplayName: "5 km",
  formattedTime: "25:00",
  formattedPace: "5:00 /km",
  improvementSeconds: null,
  improvementPercent: null,
  achievedAt: "2025-01-15T10:00:00Z",
  ...overrides,
});

describe("PersonalRecords", () => {
  beforeEach(() => {
    PersonalRecordService.getMyPersonalRecords.mockReset();
    PersonalRecordService.getUserPersonalRecords.mockReset();
    PersonalRecordService.recalculateAllPRs.mockReset();
  });

  test("shows loading state while fetching", () => {
    // Never-resolving promise keeps the component in the loading state
    PersonalRecordService.getMyPersonalRecords.mockReturnValue(
      new Promise(() => {})
    );

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    expect(screen.getByText(/Loading personal records\.\.\./i)).toBeInTheDocument();
  });

  test("shows empty state for own profile with recalculate button", async () => {
    PersonalRecordService.getMyPersonalRecords.mockResolvedValue({ data: [] });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(screen.getByText(/No personal records yet/i)).toBeInTheDocument();
    });

    // Recalculate-from-history button should be present
    expect(
      screen.getByRole("button", { name: /Recalculate from history/i })
    ).toBeInTheDocument();
  });

  test("empty state for other user's profile shows their message and NO recalculate button", async () => {
    PersonalRecordService.getUserPersonalRecords.mockResolvedValue({ data: [] });

    renderWithRouter(<PersonalRecords userId={2} isOwnProfile={false} />);

    await waitFor(() => {
      expect(
        screen.getByText(/This user hasn't set any personal records yet/i)
      ).toBeInTheDocument();
    });

    // No recalculate button at all
    expect(screen.queryByRole("button", { name: /Recalculate/i })).not.toBeInTheDocument();
  });

  test("calls getMyPersonalRecords when isOwnProfile is true", async () => {
    PersonalRecordService.getMyPersonalRecords.mockResolvedValue({ data: [] });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(PersonalRecordService.getMyPersonalRecords).toHaveBeenCalledTimes(1);
    });
    expect(PersonalRecordService.getUserPersonalRecords).not.toHaveBeenCalled();
  });

  test("calls getUserPersonalRecords when isOwnProfile is false", async () => {
    PersonalRecordService.getUserPersonalRecords.mockResolvedValue({ data: [] });

    renderWithRouter(<PersonalRecords userId={42} isOwnProfile={false} />);

    await waitFor(() => {
      expect(PersonalRecordService.getUserPersonalRecords).toHaveBeenCalledWith(42);
    });
    expect(PersonalRecordService.getMyPersonalRecords).not.toHaveBeenCalled();
  });

  test("groups records by sport and shows a 'Running' header for RUN activities", async () => {
    PersonalRecordService.getMyPersonalRecords.mockResolvedValue({
      data: [
        samplePR({ id: 1, activityType: "RUN", distanceType: "ONE_KM", distance: 1, formattedTime: "4:00" }),
        samplePR({ id: 2, activityType: "RUN", distanceType: "FIVE_KM", distance: 5, formattedTime: "22:30" }),
        samplePR({ id: 3, activityType: "RIDE", distanceType: "TEN_KM", distance: 10, formattedTime: "20:00" }),
      ],
    });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(screen.getByText("Running")).toBeInTheDocument();
    });
    expect(screen.getByText("Cycling")).toBeInTheDocument();
    expect(screen.getByText("2 PRs")).toBeInTheDocument(); // running
    expect(screen.getByText("1 PR")).toBeInTheDocument();  // cycling
  });

  test("renders one row per record with the formatted time", async () => {
    PersonalRecordService.getMyPersonalRecords.mockResolvedValue({
      data: [
        samplePR({ id: 1, formattedTime: "22:30" }),
        samplePR({ id: 2, distanceType: "TEN_KM", distance: 10, formattedTime: "47:15" }),
      ],
    });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(screen.getByText("22:30")).toBeInTheDocument();
    });
    expect(screen.getByText("47:15")).toBeInTheDocument();
  });

  test("shows header-level Recalculate button only when own profile AND records exist", async () => {
    PersonalRecordService.getMyPersonalRecords.mockResolvedValue({
      data: [samplePR()],
    });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(screen.getByText("Running")).toBeInTheDocument();
    });

    const recalcButtons = screen.getAllByRole("button", { name: /Recalculate/i });
    expect(recalcButtons.length).toBeGreaterThanOrEqual(1);
  });

  test("does NOT show header Recalculate button for another user's profile", async () => {
    PersonalRecordService.getUserPersonalRecords.mockResolvedValue({
      data: [samplePR()],
    });

    renderWithRouter(<PersonalRecords userId={2} isOwnProfile={false} />);

    await waitFor(() => {
      expect(screen.getByText("Running")).toBeInTheDocument();
    });

    expect(
      screen.queryByRole("button", { name: /Recalculate/i })
    ).not.toBeInTheDocument();
  });

  test("clicking Recalculate triggers recalculateAllPRs and refetches", async () => {
    PersonalRecordService.getMyPersonalRecords
      .mockResolvedValueOnce({ data: [samplePR({ formattedTime: "25:00" })] })
      .mockResolvedValueOnce({ data: [samplePR({ formattedTime: "24:30" })] });
    PersonalRecordService.recalculateAllPRs.mockResolvedValue({ data: {} });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(screen.getByText("25:00")).toBeInTheDocument();
    });

    const recalcButton = screen.getByRole("button", { name: /^Recalculate$/i });
    fireEvent.click(recalcButton);

    await waitFor(() => {
      expect(PersonalRecordService.recalculateAllPRs).toHaveBeenCalledTimes(1);
    });

    // After recalculation, fetchRecords runs a second time
    await waitFor(() => {
      expect(PersonalRecordService.getMyPersonalRecords).toHaveBeenCalledTimes(2);
    });

    await waitFor(() => {
      expect(screen.getByText("24:30")).toBeInTheDocument();
    });
  });

  test("renders improvement badge when improvementSeconds > 0", async () => {
    PersonalRecordService.getMyPersonalRecords.mockResolvedValue({
      data: [
        samplePR({
          improvementSeconds: 12,
          improvementPercent: 1.5,
        }),
      ],
    });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(screen.getByText(/−12s/)).toBeInTheDocument();
    });
    expect(screen.getByText(/\(1\.5%\)/)).toBeInTheDocument();
  });

  test("does not render improvement badge when improvementSeconds is null", async () => {
    PersonalRecordService.getMyPersonalRecords.mockResolvedValue({
      data: [samplePR({ improvementSeconds: null })],
    });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(screen.getByText("Running")).toBeInTheDocument();
    });

    expect(screen.queryByText(/−\d+s/)).not.toBeInTheDocument();
  });

  test("date cell renders as a link to /activities/{activityId}", async () => {
    PersonalRecordService.getMyPersonalRecords.mockResolvedValue({
      data: [samplePR({ activityId: 777, achievedAt: "2025-01-15T12:00:00Z" })],
    });

    renderWithRouter(<PersonalRecords userId={1} isOwnProfile={true} />);

    await waitFor(() => {
      expect(screen.getByText("Running")).toBeInTheDocument();
    });

    // Find the link inside the date cell
    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("href", "/activities/777");
  });
});
