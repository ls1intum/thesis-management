import { test as setup, expect, type Page } from '@playwright/test'

const DISABLE_PASSKEY_PROMPT_STORAGE_KEY = 'passkey_prompt_disabled'

const waitForAppBootstrap = async (page: Page) => {
  await page
    .locator('.mantine-Loader-root')
    .waitFor({ state: 'hidden', timeout: 30_000 })
    .catch(() => {
      // The loader may never appear on fast public pages.
    })
}

const isKeycloakPasswordLoginVisible = async (page: Page, timeout = 1_000) =>
  (await page
    .locator('#username')
    .isVisible({ timeout })
    .catch(() => false)) ||
  (await page
    .getByRole('textbox', { name: /Username or email/i })
    .isVisible({ timeout })
    .catch(() => false)) ||
  (await page
    .getByRole('heading', { name: /Sign in to your account/i })
    .isVisible({ timeout })
    .catch(() => false))

const getLoginEntryPoint = async (page: Page) => {
  if (await isKeycloakPasswordLoginVisible(page, 100)) {
    return 'keycloak'
  }

  if (
    await page
      .locator('header')
      .getByRole('button', { name: 'Login' })
      .isVisible({ timeout: 100 })
      .catch(() => false)
  ) {
    return 'app'
  }

  return 'none'
}

const openPasswordLogin = async (page: Page) => {
  const headerLoginButton = page.locator('header').getByRole('button', { name: 'Login' })

  for (let attempt = 0; attempt < 3; attempt += 1) {
    const entryPoint = await expect
      .poll(() => getLoginEntryPoint(page), { timeout: 30_000 })
      .not.toBe('none')
      .then(() => getLoginEntryPoint(page))

    if (entryPoint === 'keycloak') {
      return
    }

    await headerLoginButton.click()

    if (await isKeycloakPasswordLoginVisible(page, 10_000)) {
      return
    }

    await waitForAppBootstrap(page)
  }

  await expect.poll(() => isKeycloakPasswordLoginVisible(page), { timeout: 30_000 }).toBe(true)
}

const fillKeycloakLoginForm = async (page: Page, username: string, password: string) => {
  const usernameInput = page.locator('#username')
  const passwordInput = page.locator('#password')

  if (await usernameInput.isVisible({ timeout: 1_000 }).catch(() => false)) {
    await usernameInput.fill(username)
  } else {
    await page.getByRole('textbox', { name: /Username or email/i }).fill(username)
  }

  if (await passwordInput.isVisible({ timeout: 1_000 }).catch(() => false)) {
    await passwordInput.fill(password)
  } else {
    await page.getByLabel('Password').fill(password)
  }
}

const submitKeycloakLogin = async (page: Page) => {
  const legacySubmit = page.locator('#kc-login')

  if (await legacySubmit.isVisible({ timeout: 1_000 }).catch(() => false)) {
    await legacySubmit.click()
    return
  }

  await page.getByRole('button', { name: 'Sign In' }).click()
}

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
    await waitForAppBootstrap(page)

    await openPasswordLogin(page)

    // Fill in credentials on the Keycloak login form
    await fillKeycloakLoginForm(page, user.username, user.password)
    await submitKeycloakLogin(page)

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
