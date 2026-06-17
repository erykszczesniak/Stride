import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key) => key }),
}));

jest.mock("../../api/AiService", () => ({
  __esModule: true,
  default: {
    getActivityChat: jest.fn(),
    sendActivityChatMessage: jest.fn(),
    clearActivityChat: jest.fn(),
  },
}));

import AiService from "../../api/AiService";
import ActivityAiChat from "./ActivityAiChat";

describe("ActivityAiChat", () => {
  beforeEach(() => {
    AiService.getActivityChat.mockReset().mockResolvedValue({ data: { messages: [] } });
    AiService.sendActivityChatMessage.mockReset();
    AiService.clearActivityChat.mockReset().mockResolvedValue({ data: null });
  });

  test("loads the existing conversation on mount", async () => {
    AiService.getActivityChat.mockResolvedValue({
      data: {
        messages: [
          { role: "USER", content: "Analyze this activity." },
          { role: "ASSISTANT", content: "Strong aerobic ride." },
        ],
      },
    });

    render(<ActivityAiChat activityId={7} />);

    expect(AiService.getActivityChat).toHaveBeenCalledWith(7);
    await waitFor(() =>
      expect(screen.getByText("Strong aerobic ride.")).toBeInTheDocument()
    );
    expect(screen.getByText("Analyze this activity.")).toBeInTheDocument();
  });

  test("shows the empty state and suggestions when there is no history", async () => {
    render(<ActivityAiChat activityId={7} />);
    await waitFor(() =>
      expect(screen.getByText("aiChat.empty")).toBeInTheDocument()
    );
    expect(screen.getByText("aiChat.suggestions.analyze")).toBeInTheDocument();
  });

  test("sends a typed message and renders the reply", async () => {
    AiService.sendActivityChatMessage.mockResolvedValue({
      data: {
        messages: [
          { role: "USER", content: "How was my pacing?" },
          { role: "ASSISTANT", content: "Even splits — nicely controlled." },
        ],
      },
    });

    render(<ActivityAiChat activityId={7} />);
    await waitFor(() => expect(screen.getByText("aiChat.empty")).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText("aiChat.placeholder"), {
      target: { value: "How was my pacing?" },
    });
    fireEvent.click(screen.getByLabelText("aiChat.send"));

    expect(AiService.sendActivityChatMessage).toHaveBeenCalledWith(7, "How was my pacing?");
    await waitFor(() =>
      expect(screen.getByText("Even splits — nicely controlled.")).toBeInTheDocument()
    );
  });

  test("sends a suggestion chip as the message", async () => {
    AiService.sendActivityChatMessage.mockResolvedValue({
      data: { messages: [{ role: "ASSISTANT", content: "Done" }] },
    });

    render(<ActivityAiChat activityId={9} />);
    await waitFor(() =>
      expect(screen.getByText("aiChat.suggestions.analyze")).toBeInTheDocument()
    );

    fireEvent.click(screen.getByText("aiChat.suggestions.analyze"));

    expect(AiService.sendActivityChatMessage).toHaveBeenCalledWith(
      9,
      "aiChat.suggestions.analyze"
    );
  });

  test("surfaces an error and rolls back the optimistic message on failure", async () => {
    AiService.sendActivityChatMessage.mockRejectedValue(new Error("boom"));

    render(<ActivityAiChat activityId={7} />);
    await waitFor(() => expect(screen.getByText("aiChat.empty")).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText("aiChat.placeholder"), {
      target: { value: "Will this fail?" },
    });
    fireEvent.click(screen.getByLabelText("aiChat.send"));

    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("aiChat.error"));
    // Optimistic user bubble was rolled back.
    expect(screen.queryByText("Will this fail?")).not.toBeInTheDocument();
  });

  test("clears the conversation", async () => {
    AiService.getActivityChat.mockResolvedValue({
      data: { messages: [{ role: "ASSISTANT", content: "Some analysis" }] },
    });

    render(<ActivityAiChat activityId={7} />);
    await waitFor(() => expect(screen.getByText("Some analysis")).toBeInTheDocument());

    fireEvent.click(screen.getByLabelText("aiChat.clear"));

    expect(AiService.clearActivityChat).toHaveBeenCalledWith(7);
    await waitFor(() =>
      expect(screen.queryByText("Some analysis")).not.toBeInTheDocument()
    );
  });
});
