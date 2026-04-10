import { test, expect } from "@playwright/test";
import { login } from "./helpers";
import * as path from "path";

const BACKEND = "http://localhost:8080/api/v1";
const SAMPLES_DIR = path.resolve(__dirname, "../../src/main/resources/sample-activities");

test.describe("Activity import", () => {
  let token: string;

  test.beforeEach(async ({ page }) => {
    token = await login(page, "john@example.com", "password123");
    await page.goto("/activities/new");
    await expect(
      page.locator("h1, h2").filter({ hasText: "Record Activity" })
    ).toBeVisible({ timeout: 10_000 });
  });

  test("shows Manual Entry / Upload File mode toggle on new activity", async ({ page }) => {
    await expect(page.getByRole("button", { name: /manual entry/i })).toBeVisible();
    await expect(page.getByRole("button", { name: /upload file/i })).toBeVisible();
  });

  test("switches to upload mode and shows dropzone + sample list", async ({ page }) => {
    await page.getByRole("button", { name: /upload file/i }).click();

    // Dropzone copy
    await expect(page.getByText(/click to choose a file or drag it here/i)).toBeVisible();
    await expect(
      page.getByText(/works for .*gpx.*tcx.*fit.*10\s?mb/i)
    ).toBeVisible();

    // Sample cards should load from the backend
    await expect(page.getByText("morning_run_5k.gpx")).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText("Night_Run.fit")).toBeVisible();
  });

  test("imports a GPX file and lands on the created activity", async ({ page }) => {
    await page.getByRole("button", { name: /upload file/i }).click();

    // Select the bundled sample file via the hidden input
    const fileInput = page.locator('input[type="file"][accept*=".gpx"]');
    await fileInput.setInputFiles(path.join(SAMPLES_DIR, "morning_run_5k.gpx"));

    // Preview box appears with the filename
    await expect(page.getByText("morning_run_5k.gpx").first()).toBeVisible();

    // Kick off the import
    await page.getByRole("button", { name: /import activity/i }).click();

    // Success message appears, then we get redirected to the activity detail page
    await expect(page.getByText(/successfully imported/i)).toBeVisible({ timeout: 10_000 });
    await page.waitForURL(/\/activities\/\d+/, { timeout: 10_000 });

    // Activity detail should show the title from the GPX metadata, distance, and the map
    await expect(page.getByText(/morning run/i)).toBeVisible();
    await expect(page.getByText(/distance/i).first()).toBeVisible();

    // Clean up: delete the imported activity so the DB doesn't leak across runs
    const match = page.url().match(/\/activities\/(\d+)/);
    if (match) {
      await page.request.delete(`${BACKEND}/activities/${match[1]}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
    }
  });

  test("rejects unsupported file extension with a client-side error", async ({ page }) => {
    await page.getByRole("button", { name: /upload file/i }).click();

    // Feed the hidden input a fake .txt file via Buffer (avoids creating a temp file on disk)
    const fileInput = page.locator('input[type="file"][accept*=".gpx"]');
    await fileInput.setInputFiles({
      name: "not-an-activity.txt",
      mimeType: "text/plain",
      buffer: Buffer.from("hello world"),
    });

    await expect(page.getByText(/unsupported file format/i)).toBeVisible();

    // The Import button should not have been enabled for a rejected file
    const importBtn = page.getByRole("button", { name: /import activity/i });
    await expect(importBtn).toBeDisabled();
  });

  test("switching back to manual mode hides the dropzone", async ({ page }) => {
    await page.getByRole("button", { name: /upload file/i }).click();
    await expect(page.getByText(/click to choose a file or drag it here/i)).toBeVisible();

    await page.getByRole("button", { name: /manual entry/i }).click();
    await expect(page.getByText(/click to choose a file or drag it here/i)).not.toBeVisible();

    // Manual form fields should be visible again
    await expect(page.locator("input[name='title']")).toBeVisible();
    await expect(page.locator("input[name='durationHours']")).toBeVisible();
  });
});
