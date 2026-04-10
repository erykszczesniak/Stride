jest.mock("./ApiClient", () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

import apiClient from "./ApiClient";
import personalRecordService from "./PersonalRecordService";

describe("PersonalRecordService", () => {
  beforeEach(() => {
    apiClient.get.mockReset();
    apiClient.post.mockReset();
    apiClient.get.mockResolvedValue({ data: [] });
    apiClient.post.mockResolvedValue({ data: {} });
  });

  test("getMyPersonalRecords GETs /personal-records", async () => {
    await personalRecordService.getMyPersonalRecords();
    expect(apiClient.get).toHaveBeenCalledTimes(1);
    expect(apiClient.get).toHaveBeenCalledWith("/personal-records");
  });

  test("getUserPersonalRecords GETs /personal-records/user/{userId}", async () => {
    await personalRecordService.getUserPersonalRecords(42);
    expect(apiClient.get).toHaveBeenCalledTimes(1);
    expect(apiClient.get).toHaveBeenCalledWith("/personal-records/user/42");
  });

  test("getUserPersonalRecords supports string userId", async () => {
    await personalRecordService.getUserPersonalRecords("abc");
    expect(apiClient.get).toHaveBeenCalledWith("/personal-records/user/abc");
  });

  test("getPersonalRecordsByActivityType GETs /personal-records/activity-type/{type}", async () => {
    await personalRecordService.getPersonalRecordsByActivityType("RUN");
    expect(apiClient.get).toHaveBeenCalledTimes(1);
    expect(apiClient.get).toHaveBeenCalledWith("/personal-records/activity-type/RUN");
  });

  test("recalculateAllPRs POSTs /personal-records/recalculate", async () => {
    await personalRecordService.recalculateAllPRs();
    expect(apiClient.post).toHaveBeenCalledTimes(1);
    expect(apiClient.post).toHaveBeenCalledWith("/personal-records/recalculate");
  });
});
