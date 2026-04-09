import { test, expect } from "@playwright/test";
import { login } from "./helpers";

test.describe("Activities", () => {
  test.beforeEach(async ({ page }) => {
    await login(page, "john@example.com", "password123");
    await page.goto("/activities");
    await expect(page.locator("a[href^='/activities/']:not([href='/activities/new'])").first()).toBeVisible({ timeout: 10_000 });

    // Dismiss notification prompt if it appears
    const notNow = page.getByText("Not now");
    if (await notNow.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await notNow.click();
    }
  });

  test("create a new activity", async ({ page }) => {
    await page.goto("/activities/new");

    await expect(page.locator("h1, h2").filter({ hasText: "Record Activity" })).toBeVisible({
      timeout: 10_000,
    });

    await page.locator("input[name='title']").fill("");
    await page.locator("input[name='title']").fill("E2E Test Ride");
    await page.locator("textarea[name='description']").fill("Automated test activity");
    await page.locator("input[name='distance']").fill("25.5");
    await page.locator("input[name='duration']").fill("60");
    await page.locator("input[name='elevation']").fill("150");
    await page.locator("input[name='calories']").fill("600");

    await page.getByRole("button", { name: "Save Activity" }).click();
    await page.waitForURL("/activities", { timeout: 10_000 });
  });

  test("view activity details", async ({ page }) => {
    // Click on the activity title text to navigate
    const card = page.locator("a[href^='/activities/']:not([href='/activities/new'])").first();
    await card.click({ position: { x: 150, y: 20 } });

    await page.waitForURL(/\/activities\/\d+/, { timeout: 10_000 });
    await expect(page.getByText(/distance/i).first()).toBeVisible({ timeout: 5_000 });
  });

  test("like and unlike an activity", async ({ page }) => {
    const card = page.locator("a[href^='/activities/']:not([href='/activities/new'])").first();
    await card.click({ position: { x: 150, y: 20 } });
    await page.waitForURL(/\/activities\/\d+/, { timeout: 10_000 });

    const likeButton = page.locator("button").filter({ hasText: /like/i }).first();
    await expect(likeButton).toBeVisible({ timeout: 5_000 });

    const initialText = await likeButton.textContent();
    const initialCount = parseInt(initialText?.match(/\d+/)?.[0] || "0", 10);

    await likeButton.click();
    await page.waitForTimeout(1000);

    const afterText = await likeButton.textContent();
    const afterCount = parseInt(afterText?.match(/\d+/)?.[0] || "0", 10);
    expect(Math.abs(afterCount - initialCount)).toBe(1);

    await likeButton.click();
    await page.waitForTimeout(1000);

    const finalText = await likeButton.textContent();
    const finalCount = parseInt(finalText?.match(/\d+/)?.[0] || "0", 10);
    expect(finalCount).toBe(initialCount);
  });
});
