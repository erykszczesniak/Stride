import { test, expect, Browser } from "@playwright/test";
import { login } from "./helpers";

const BACKEND = "http://localhost:8080/api/v1";

async function loggedInPage(browser: Browser, email: string, password: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  const token = await login(page, email, password);

  return { context, page, token };
}

test.describe.configure({ mode: "serial" });

test.describe("Notifications", () => {
  test("like activity triggers notification for the activity owner", async ({ browser }) => {
    // Setup: Pan's context
    const pan = await loggedInPage(browser, "pan@example.com", "password123");

    // Clear Pan's notifications via API
    await pan.page.request.delete(`${BACKEND}/notifications`, {
      headers: { Authorization: `Bearer ${pan.token}` },
    });

    // John likes Pan's activity via API (fast, no UI needed)
    const john = await loggedInPage(browser, "john@example.com", "password123");
    // Get Pan's activities
    const activitiesRes = await john.page.request.get(`${BACKEND}/activities/public?page=0&size=50`, {
      headers: { Authorization: `Bearer ${john.token}` },
    });
    const activities = await activitiesRes.json();
    const panActivity = activities.content?.find((a: any) => a.userName?.includes("Pan")) || activities[0];

    if (panActivity) {
      // Unlike first to ensure the like is fresh
      await john.page.request.delete(`${BACKEND}/activities/${panActivity.id}/like`, {
        headers: { Authorization: `Bearer ${john.token}` },
      });
      await john.page.request.post(`${BACKEND}/activities/${panActivity.id}/like`, {
        headers: { Authorization: `Bearer ${john.token}` },
      });
    }
    await john.context.close();

    // Pan checks notifications
    await pan.page.goto("http://localhost:3000/");
    await pan.page.waitForSelector("text=Welcome back", { timeout: 10_000 });

    // Dismiss notification prompt if visible
    const notNow = pan.page.getByText("Not now");
    if (await notNow.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await notNow.click();
    }

    const bellButton = pan.page.getByTitle("Notifications");
    await expect(bellButton).toBeVisible();
    await bellButton.click();

    await expect(pan.page.getByText(/liked/i).first()).toBeVisible({ timeout: 10_000 });

    await pan.context.close();
  });

  test("clear all notifications", async ({ browser }) => {
    const { context, page, token } = await loggedInPage(browser, "pan@example.com", "password123");

    // Ensure at least one notification exists
    const john = await loggedInPage(browser, "john@example.com", "password123");
    const activitiesRes = await john.page.request.get(`${BACKEND}/activities/public?page=0&size=10`, {
      headers: { Authorization: `Bearer ${john.token}` },
    });
    const activities = await activitiesRes.json();
    const panActivity = activities.content?.find((a: any) => a.userName?.includes("Pan")) || activities[0];
    if (panActivity) {
      // Unlike first in case already liked, then like
      await john.page.request.delete(`${BACKEND}/activities/${panActivity.id}/like`, {
        headers: { Authorization: `Bearer ${john.token}` },
      });
      await john.page.request.post(`${BACKEND}/activities/${panActivity.id}/like`, {
        headers: { Authorization: `Bearer ${john.token}` },
      });
    }
    await john.context.close();

    await page.goto("http://localhost:3000/");
    await page.waitForSelector("text=Welcome back", { timeout: 10_000 });

    const notNow = page.getByText("Not now");
    if (await notNow.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await notNow.click();
    }

    const bellButton = page.getByTitle("Notifications");
    await bellButton.click();
    await page.waitForTimeout(500);

    const clearAll = page.getByTitle("Clear all notifications");
    if (await clearAll.isVisible().catch(() => false)) {
      await clearAll.click();
      await page.waitForTimeout(1000);
      await expect(page.getByText("No notifications yet")).toBeVisible({ timeout: 5_000 });
    }

    await context.close();
  });

  test("delete single notification", async ({ browser }) => {
    const pan = await loggedInPage(browser, "pan@example.com", "password123");

    await pan.page.goto("http://localhost:3000/");
    await pan.page.waitForSelector("text=Welcome back", { timeout: 10_000 });

    const notNow = pan.page.getByText("Not now");
    if (await notNow.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await notNow.click();
    }

    const bellButton = pan.page.getByTitle("Notifications");
    await bellButton.click();
    await pan.page.waitForTimeout(500);

    const deleteButtons = pan.page.getByTitle("Delete notification");
    const count = await deleteButtons.count();

    if (count > 0) {
      // Hover parent to reveal delete button
      const firstItem = deleteButtons.first().locator("..");
      await firstItem.hover();
      await deleteButtons.first().click();
      await pan.page.waitForTimeout(1000);

      const newCount = await pan.page.getByTitle("Delete notification").count();
      expect(newCount).toBeLessThan(count);
    }

    await pan.context.close();
  });
});
