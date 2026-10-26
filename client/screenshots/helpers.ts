import type { Locator, Page } from '@playwright/test'
import { expect } from '@playwright/test'
import { navigateTo } from '../e2e/helpers'

/**
 * Screenshot-oriented helpers. These build on top of `e2e/helpers.ts` but are
 * intentionally more forgiving — a slow selector should not fail a screenshot
 * job; it should log and move on so the rest of the batch still runs.
 */

/** Wait for the app skeleton (Mantine Loader) to disappear. Same as e2e. */
export async function waitForAppReady(page: Page, timeout = 30_000): Promise<void> {
  await page
    .locator('.mantine-Loader-root')
    .waitFor({ state: 'hidden', timeout })
    .catch(() => {
      // Loader may never appear on fast public pages.
    })
}

/** Go to a route and wait for it to settle. */
export async function goto(page: Page, path: string): Promise<void> {
  await navigateTo(page, path)
  // A short "settle" delay so lazy-loaded avatars & charts finish rendering.
  await page.waitForTimeout(750)
}

/**
 * Best-effort click on the first element matching `selector`. Silently swallows
 * timeouts — scenes should not fail just because an optional overlay is
 * missing (e.g. a passkey prompt that never appeared).
 */
export async function tryClick(locator: Locator, timeout = 2_000): Promise<boolean> {
  const visible = await locator.isVisible({ timeout }).catch(() => false)
  if (!visible) return false
  await locator.click().catch(() => {})
  return true
}

/**
 * Dismiss the passkey registration prompt that shows on first login. It's
 * disabled in e2e auth setup, but we don't rely on that here — capture jobs
 * may be run against a manually-seeded environment.
 */
export async function dismissPasskeyPrompt(page: Page): Promise<void> {
  await tryClick(page.getByRole('button', { name: /(skip|later|no thanks)/i }))
}

/**
 * Wait for network to be idle before taking a screenshot. Prevents shots with
 * half-loaded avatars or empty tables. Times out silently.
 */
export async function settle(page: Page, timeoutMs = 3_000): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: timeoutMs }).catch(() => {})
  await page.waitForTimeout(300)
}

/**
 * Highlight a locator by drawing a coloured outline around it. Useful when a
 * screenshot is meant to draw attention to a single button/section. Applied
 * via a temporary CSS style so it does not persist across scenes.
 */
export async function highlight(locator: Locator, color = '#e67e22'): Promise<void> {
  await locator.evaluate((el, c) => {
    ;(el as HTMLElement).style.outline = `3px solid ${c}`
    ;(el as HTMLElement).style.outlineOffset = '2px'
    ;(el as HTMLElement).style.borderRadius = '6px'
  }, color)
}

/**
 * Scroll a locator into view and wait for the scroll to settle. `scrollIntoView`
 * alone can leave the element flush against the viewport top; the small delay
 * lets the sticky header re-render before the screenshot.
 */
export async function scrollTo(locator: Locator): Promise<void> {
  await locator.scrollIntoViewIfNeeded().catch(() => {})
  await locator.page().waitForTimeout(250)
}

/** Assert that a locator is visible with the shared screenshot timeout. */
export async function expectVisible(locator: Locator, timeout = 15_000): Promise<void> {
  await expect(locator).toBeVisible({ timeout })
}
