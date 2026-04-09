import { Page, expect } from "@playwright/test";

const API_URL = "http://localhost:3000/api/v1";

/**
 * Login via the API and inject the token into localStorage so the app
 * recognises the user without going through the login form.
 */
export async function login(page: Page, email: string, password: string) {
  const response = await page.request.post(`${API_URL}/auth/login`, {
    data: { email, password },
  });

  expect(response.ok()).toBeTruthy();

  const body = await response.json();

  // Inject tokens into localStorage before navigating so the app picks them up
  await page.addInitScript(
    ({ accessToken, refreshToken }: { accessToken: string; refreshToken: string }) => {
      localStorage.setItem("token", accessToken);
      localStorage.setItem("refreshToken", refreshToken);
    },
    { accessToken: body.accessToken, refreshToken: body.refreshToken },
  );
}

/**
 * Login via the UI form (useful for testing the actual login flow).
 */
export async function loginViaUI(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByPlaceholder("Enter your email").fill(email);
  await page.getByPlaceholder("Enter your password").fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
}
