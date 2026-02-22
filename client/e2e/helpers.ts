import { Page, expect } from '@playwright/test'

/**
 * Wait for the app to fully load after navigation.
 * The Mantine Loader (spinning circle) must disappear before we consider the page ready.
 */
export async function waitForPageLoad(page: Page, timeout = 30_000) {
  // Wait for the Mantine loader/spinner to appear and then disappear
  // The loader uses a CSS animation (mantine-Loader-root class)
  await page
    .locator('.mantine-Loader-root')
    .waitFor({ state: 'hidden', timeout })
    .catch(() => {
      // Loader may never appear if the page loads instantly
    })
}

/**
 * Navigate to a page and wait for it to fully load.
 */
export async function navigateTo(page: Page, path: string) {
  await page.goto(path, { waitUntil: 'domcontentloaded' })
  await waitForPageLoad(page)
}

/**
 * Use a specific auth state file for a test.
 */
export function authStatePath(role: 'student' | 'advisor' | 'supervisor' | 'admin'): string {
  return `e2e/.auth/${role}.json`
}
