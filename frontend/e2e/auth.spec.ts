import { test, expect } from "@playwright/test";
import { login, loginViaUI, waitForDashboard } from "./helpers";

test.describe("Authentication", () => {
  test.beforeEach(async ({ page }) => {
    // Ensure clean state - no leftover tokens
    await page.goto("/login");
    await page.evaluate(() => localStorage.clear());
  });

  test("login with valid credentials and redirect to dashboard", async ({ page }) => {
    await loginViaUI(page, "john@example.com", "password123");

    await page.waitForURL("/", { timeout: 10_000 });
    await expect(page).toHaveURL("/");
    await expect(page.getByText("STRIDE")).toBeVisible();
  });

  test("show error on invalid credentials", async ({ page }) => {
    await loginViaUI(page, "wrong@example.com", "wrongpassword");

    await expect(page.getByText(/invalid|error|incorrect/i)).toBeVisible({ timeout: 10_000 });
    await expect(page).toHaveURL("/login");
  });

  test("logout flow", async ({ page }) => {
    await login(page, "john@example.com", "password123");
    await page.goto("/");
    await waitForDashboard(page);

    // Dismiss notification prompt if visible
    const notNow = page.getByText("Not now");
    if (await notNow.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await notNow.click();
    }

    // Sidebar "Sign Out" button
    const signOut = page.getByRole("button", { name: "Sign Out" }).first();
    await signOut.scrollIntoViewIfNeeded();
    await signOut.click();

    await page.waitForURL("/login", { timeout: 10_000 });
    await expect(page).toHaveURL("/login");
  });
});
