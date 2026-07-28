import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import FollowListModal from "./FollowListModal";
import UserService from "../../api/UserService";

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key) => key }),
}));

jest.mock("../../i18n", () => ({
  t: (key) => key,
}));

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  useNavigate: () => mockNavigate,
}));

jest.mock("../../api/UserService", () => ({
  getFollowers: jest.fn(),
  getFollowing: jest.fn(),
}));

const users = [
  { id: 7, firstName: "Jane", lastName: "Smith", avatar: null, city: "New York" },
  { id: 9, firstName: "Bob", lastName: "Runner", avatar: null, city: null },
];

describe("FollowListModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders nothing when closed and fetches nothing", () => {
    render(
      <FollowListModal isOpen={false} onClose={jest.fn()} userId={1} kind="followers" />,
    );
    expect(screen.queryByTestId("follow-list-modal")).not.toBeInTheDocument();
    expect(UserService.getFollowers).not.toHaveBeenCalled();
  });

  it("lists followers with initials and city", async () => {
    UserService.getFollowers.mockResolvedValue({ data: users });
    render(
      <FollowListModal isOpen onClose={jest.fn()} userId={1} kind="followers" />,
    );

    expect(await screen.findByText("Jane Smith")).toBeInTheDocument();
    expect(screen.getByText("JS")).toBeInTheDocument();
    expect(screen.getByText("New York")).toBeInTheDocument();
    expect(screen.getByText("profile.followList.followersTitle")).toBeInTheDocument();
    expect(UserService.getFollowers).toHaveBeenCalledWith(1, 0, 100);
  });

  it("uses the following endpoint for kind=following", async () => {
    UserService.getFollowing.mockResolvedValue({ data: [] });
    render(
      <FollowListModal isOpen onClose={jest.fn()} userId={1} kind="following" />,
    );

    expect(await screen.findByText("profile.followList.empty")).toBeInTheDocument();
    expect(UserService.getFollowing).toHaveBeenCalledWith(1, 0, 100);
    expect(UserService.getFollowers).not.toHaveBeenCalled();
  });

  it("navigates to the tapped athlete's profile and closes", async () => {
    UserService.getFollowers.mockResolvedValue({ data: users });
    const onClose = jest.fn();
    render(
      <FollowListModal isOpen onClose={onClose} userId={1} kind="followers" />,
    );

    fireEvent.click(await screen.findByText("Jane Smith"));
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/profile/7");
    });
    expect(onClose).toHaveBeenCalled();
  });

  it("shows the error state when the request fails", async () => {
    UserService.getFollowers.mockRejectedValue(new Error("boom"));
    render(
      <FollowListModal isOpen onClose={jest.fn()} userId={1} kind="followers" />,
    );

    expect(await screen.findByText("profile.followList.error")).toBeInTheDocument();
  });
});
