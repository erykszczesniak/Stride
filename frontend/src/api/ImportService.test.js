jest.mock("./ApiClient", () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

import apiClient from "./ApiClient";
import importService from "./ImportService";

describe("ImportService", () => {
  beforeEach(() => {
    apiClient.get.mockReset();
    apiClient.post.mockReset();
    apiClient.get.mockResolvedValue({ data: [] });
    apiClient.post.mockResolvedValue({ data: {} });
  });

  describe("importActivity", () => {
    test("POSTs to /import/activities with FormData containing the file", async () => {
      const file = new File(["hello"], "run.gpx", { type: "application/gpx+xml" });
      await importService.importActivity(file);

      expect(apiClient.post).toHaveBeenCalledTimes(1);
      const [url, formData, config] = apiClient.post.mock.calls[0];

      expect(url).toBe("/import/activities");
      expect(formData).toBeInstanceOf(FormData);
      expect(formData.get("file")).toBe(file);
      expect(config).toEqual({
        headers: { "Content-Type": "multipart/form-data" },
      });
    });

    test("includes title and activityType when provided", async () => {
      const file = new File(["data"], "ride.tcx");
      await importService.importActivity(file, {
        title: "My Morning Ride",
        activityType: "RIDE",
      });

      const [, formData] = apiClient.post.mock.calls[0];
      expect(formData.get("title")).toBe("My Morning Ride");
      expect(formData.get("activityType")).toBe("RIDE");
    });

    test("includes isPrivate flag when provided (even false)", async () => {
      const file = new File(["data"], "run.gpx");
      await importService.importActivity(file, { isPrivate: false });

      const [, formData] = apiClient.post.mock.calls[0];
      // FormData stringifies booleans
      expect(formData.get("isPrivate")).toBe("false");
    });

    test("does NOT include isPrivate when option is undefined", async () => {
      const file = new File(["data"], "run.gpx");
      await importService.importActivity(file, {});

      const [, formData] = apiClient.post.mock.calls[0];
      expect(formData.get("isPrivate")).toBeNull();
    });

    test("sets Content-Type header to multipart/form-data", async () => {
      const file = new File(["data"], "run.gpx");
      await importService.importActivity(file);

      const [, , config] = apiClient.post.mock.calls[0];
      expect(config.headers["Content-Type"]).toBe("multipart/form-data");
    });
  });

  describe("listSampleFiles", () => {
    test("GETs /import/samples", async () => {
      await importService.listSampleFiles();
      expect(apiClient.get).toHaveBeenCalledTimes(1);
      expect(apiClient.get).toHaveBeenCalledWith("/import/samples");
    });
  });

  describe("downloadSampleFile", () => {
    test("GETs /import/samples/{filename} with responseType blob", async () => {
      await importService.downloadSampleFile("foo.gpx");
      expect(apiClient.get).toHaveBeenCalledTimes(1);
      expect(apiClient.get).toHaveBeenCalledWith("/import/samples/foo.gpx", {
        responseType: "blob",
      });
    });
  });
});
