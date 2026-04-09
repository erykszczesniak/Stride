import { test, expect } from "@playwright/test";
import { login } from "./helpers";

test.describe("Activities", () => {
  test.beforeEach(async ({ page }) => {
    await login(page, "john@example.com", "password123");
  });

  test("create a new activity", async ({ page }) => {
    await page.goto("/activities/new");
    await page.waitForLoadState("networkidle");

    // Page heading
    await expect(page.getByText("Record Activity")).toBeVisible();

    // Select activity type -- Run is selected by default, pick Ride instead
    await page.getByText("Ride").click();

    // Fill in required fields
    await page.locator("input[name='title']").fill("E2E Test Ride");
    await page.locator("textarea[name='description']").fill("Automated test activity");
    await page.locator("input[name='distance']").fill("25.5");
    await page.locator("input[name='duration']").fill("60");

    // Optional fields
    await page.locator("input[name='elevation']").fill("150");
    await page.locator("input[name='calories']").fill("600");

    // Save
    await page.getByRole("button", { name: "Save Activity" }).click();

    // Should navigate to activities list
    await page.waitForURL("/activities", { timeout: 10_000 });
    await expect(page).toHaveURL("/activities");

    // The new activity should appear in the list
    await expect(page.getByText("E2E Test Ride")).toBeVisible({ timeout: 10_000 });
  });

  test("view activity details", async ({ page }) => {
    await page.goto("/activities");
    await page.waitForLoadState("networkidle");

    // Click on the first activity card to view details
    const firstCard = page.locator("a[href^='/activities/']").first();
    await expect(firstCard).toBeVisible({ timeout: 10_000 });
    const activityTitle = await firstCard.locator("h3, h2, [class*='Title']").first().textContent();

    await firstCard.click();

    // Should navigate to activity details page
    await page.waitForURL(/\/activities\/\d+/, { timeout: 10_000 });

    // Should show activity details
    await expect(page.getByText("Back to Activities")).toBeVisible();
    await expect(page.getByText("Distance (km)")).toBeVisible();
    await expect(page.getByText("Duration")).toBeVisible();

    // The activity title should be displayed
    if (activityTitle) {
      await expect(page.getByText(activityTitle)).toBeVisible();
    }
  });

  test("like and unlike an activity", async ({ page }) => {
    await page.goto("/activities");
    await page.waitForLoadState("networkidle");

    // Navigate to the first activity's detail page
    const firstCard = page.locator("a[href^='/activities/']").first();
    await expect(firstCard).toBeVisible({ timeout: 10_000 });
    await firstCard.click();
    await page.waitForURL(/\/activities\/\d+/, { timeout: 10_000 });

    // Find the like button (contains FiHeart icon and "Likes" text)
    const likeButton = page.locator("button").filter({ hasText: /Likes/i }).first();
    await expect(likeButton).toBeVisible();

    // Get the initial like count
    const initialText = await likeButton.textContent();
    const initialCount = parseInt(initialText?.match(/\d+/)?.[0] || "0", 10);

    // Click to like
    await likeButton.click();
    await page.waitForTimeout(1000);

    // Verify the count changed
    const afterLikeText = await likeButton.textContent();
    const afterLikeCount = parseInt(afterLikeText?.match(/\d+/)?.[0] || "0", 10);

    // The count should have changed by 1 (either +1 if was unliked, or -1 if was liked)
    expect(Math.abs(afterLikeCount - initialCount)).toBe(1);

    // Click again to toggle back
    await likeButton.click();
    await page.waitForTimeout(1000);

    const afterUnlikeText = await likeButton.textContent();
    const afterUnlikeCount = parseInt(afterUnlikeText?.match(/\d+/)?.[0] || "0", 10);

    // Should be back to the initial count
    expect(afterUnlikeCount).toBe(initialCount);
  });
});
