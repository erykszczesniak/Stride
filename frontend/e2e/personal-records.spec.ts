import { test, expect } from "@playwright/test";
import { login } from "./helpers";

const BACKEND = "http://localhost:8080/api/v1";

test.describe("Personal Records on profile", () => {
  let token: string;

  test.beforeEach(async ({ page }) => {
    token = await login(page, "john@example.com", "password123");
    await page.goto("/profile");
    await expect(page.getByText(/John Doe/i).first()).toBeVisible({ timeout: 10_000 });
  });

  test("Personal Records tab is reachable on own profile", async ({ page }) => {
    const prTab = page.getByRole("button", { name: /personal records/i });
    await expect(prTab).toBeVisible();
    await prTab.click();

    await expect(page.getByRole("heading", { name: /personal records/i })).toBeVisible();
    await expect(
      page.getByText(/your fastest times for standard distances/i)
    ).toBeVisible();
  });

  test("shows Recalculate button only on own profile when PRs exist", async ({ page }) => {
    await page.getByRole("button", { name: /personal records/i }).click();

    // Wait for the loading state to finish
    await expect(page.getByText(/loading personal records/i)).not.toBeVisible({ timeout: 5_000 });

    // On own profile we either have the empty-state Recalculate button OR the header one —
    // at least one should exist.
    const recalcButtons = page.getByRole("button", { name: /recalculate/i });
    await expect(recalcButtons.first()).toBeVisible({ timeout: 5_000 });
  });

  test("Recalculate triggers an API call and refreshes the list", async ({ page }) => {
    await page.getByRole("button", { name: /personal records/i }).click();
    await expect(page.getByText(/loading personal records/i)).not.toBeVisible({ timeout: 5_000 });

    // Wait for a Recalculate button to show (either header or empty-state variant)
    const recalcBtn = page.getByRole("button", { name: /recalculate/i }).first();
    await expect(recalcBtn).toBeVisible({ timeout: 5_000 });

    // Watch for the recalculate request to fire
    const recalcRequest = page.waitForResponse(
      (res) =>
        res.url().includes("/personal-records/recalculate") && res.request().method() === "POST",
      { timeout: 10_000 }
    );
    await recalcBtn.click();
    const response = await recalcRequest;

    // Should succeed — this is the exact endpoint that was broken before PR #78
    expect(response.status()).toBe(200);

    // After recalculate, the get-my-prs endpoint should also fire to refresh the view
    await page.waitForResponse(
      (res) =>
        res.url().endsWith("/personal-records") && res.request().method() === "GET",
      { timeout: 10_000 }
    );
  });

  test("public profile also has a Personal Records tab with no Recalculate button", async ({ page }) => {
    // Navigate to another user's profile (Pan, id=1)
    await page.goto("/profile/1");
    await expect(page.getByText(/Pan Szczesniak/i).first()).toBeVisible({ timeout: 10_000 });

    const prTab = page.getByRole("button", { name: /personal records/i });
    await expect(prTab).toBeVisible();
    await prTab.click();

    await expect(page.getByRole("heading", { name: /personal records/i })).toBeVisible();

    // Viewing someone else's profile — the header Recalculate button must NOT appear
    // (empty-state one may still show with the "No personal records yet" text for other user,
    // but the code path disables that for non-own profiles too, so it should be absent)
    await expect(page.getByRole("button", { name: /recalculate/i })).not.toBeVisible();
  });

  test("displays PR rows grouped by sport when records exist", async ({ page }) => {
    // Seed at least one PR by creating a qualifying activity via the API
    const createRes = await page.request.post(`${BACKEND}/activities`, {
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      data: {
        title: "E2E PR seed run",
        description: "Seeded by personal-records.spec.ts",
        activityType: "RUN",
        distance: 5.0,
        duration: 1500, // 25:00 → 5:00 /km
        startTime: new Date().toISOString(),
        isPrivate: false,
      },
    });
    expect(createRes.ok()).toBeTruthy();
    const created = await createRes.json();
    const activityId = created.id;

    try {
      await page.reload();
      await page.getByRole("button", { name: /personal records/i }).click();
      await expect(page.getByText(/loading personal records/i)).not.toBeVisible({ timeout: 5_000 });

      // The "Running" sport section should now be visible
      await expect(page.getByText(/^running$/i)).toBeVisible({ timeout: 5_000 });

      // And there should be a 5 km row with a best time (hh:mm or mm:ss format)
      await expect(page.getByText(/5 km|5\.0 km/i).first()).toBeVisible();
    } finally {
      // Clean up: delete the seed activity (cascades to the PR via FK)
      await page.request.delete(`${BACKEND}/activities/${activityId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
    }
  });
});
