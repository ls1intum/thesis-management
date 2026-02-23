import { test as setup, expect } from '@playwright/test'

const TEST_USERS = [
  { name: 'student', username: 'student', password: 'student' },
  { name: 'student2', username: 'student2', password: 'student2' },
  { name: 'student3', username: 'student3', password: 'student3' },
  { name: 'advisor', username: 'advisor', password: 'advisor' },
  { name: 'advisor2', username: 'advisor2', password: 'advisor2' },
  { name: 'supervisor', username: 'supervisor', password: 'supervisor' },
  { name: 'supervisor2', username: 'supervisor2', password: 'supervisor2' },
  { name: 'admin', username: 'admin', password: 'admin' },
  { name: 'delete_old_thesis', username: 'delete_old_thesis', password: 'delete_old_thesis' },
  { name: 'delete_recent_thesis', username: 'delete_recent_thesis', password: 'delete_recent_thesis' },
  { name: 'delete_rejected_app', username: 'delete_rejected_app', password: 'delete_rejected_app' },
] as const

for (const user of TEST_USERS) {
  setup(`authenticate as ${user.name}`, async ({ page }) => {
    // Navigate to a protected route to trigger Keycloak login redirect
    await page.goto('/dashboard')

    // Wait for Keycloak login page to load
    await expect(page.locator('#kc-login')).toBeVisible({ timeout: 30_000 })

    // Fill in credentials on the Keycloak login form
    await page.locator('#username').fill(user.username)
    await page.locator('#password').fill(user.password)
    await page.locator('#kc-login').click()

    // Wait for redirect back to the app and the dashboard to load
    await expect(page).toHaveURL(/localhost:3000/, { timeout: 30_000 })

    // Wait for the app to fully initialize with the auth tokens
    await page.waitForFunction(() => {
      try {
        const tokens = localStorage.getItem('authentication_tokens')
        if (!tokens) return false
        const parsed = JSON.parse(tokens)
        return !!parsed.access_token && !!parsed.refresh_token
      } catch {
        return false
      }
    }, { timeout: 15_000 })

    // Save the authenticated state (localStorage + cookies including Keycloak session)
    await page.context().storageState({ path: `e2e/.auth/${user.name}.json` })
  })
}
