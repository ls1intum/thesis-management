import { test, expect } from '@playwright/test'
import { SERVER_ORIGIN, loginWithApi, logoutWithApi, navigateTo } from '../helpers'

// A browser family and system that no other spec presents, so the row of the second device can be told apart
// from the sessions of the same seed user that other specs hold in parallel.
const LINUX_FIREFOX = 'Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0'
const LINUX_FIREFOX_LABEL = 'Firefox on Linux'

test.describe('Active sessions', () => {
  test('a user can end a session of another device from the settings', async ({ browser }) => {
    const mine = await browser.newContext({ storageState: { cookies: [], origins: [] } })
    const other = await browser.newContext({
      userAgent: LINUX_FIREFOX,
      storageState: { cookies: [], origins: [] },
    })

    try {
      await loginWithApi(mine, 'student5')
      const myPage = await mine.newPage()

      // a second device, recognisable by its own browser family and system
      await loginWithApi(other, 'student5')
      expect((await other.request.get(`${SERVER_ORIGIN}/api/v2/auth/session`)).status()).toBe(200)

      await navigateTo(myPage, '/settings/account')

      await expect(myPage.getByRole('heading', { name: 'Active sessions' })).toBeVisible({
        timeout: 15_000,
      })
      await expect(myPage.getByText('This device', { exact: true })).toBeVisible()
      // sessions are listed oldest first, so the newest matching row is the one created above
      const otherDevice = myPage
        .locator('.mantine-Paper-root', { hasText: LINUX_FIREFOX_LABEL })
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
    } finally {
      // do not leave sessions of the seed user behind, whether the test passed or not
      await logoutWithApi(mine)
      await logoutWithApi(other)
      await mine.close()
      await other.close()
    }
  })
})
