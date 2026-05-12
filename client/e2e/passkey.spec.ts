import { expect, Page, test } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const PASSKEY_PROMPT_TITLE = 'One click for multiple AET apps'
const NEVER_ASK_AGAIN_STORAGE_KEY_PREFIX = 'passkey_prompt_never_ask_again'
const DISABLE_PASSKEY_PROMPT_STORAGE_KEY = 'passkey_prompt_disabled'

const passkeyPromptDialog = (page: Page) => page.getByRole('dialog', { name: PASSKEY_PROMPT_TITLE })

const disablePasskeyPromptAtStartup = async (page: Page) => {
  await page.addInitScript((storageKey) => {
    localStorage.setItem(storageKey, 'true')
  }, DISABLE_PASSKEY_PROMPT_STORAGE_KEY)
}

const clearPasskeyPromptPreferences = async (page: Page) => {
  await page.evaluate(
    ({ disableStorageKey, perUserStorageKeyPrefix }) => {
      localStorage.removeItem(disableStorageKey)
      for (const key of Object.keys(localStorage)) {
        if (key.startsWith(perUserStorageKeyPrefix)) {
          localStorage.removeItem(key)
        }
      }
    },
    {
      disableStorageKey: DISABLE_PASSKEY_PROMPT_STORAGE_KEY,
      perUserStorageKeyPrefix: NEVER_ASK_AGAIN_STORAGE_KEY_PREFIX,
    },
  )
}

const navigateToPasskeySettings = async (page: Page) => {
  await navigateTo(page, '/settings/account')
  await expect(page.getByRole('heading', { name: 'Passkeys' })).toBeVisible({ timeout: 30_000 })
}

const expectNotification = async (page: Page, text: string) => {
  await expect(
    page.locator('.mantine-Notification-description').filter({ hasText: text }).last(),
  ).toBeVisible({ timeout: 30_000 })
}

const setupVirtualAuthenticator = async (page: Page) => {
  const cdpSession = await page.context().newCDPSession(page)
  await cdpSession.send('WebAuthn.enable')

  const result = (await cdpSession.send('WebAuthn.addVirtualAuthenticator', {
    options: {
      protocol: 'ctap2',
      transport: 'internal',
      hasResidentKey: true,
      hasUserVerification: true,
      isUserVerified: true,
      automaticPresenceSimulation: true,
    },
  })) as { authenticatorId: string }

  return { cdpSession, authenticatorId: result.authenticatorId }
}

type WebAuthnCdpSession = {
  send: (method: string, params?: Record<string, unknown>) => Promise<unknown>
}

const teardownVirtualAuthenticator = async (
  cdpSession: WebAuthnCdpSession,
  authenticatorId: string,
) => {
  await cdpSession.send('WebAuthn.removeVirtualAuthenticator', { authenticatorId }).catch(() => {})
  await cdpSession.send('WebAuthn.disable').catch(() => {})
}

const waitForVirtualPasskey = async (cdpSession: WebAuthnCdpSession, authenticatorId: string) => {
  await expect
    .poll(
      async () =>
        (
          (await cdpSession.send('WebAuthn.getCredentials', {
            authenticatorId,
          })) as { credentials: unknown[] }
        ).credentials.length,
      { timeout: 30_000 },
    )
    .toBeGreaterThan(0)
}

const deleteExistingPasskeys = async (page: Page) => {
  const passkeysContainer = page.getByRole('heading', { name: 'Passkeys' }).locator('..')
  const deleteButtons = passkeysContainer.getByRole('button', { name: 'Delete', exact: true })
  const existingPasskeysCount = await deleteButtons.count()

  for (let index = 0; index < existingPasskeysCount; index += 1) {
    const currentDeleteButton = deleteButtons.first()
    await expect(currentDeleteButton).toBeEnabled({ timeout: 15_000 })
    await currentDeleteButton.click()
    await expectNotification(page, 'Passkey deleted successfully')
  }
}

const registerPasskeyFromSettings = async (page: Page) => {
  await page.getByRole('button', { name: 'Register Passkey', exact: true }).click()
  await expectNotification(page, 'Passkey registered successfully')
}

const prepareUserWithoutPasskeys = async (page: Page) => {
  await navigateToPasskeySettings(page)
  await deleteExistingPasskeys(page)
  await expect(page.getByText('No passkeys registered yet.')).toBeVisible({ timeout: 15_000 })
}

