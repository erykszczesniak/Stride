import { test, expect } from "@playwright/test";
import { login, loginViaUI } from "./helpers";

test.describe("Authentication", () => {
  test("login with valid credentials and redirect to dashboard", async ({ page }) => {
    await loginViaUI(page, "john@example.com", "password123");

    // Should redirect to the dashboard (root path)
    await page.waitForURL("/", { timeout: 10_000 });
    await expect(page).toHaveURL("/");

    // Navbar should be visible with the STRIDE brand
    await expect(page.getByText("STRIDE")).toBeVisible();

    // Dashboard nav link should be present
    await expect(page.getByRole("link", { name: "Dashboard" })).toBeVisible();
  });

  test("show error on invalid credentials", async ({ page }) => {
    await page.goto("/login");
    await page.getByPlaceholder("Enter your email").fill("wrong@example.com");
    await page.getByPlaceholder("Enter your password").fill("wrongpassword");
    await page.getByRole("button", { name: "Sign In" }).click();

    // An error message should appear
    await expect(page.getByRole("alert")).toBeVisible({ timeout: 10_000 });
    await expect(page.getByRole("alert")).toContainText(/invalid|error|incorrect/i);

    // Should still be on the login page
    await expect(page).toHaveURL("/login");
  });

  test("logout flow", async ({ page }) => {
    // Login via API for speed
    await login(page, "john@example.com", "password123");
    await page.goto("/");
    await page.waitForLoadState("networkidle");

    // Open user dropdown by clicking the avatar
    const avatar = page.locator("nav").getByText(/^[A-Z]{1,2}$/).first();
    await avatar.click();

    // Click Log Out
    await page.getByRole("button", { name: "Log Out" }).click();

    // Should redirect to login
    await page.waitForURL("/login", { timeout: 10_000 });
    await expect(page).toHaveURL("/login");
  });
});
