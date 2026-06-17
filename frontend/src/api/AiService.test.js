jest.mock("./ApiClient", () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    delete: jest.fn(),
  },
}));

import apiClient from "./ApiClient";
import aiService from "./AiService";

describe("AiService activity chat", () => {
  beforeEach(() => {
    apiClient.get.mockReset().mockResolvedValue({ data: { messages: [] } });
    apiClient.post.mockReset().mockResolvedValue({ data: { messages: [] } });
    apiClient.delete.mockReset().mockResolvedValue({ data: null });
  });

  test("getActivityChat GETs the conversation endpoint", async () => {
    await aiService.getActivityChat(42);
    expect(apiClient.get).toHaveBeenCalledWith("/ai/activities/42/chat");
  });

  test("sendActivityChatMessage POSTs the message body", async () => {
    await aiService.sendActivityChatMessage(42, "Analyze this activity.");
    expect(apiClient.post).toHaveBeenCalledWith("/ai/activities/42/chat", {
      message: "Analyze this activity.",
    });
  });

  test("clearActivityChat DELETEs the conversation endpoint", async () => {
    await aiService.clearActivityChat(42);
    expect(apiClient.delete).toHaveBeenCalledWith("/ai/activities/42/chat");
  });
});
