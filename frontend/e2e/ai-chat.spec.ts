import { test, expect } from "@playwright/test";
import { login } from "./helpers";

/**
 * Drives the AI activity-analysis chat end-to-end. The Claude-backed endpoints are
 * intercepted so the test is deterministic and needs no ANTHROPIC_API_KEY — it verifies
 * the UI wiring (load conversation, send message, render reply, clear), not the model.
 */
test.describe("AI activity chat", () => {
  test.beforeEach(async ({ page }) => {
    await login(page, "john@example.com", "password123");

    await page.route("**/api/v1/ai/activities/*/chat", async (route) => {
      const method = route.request().method();
      if (method === "GET") {
        await route.fulfill({ json: { activityId: 1, messages: [] } });
      } else if (method === "POST") {
        const body = JSON.parse(route.request().postData() || "{}");
        await route.fulfill({
          json: {
            activityId: 1,
            messages: [
              { role: "USER", content: body.message },
              {
                role: "ASSISTANT",
                content: "This was a strong aerobic effort with steady power.",
              },
            ],
          },
        });
      } else if (method === "DELETE") {
        await route.fulfill({ status: 204, body: "" });
      } else {
        await route.continue();
      }
    });
  });

  async function openFirstActivity(page) {
    await page.goto("/activities");
    const card = page
      .locator("a[href^='/activities/']:not([href='/activities/new'])")
      .first();
    await expect(card).toBeVisible({ timeout: 10_000 });
    await card.click({ position: { x: 150, y: 20 } });
    await page.waitForURL(/\/activities\/\d+/, { timeout: 10_000 });
  }

  test("sends a message and renders the AI reply", async ({ page }) => {
    await openFirstActivity(page);

    const chat = page.getByTestId("activity-ai-chat");
    await expect(chat).toBeVisible({ timeout: 10_000 });

    await chat.locator("input").fill("How was my pacing?");
    await chat.locator("button[type='submit']").click();

    await expect(chat.getByTestId("chat-bubble-USER")).toHaveText("How was my pacing?");
    await expect(chat.getByTestId("chat-bubble-ASSISTANT")).toContainText(
      "aerobic effort"
    );
  });

  test("clears the conversation", async ({ page }) => {
    await openFirstActivity(page);

    const chat = page.getByTestId("activity-ai-chat");
    await expect(chat).toBeVisible({ timeout: 10_000 });

    await chat.locator("input").fill("Analyze this activity");
    await chat.locator("button[type='submit']").click();
    await expect(chat.getByTestId("chat-bubble-ASSISTANT")).toBeVisible();

    await chat.getByTestId("ai-chat-clear").click();
    await expect(chat.getByTestId("chat-bubble-ASSISTANT")).toHaveCount(0);
    await expect(chat.getByTestId("chat-bubble-USER")).toHaveCount(0);
  });
});
