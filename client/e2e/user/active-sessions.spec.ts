import { test, expect } from '@playwright/test'
import { SERVER_ORIGIN, loginWithApi, logoutWithApi, navigateTo } from '../helpers'

const WINDOWS_CHROME =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36'

test.describe('Active sessions', () => {
  test('a user can end a session of another device from the settings', async ({ browser }) => {
    const mine = await browser.newContext({ storageState: { cookies: [], origins: [] } })
    await loginWithApi(mine, 'student5')
    const myPage = await mine.newPage()

    // a second device, recognisable by its own browser family and system
    const other = await browser.newContext({
      userAgent: WINDOWS_CHROME,
      storageState: { cookies: [], origins: [] },
    })
    await loginWithApi(other, 'student5')
    expect((await other.request.get(`${SERVER_ORIGIN}/api/v2/auth/session`)).status()).toBe(200)

    await navigateTo(myPage, '/settings/account')

    await expect(myPage.getByRole('heading', { name: 'Active sessions' })).toBeVisible({
      timeout: 15_000,
    })
    await expect(myPage.getByText('This device', { exact: true })).toBeVisible()
    // sessions are listed oldest first, so the newest "Chrome on Windows" row is the one created above
    const otherDevice = myPage
      .locator('.mantine-Paper-root', { hasText: 'Chrome on Windows' })
      .last()
    await expect(otherDevice).toBeVisible()

    await otherDevice.getByRole('button', { name: 'End session' }).click()

    await expect(
      myPage
        .locator('.mantine-Notification-description')
        .filter({ hasText: 'Session ended.' })
        .last(),
    ).toBeVisible({ timeout: 15_000 })
    expect((await other.request.get(`${SERVER_ORIGIN}/api/v2/auth/session`)).status()).toBe(401)
    expect((await mine.request.get(`${SERVER_ORIGIN}/api/v2/auth/session`)).status()).toBe(200)

    await logoutWithApi(mine)
    await mine.close()
    await other.close()
  })
})
