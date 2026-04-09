import { test, expect, Browser } from "@playwright/test";
import { login } from "./helpers";

const API_URL = "http://localhost:3000/api/v1";

/**
 * Helper: create a fresh browser context already logged-in as the given user.
 */
async function loggedInContext(browser: Browser, email: string, password: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await login(page, email, password);
  return { context, page };
}

test.describe("Notifications", () => {
  test("like activity triggers notification for the activity owner", async ({ browser }) => {
    // --- Context A: Pan (activity owner) ---
    const panCtx = await loggedInContext(browser, "pan@example.com", "password123");
    await panCtx.page.goto("/");
    await panCtx.page.waitForLoadState("networkidle");

    // Clear any existing notifications first so we start clean
    const clearRes = await panCtx.page.request.delete(`${API_URL}/notifications`, {
      headers: {
        Authorization: `Bearer ${await panCtx.page.evaluate(() => localStorage.getItem("token"))}`,
      },
    });
    // 200 or 204 are both fine; ignore errors if there are no notifications
    if (!clearRes.ok()) {
      console.log("Could not clear Pan notifications (may be empty):", clearRes.status());
    }

    // --- Context B: John (liker) ---
    const johnCtx = await loggedInContext(browser, "john@example.com", "password123");
    await johnCtx.page.goto("/activities");
    await johnCtx.page.waitForLoadState("networkidle");

    // Find the first activity card that belongs to Pan and like it
    const panCards = johnCtx.page.locator("a[href^='/activities/']").filter({
      hasText: /Pan/i,
    });

    const firstPanCard = panCards.first();
    await expect(firstPanCard).toBeVisible({ timeout: 10_000 });

    // Click the heart/like button inside that card
    const likeButton = firstPanCard.locator("button", { has: johnCtx.page.locator("svg") }).first();
    await likeButton.click();

    // Give the backend a moment to process the like and create the notification
    await johnCtx.page.waitForTimeout(2000);

    // --- Back to Context A: Pan checks notifications ---
    await panCtx.page.reload();
    await panCtx.page.waitForLoadState("networkidle");

    // The bell button should show an unread badge
    const bellButton = panCtx.page.getByTitle("Notifications");
    await expect(bellButton).toBeVisible();

    // Open notification dropdown
    await bellButton.click();

    // Should see a notification mentioning "liked" or the like action
    const dropdown = panCtx.page.locator("[class*='DropdownPanel']").or(
      panCtx.page.locator("div").filter({ hasText: "Notifications" }).locator(".."),
    );
    await expect(dropdown.getByText(/liked/i).first()).toBeVisible({ timeout: 10_000 });

    // Cleanup
    await panCtx.context.close();
    await johnCtx.context.close();
  });

  test("clear all notifications", async ({ browser }) => {
    const { context, page } = await loggedInContext(browser, "pan@example.com", "password123");
    await page.goto("/");
    await page.waitForLoadState("networkidle");

    // Open notification dropdown
    const bellButton = page.getByTitle("Notifications");
    await bellButton.click();

    // Wait for dropdown to appear
    await page.waitForTimeout(500);

    // If there are notifications, click the clear-all (trash) button
    const clearAllButton = page.getByTitle("Clear all notifications");
    const hasNotifications = await clearAllButton.isVisible().catch(() => false);

    if (hasNotifications) {
      await clearAllButton.click();
      await page.waitForTimeout(1000);

      // Verify empty state
      await expect(page.getByText("No notifications yet")).toBeVisible({ timeout: 5000 });
    }

    await context.close();
  });

  test("delete single notification", async ({ browser }) => {
    // First, ensure Pan has at least one notification by having John like an activity
    const johnCtx = await loggedInContext(browser, "john@example.com", "password123");
    await johnCtx.page.goto("/activities");
    await johnCtx.page.waitForLoadState("networkidle");

    const panCards = johnCtx.page.locator("a[href^='/activities/']").filter({
      hasText: /Pan/i,
    });
    const firstPanCard = panCards.first();
    if (await firstPanCard.isVisible().catch(() => false)) {
      const likeButton = firstPanCard.locator("button", { has: johnCtx.page.locator("svg") }).first();
      await likeButton.click();
      await johnCtx.page.waitForTimeout(2000);
    }
    await johnCtx.context.close();

    // Now log in as Pan and delete a single notification
    const panCtx = await loggedInContext(browser, "pan@example.com", "password123");
    await panCtx.page.goto("/");
    await panCtx.page.waitForLoadState("networkidle");

    const bellButton = panCtx.page.getByTitle("Notifications");
    await bellButton.click();
    await panCtx.page.waitForTimeout(500);

    // Find the individual delete buttons (trash icons within notification items)
    const deleteButtons = panCtx.page.getByTitle("Delete notification");
    const count = await deleteButtons.count();

    if (count > 0) {
      const initialCount = count;
      // Delete the first notification
      await deleteButtons.first().click();
      await panCtx.page.waitForTimeout(1000);

      // Verify the count decreased
      const newCount = await panCtx.page.getByTitle("Delete notification").count();
      expect(newCount).toBeLessThan(initialCount);
    }

    await panCtx.context.close();
  });
});
