import { Page, expect, request } from "@playwright/test";

const BACKEND_URL = "http://localhost:8080/api/v1";

// Cache tokens to avoid rate limiting on login endpoint
const tokenCache: Record<string, string> = {};

async function fetchToken(email: string, password: string): Promise<string> {
  if (tokenCache[email]) return tokenCache[email];

  const ctx = await request.newContext();
  const response = await ctx.post(`${BACKEND_URL}/auth/login`, {
    data: { email, password },
  });

  if (response.status() === 429) {
    await new Promise((r) => setTimeout(r, 5000));
    const retry = await ctx.post(`${BACKEND_URL}/auth/login`, {
      data: { email, password },
    });
    expect(retry.ok()).toBeTruthy();
    const body = await retry.json();
    tokenCache[email] = body.accessToken;
    await ctx.dispose();
    return body.accessToken;
  }

  expect(response.ok()).toBeTruthy();
  const body = await response.json();
  tokenCache[email] = body.accessToken;
  await ctx.dispose();
  return body.accessToken;
}

/**
 * Login via the backend API and inject token into localStorage.
 * Caches tokens to avoid hitting rate limits.
 */
export async function login(page: Page, email: string, password: string) {
  const token = await fetchToken(email, password);

  await page.addInitScript((t: string) => {
    localStorage.setItem("token", t);
  }, token);

  return token;
}

/**
 * Login via the UI form.
 */
export async function loginViaUI(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByPlaceholder("Enter your email").fill(email);
  await page.getByPlaceholder("Enter your password").fill(password);
  await page.getByRole("button", { name: "Sign In" }).click();
}

/**
 * Wait for dashboard to load.
 */
export async function waitForDashboard(page: Page) {
  await page.waitForSelector("text=Welcome back", { timeout: 10_000 });
}
