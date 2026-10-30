import { test, expect } from '@playwright/test'
import {
  KEYCLOAK_ORIGIN,
  SERVER_ORIGIN,
  authStatePath,
  getSessionCookie,
  loginWithApi,
  logoutWithApi,
  navigateTo,
} from '../helpers'

// Users who already hold an application session keep working while Keycloak is down. Every test starts
// from a session of its own (created through the API) so that ending or rotating it cannot affect other tests.
test.describe('Keycloak outage', () => {
  test('a logged-in user keeps working while Keycloak is unreachable', async ({ browser }) => {
    const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })
    await loginWithApi(context, 'student5')
    const page = await context.newPage()

    const keycloakRequests: string[] = []
    await context.route(`${KEYCLOAK_ORIGIN}/**`, async (route) => {
      keycloakRequests.push(route.request().url())
      await route.abort('connectionrefused')
    })

    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('header').getByText('Login')).toBeHidden()

    // moving around and reloading needs neither a login nor a token refresh
    await navigateTo(page, '/settings/my-information')
    await expect(page.getByRole('tab', { name: 'My Information' })).toBeVisible()
    await page.reload()
    await expect(page.getByRole('tab', { name: 'My Information' })).toBeVisible({ timeout: 15_000 })
    await expect(page.locator('header').getByText('Login')).toBeHidden()

    // The page never asked Keycloak to authenticate or refresh anything. (It may still probe Keycloak's
    // passkey health endpoint on load; that only decides whether passkey buttons are shown.)
    expect(
      keycloakRequests.filter((url) => url.includes('/protocol/openid-connect/')),
    ).toStrictEqual([])

    // The block really is in place: a request to Keycloak from the page is intercepted and refused
    const reachedKeycloak = await page.evaluate(
      (url) =>
        fetch(url, { mode: 'no-cors' }).then(
          () => true,
          () => false,
        ),
      `${KEYCLOAK_ORIGIN}/realms/thesis-management/.well-known/openid-configuration`,
    )
    expect(reachedKeycloak).toBe(false)
    expect(keycloakRequests.length).toBeGreaterThan(0)

    await logoutWithApi(context)
    await context.close()
  })

  test('logging out works and ends the session while Keycloak is unreachable', async ({
    browser,
  }) => {
    const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })
    await loginWithApi(context, 'student5')
    const page = await context.newPage()
    await navigateTo(page, '/dashboard')
    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })

    await context.route(`${KEYCLOAK_ORIGIN}/**`, (route) => route.abort('connectionrefused'))
    await page.goto('/logout')

    await expect(page).toHaveURL(/localhost:\d+\/$/, { timeout: 15_000 })
    await expect(page.locator('header').getByText('Login')).toBeVisible({ timeout: 15_000 })
    const session = await context.request.get(`${SERVER_ORIGIN}/api/v2/auth/session`)
    expect(session.status()).toBe(401)

    await context.close()
  })

  test('logging out immediately clears the signed-in UI in another open tab', async ({
    browser,
  }) => {
    const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })

    try {
      await loginWithApi(context, 'student5')
      const page = await context.newPage()
      const otherTab = await context.newPage()
      await navigateTo(page, '/dashboard')
      await navigateTo(otherTab, '/dashboard')
      await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible()
      await expect(otherTab.getByRole('heading', { name: /dashboard/i })).toBeVisible()

      // Logout must clear both tabs even when the university login cannot be reached. Do not navigate or
      // reload the second tab: it must learn about the successful logout through the shared session channel.
      await context.route(`${KEYCLOAK_ORIGIN}/**`, (route) => route.abort('connectionrefused'))
      await page.goto('/logout')

      await expect(page.locator('header').getByText('Login')).toBeVisible()
      await expect(
        otherTab.locator('header').getByRole('button', { name: 'Login', exact: true }),
      ).toBeVisible()
      await expect(otherTab.getByRole('heading', { name: /dashboard/i })).toBeHidden()
      expect((await context.request.get(`${SERVER_ORIGIN}/api/v2/auth/session`)).status()).toBe(401)
    } finally {
      await logoutWithApi(context)
      await context.close()
    }
  })

  test('public pages load for visitors without a session while Keycloak is unreachable', async ({
    browser,
  }) => {
    const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })
    await context.route(`${KEYCLOAK_ORIGIN}/**`, (route) => route.abort('connectionrefused'))
    const page = await context.newPage()

    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible()
    await expect(page.locator('header').getByText('Login')).toBeVisible()

    // logging in needs Keycloak: the user is told so instead of being sent to a dead page or left waiting
    await page.locator('header').getByRole('button', { name: 'Login' }).click()
    await expect(
      page
        .locator('.mantine-Notification-description')
        .filter({ hasText: /login service is currently unavailable/i })
        .first(),
    ).toBeVisible({ timeout: 15_000 })
    await expect(page).toHaveURL(/localhost:\d+\/?$/)

    await context.close()
  })

  test('a lost session cookie without a Keycloak login leads to the login prompt, not to errors', async ({
    browser,
  }) => {
    const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })
    await loginWithApi(context, 'student5')
    const page = await context.newPage()
    await navigateTo(page, '/dashboard')
    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })

    // Only the application session cookie disappears (in real life: the 24 hour limit, or ended from
    // another device). This browser has no Keycloak login either, so the app cannot sign in silently
    // and shows the login prompt instead of failing with errors.
    const sessionCookie = await getSessionCookie(context)
    expect(sessionCookie).toBeDefined()

    try {
      await context.clearCookies({ name: sessionCookie!.name })
      await page.reload()

      await expect(page.locator('header').getByText('Login')).toBeVisible({ timeout: 30_000 })
    } finally {
      // the session itself still exists on the server: put the cookie back so that it can be ended
      await context.addCookies([sessionCookie!])
      await logoutWithApi(context)
      await context.close()
    }
  })

  test('a lost session cookie is replaced silently through the Keycloak login of the browser', async ({
    browser,
  }) => {
    // this storage state holds a Keycloak login (single sign-on cookies) next to the application session
    const context = await browser.newContext({ storageState: authStatePath('student4') })
    const page = await context.newPage()
    const sessionCookie = await getSessionCookie(context)
    expect(sessionCookie).toBeDefined()
    await context.clearCookies({ name: sessionCookie!.name })

    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('header').getByText('Login')).toBeHidden()
    expect(await getSessionCookie(context)).toBeDefined()

    await logoutWithApi(context)
    await context.close()
  })
})
