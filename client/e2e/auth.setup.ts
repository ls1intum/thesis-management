import { test as setup, expect } from '@playwright/test'

const DISABLE_PASSKEY_PROMPT_STORAGE_KEY = 'passkey_prompt_disabled'

const TEST_USERS = [
  { name: 'student', username: 'student', password: 'student' },
  { name: 'student2', username: 'student2', password: 'student2' },
  { name: 'student3', username: 'student3', password: 'student3' },
  { name: 'student4', username: 'student4', password: 'student4' },
  { name: 'student5', username: 'student5', password: 'student5' },
  { name: 'passkey_user', username: 'passkey_user', password: 'passkey_user' },
  { name: 'supervisor', username: 'supervisor', password: 'supervisor' },
  { name: 'supervisor2', username: 'supervisor2', password: 'supervisor2' },
  { name: 'examiner', username: 'examiner', password: 'examiner' },
  { name: 'examiner2', username: 'examiner2', password: 'examiner2' },
  { name: 'admin', username: 'admin', password: 'admin' },
  { name: 'delete_old_thesis', username: 'delete_old_thesis', password: 'delete_old_thesis' },
  {
    name: 'delete_recent_thesis',
    username: 'delete_recent_thesis',
    password: 'delete_recent_thesis',
  },
  { name: 'delete_rejected_app', username: 'delete_rejected_app', password: 'delete_rejected_app' },
] as const

for (const user of TEST_USERS) {
  setup(`authenticate as ${user.name}`, async ({ page }) => {
    // Start from a public route and use the regular header login button to log in
    await page.goto('/')

    await expect(page).toHaveURL(/\/$/)
    await page.locator('header').getByRole('button', { name: 'Login' }).click()

    // Wait for Keycloak login page to load
    await expect(page.locator('#kc-login')).toBeVisible({ timeout: 30_000 })

    // Fill in credentials on the Keycloak login form
    await page.locator('#username').fill(user.username)
    await page.locator('#password').fill(user.password)
    await page.locator('#kc-login').click()

    // Wait for redirect back to the app and the dashboard to load
    await expect(page).toHaveURL(/localhost:\d+/, { timeout: 30_000 })

    // Wait for the app to fully initialize with the auth tokens
    await page.waitForFunction(
      () => {
        try {
          const tokens = localStorage.getItem('authentication_tokens')
          if (!tokens) return false
          const parsed = JSON.parse(tokens)
          return !!parsed.access_token && !!parsed.refresh_token
        } catch {
          return false
        }
      },
      { timeout: 15_000 },
    )

    await page.evaluate((storageKey) => {
      localStorage.setItem(storageKey, 'true')
    }, DISABLE_PASSKEY_PROMPT_STORAGE_KEY)

    // Save the authenticated state (localStorage + cookies including Keycloak session)
    await page.context().storageState({ path: `e2e/.auth/${user.name}.json` })
  })
}
