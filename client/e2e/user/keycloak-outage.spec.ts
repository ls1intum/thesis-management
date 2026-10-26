import { test, expect } from '@playwright/test'
import {
  KEYCLOAK_ORIGIN,
  SERVER_ORIGIN,
  authStatePath,
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

    // the page never asked Keycloak to authenticate or refresh anything
    expect(
      keycloakRequests.filter((url) => url.includes('/protocol/openid-connect/')),
    ).toStrictEqual([])

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

  test('public pages load for visitors without a session while Keycloak is unreachable', async ({
    browser,
  }) => {
    const context = await browser.newContext({ storageState: { cookies: [], origins: [] } })
    await context.route(`${KEYCLOAK_ORIGIN}/**`, (route) => route.abort('connectionrefused'))
    const page = await context.newPage()

    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible()
    await expect(page.locator('header').getByText('Login')).toBeVisible()

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
    await context.clearCookies({ name: 'sid' })
    await page.reload()

    await expect(page.locator('header').getByText('Login')).toBeVisible({ timeout: 30_000 })

    await context.close()
  })

  test('a lost session cookie is replaced silently through the Keycloak login of the browser', async ({
    browser,
  }) => {
    // this storage state holds a Keycloak login (single sign-on cookies) next to the application session
    const context = await browser.newContext({ storageState: authStatePath('student4') })
    const page = await context.newPage()
    await context.clearCookies({ name: 'sid' })

    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('header').getByText('Login')).toBeHidden()
    expect((await context.cookies()).some((cookie) => cookie.name === 'sid')).toBe(true)

    await logoutWithApi(context)
    await context.close()
  })
})