test.describe('Passkey - Prompt', () => {
  test.describe('Temporary dismissal', () => {
    test.use({ storageState: authStatePath('student2') })

    test('shows passkey options and reappears after Maybe later', async ({ page }) => {
      await prepareUserWithoutPasskeys(page)
      await clearPasskeyPromptPreferences(page)

      await page.reload()

      const promptDialog = passkeyPromptDialog(page)
      await expect(promptDialog).toBeVisible({ timeout: 30_000 })
      await expect(promptDialog.getByText('One passkey for fast, secure sign-in across multiple apps.')).toBeVisible()
      await expect(promptDialog.getByRole('checkbox', { name: 'Never ask again' })).not.toBeChecked()
      await expect(promptDialog.getByRole('button', { name: 'Maybe later' })).toBeVisible()
      await expect(promptDialog.getByRole('button', { name: 'Register Passkey' })).toBeVisible()

      await promptDialog.getByRole('button', { name: 'Maybe later' }).click()
      await expect(promptDialog).toBeHidden()

      await page.reload()
      await expect(promptDialog).toBeVisible({ timeout: 30_000 })
    })
  })

  test.describe('Never ask again', () => {
    test.use({ storageState: authStatePath('student3') })

    test('persists per-user preference after dismissal', async ({ page }) => {
      await prepareUserWithoutPasskeys(page)
      await clearPasskeyPromptPreferences(page)

      await page.reload()

      const promptDialog = passkeyPromptDialog(page)
      await expect(promptDialog).toBeVisible({ timeout: 30_000 })
      await promptDialog.getByRole('checkbox', { name: 'Never ask again' }).check()
      await promptDialog.getByRole('button', { name: 'Maybe later' }).click()
      await expect(promptDialog).toBeHidden()

      const neverAskAgainKeys = await page.evaluate((storageKeyPrefix) => {
        return Object.keys(localStorage).filter((key) => key.startsWith(storageKeyPrefix))
      }, NEVER_ASK_AGAIN_STORAGE_KEY_PREFIX)
      expect(neverAskAgainKeys.length).toBeGreaterThan(0)

      await page.reload()
      await expect(promptDialog).toBeHidden()
    })
  })

  test.describe('Registration from prompt', () => {
    test.use({ storageState: authStatePath('student4') })

    test('registers a passkey directly from the prompt modal', async ({ page }) => {
      const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)
      try {
        await prepareUserWithoutPasskeys(page)
        await clearPasskeyPromptPreferences(page)

        await page.reload()

        const promptDialog = passkeyPromptDialog(page)
        await expect(promptDialog).toBeVisible({ timeout: 30_000 })
        await promptDialog.getByRole('button', { name: 'Register Passkey' }).click()

        await expectNotification(page, 'Passkey registered successfully')
        await expect(promptDialog).toBeHidden()
        await waitForVirtualPasskey(cdpSession, authenticatorId)

        await navigateToPasskeySettings(page)
        await expect(page.getByRole('button', { name: 'Delete', exact: true })).toBeVisible()
      } finally {
        await teardownVirtualAuthenticator(cdpSession, authenticatorId)
      }
    })
  })
})

test.describe('Passkey - Settings', () => {
  test.use({ storageState: authStatePath('student5') })

  test('registers and deletes a passkey from account settings', async ({ page }) => {
    await disablePasskeyPromptAtStartup(page)

    const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)
    try {
      await prepareUserWithoutPasskeys(page)
      await registerPasskeyFromSettings(page)
      await waitForVirtualPasskey(cdpSession, authenticatorId)

      const passkeysContainer = page.getByRole('heading', { name: 'Passkeys' }).locator('..')
      const deleteButtons = passkeysContainer.getByRole('button', { name: 'Delete', exact: true })
      await expect(deleteButtons).toHaveCount(1)

      await deleteButtons.first().click()
      await expectNotification(page, 'Passkey deleted successfully')
      await expect(page.getByText('No passkeys registered yet.')).toBeVisible()
    } finally {
      await teardownVirtualAuthenticator(cdpSession, authenticatorId)
    }
  })
})

test.describe('Passkey - Login', () => {
  test.use({ storageState: authStatePath('examiner2') })

  test('signs in with a registered passkey from the login modal', async ({ page }) => {
    await disablePasskeyPromptAtStartup(page)

    const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)
    try {
      await prepareUserWithoutPasskeys(page)
      await registerPasskeyFromSettings(page)
      await waitForVirtualPasskey(cdpSession, authenticatorId)

      await page.goto('/logout')
      await expect(page.locator('header').getByText('Login')).toBeVisible({ timeout: 60_000 })

      await page.goto('/dashboard')
      const loginModal = page.getByRole('dialog', { name: 'Login' })
      await expect(loginModal).toBeVisible({ timeout: 30_000 })
      await loginModal.getByRole('button', { name: 'AET Passkey' }).click()

      await expect(page).toHaveURL(/\/dashboard/, { timeout: 60_000 })
      await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({
        timeout: 30_000,
      })
      await expect(page.locator('header').getByText('Login')).toBeHidden()
    } finally {
      await teardownVirtualAuthenticator(cdpSession, authenticatorId)
    }
  })
})
