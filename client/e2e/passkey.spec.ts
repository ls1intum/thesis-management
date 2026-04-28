import { expect, Page, test } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const PASSKEY_PROMPT_TITLE = 'Register a passkey'

const passkeyPromptDialog = (page: Page) => page.getByRole('dialog', { name: PASSKEY_PROMPT_TITLE })

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

test.describe('Passkey - Login', () => {
  test.use({ storageState: authStatePath('student5') })

  test('logs in with passkey from the login modal', async ({ page }) => {
    const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)
    const waitForVirtualPasskey = async () => {
      await expect
        .poll(
          async () =>
            (
              (await cdpSession.send('WebAuthn.getCredentials', {
                authenticatorId,
              })) as { credentials: unknown[] }
            ).credentials.length,
          { timeout: 15_000 },
        )
        .toBeGreaterThan(0)
    }

    try {
      await navigateTo(page, '/settings/account')
      await expect(page.getByRole('heading', { name: 'Passkeys' })).toBeVisible()

      const prompt = passkeyPromptDialog(page)
      if (await prompt.isVisible()) {
        await prompt.getByRole('button', { name: 'Maybe later' }).click()
        await expect(prompt).toBeHidden()
      }

      const passkeyDeleteButton = page.getByRole('button', { name: 'Delete', exact: true })
      while ((await passkeyDeleteButton.count()) > 0) {
        await passkeyDeleteButton.first().click()
        await expect(page.getByText('Passkey deleted successfully')).toBeVisible()
      }

      const registerPasskeyButton = page.getByRole('button', {
        name: 'Register Passkey',
        exact: true,
      })
      const passkeySaveResponsePromise = page.waitForResponse(
        (response) =>
          response.request().method() === 'POST' &&
          response.url().includes('/passkey/') &&
          response.url().includes('/save'),
      )
      await registerPasskeyButton.click()
      const passkeySaveResponse = await passkeySaveResponsePromise
      expect(passkeySaveResponse.ok()).toBe(true)
      await waitForVirtualPasskey()

      await page.goto('/logout', { waitUntil: 'domcontentloaded' })
      const header = page.locator('header')
      await expect(header.getByRole('button', { name: 'Login', exact: true })).toBeVisible({
        timeout: 30_000,
      })
      await header.getByRole('button', { name: 'Login', exact: true }).click()

      const loginModal = page.getByRole('dialog', { name: 'Login' })
      await expect(loginModal).toBeVisible()
      await loginModal.getByRole('button', { name: 'Login with Passkey', exact: true }).click()

      await expect(page).toHaveURL(/\/dashboard/)
      await expect(header.getByText('Login')).toBeHidden()
    } finally {
      await cdpSession
        .send('WebAuthn.removeVirtualAuthenticator', { authenticatorId })
        .catch(() => undefined)
      await cdpSession.send('WebAuthn.disable').catch(() => undefined)
    }
  })
})

test.describe('Passkey - Registration', () => {
  test.use({ storageState: authStatePath('student4') })

  test('registers a passkey using the normal settings flow', async ({ page }) => {
    const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)
    const waitForVirtualPasskey = async () => {
      await expect
        .poll(
          async () =>
            (
              (await cdpSession.send('WebAuthn.getCredentials', {
                authenticatorId,
              })) as { credentials: unknown[] }
            ).credentials.length,
          { timeout: 15_000 },
        )
        .toBeGreaterThan(0)
    }

    try {
      await navigateTo(page, '/settings/account')
      await expect(page.getByRole('heading', { name: 'Passkeys' })).toBeVisible()

      const prompt = passkeyPromptDialog(page)
      if (await prompt.isVisible()) {
        await prompt.getByRole('button', { name: 'Maybe later' }).click()
        await expect(prompt).toBeHidden()
      }

      const passkeyDeleteButton = page.getByRole('button', { name: 'Delete', exact: true })
      while ((await passkeyDeleteButton.count()) > 0) {
        await passkeyDeleteButton.first().click()
        await expect(page.getByText('Passkey deleted successfully')).toBeVisible()
      }

      const registerPasskeyButton = page.getByRole('button', {
        name: 'Register Passkey',
        exact: true,
      })
      const passkeySaveResponsePromise = page.waitForResponse(
        (response) =>
          response.request().method() === 'POST' &&
          response.url().includes('/passkey/') &&
          response.url().includes('/save'),
      )
      await registerPasskeyButton.click()

      const passkeySaveResponse = await passkeySaveResponsePromise
      expect(passkeySaveResponse.ok()).toBe(true)
      await waitForVirtualPasskey()
      await expect(page.getByText('Passkey registered successfully')).toBeVisible()
      await expect(passkeyDeleteButton.first()).toBeVisible()
      await expect(page.getByText('No passkeys registered yet.')).toBeHidden()
    } finally {
      await cdpSession
        .send('WebAuthn.removeVirtualAuthenticator', { authenticatorId })
        .catch(() => undefined)
      await cdpSession.send('WebAuthn.disable').catch(() => undefined)
    }
  })

  test.describe('Passkey - Unauthenticated', () => {
    test.use({ storageState: { cookies: [], origins: [] } })

    test('login modal offers passkey and password methods', async ({ page }) => {
      await navigateTo(page, '/')
      await page.getByRole('button', { name: 'Login' }).click()

      await expect(page.getByRole('dialog', { name: 'Login' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Login with Passkey' })).toBeVisible()
      await expect(page.getByRole('button', { name: 'Login with Password' })).toBeVisible()
    })
  })

  test.describe('Passkey - Authenticated Student', () => {
    test.use({ storageState: authStatePath('student') })

    test('prompts passkey registration after login when no passkey is registered', async ({
      page,
    }) => {
      await page.route('**/realms/**/account/credentials**', async (route) => {
        if (route.request().method() !== 'GET') {
          await route.continue()
          return
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: '[]',
        })
      })

      await navigateTo(page, '/dashboard')

      const prompt = passkeyPromptDialog(page)
      await expect(prompt).toBeVisible()
      await expect(prompt.getByText('Never ask again')).toBeVisible()
      await expect(prompt.getByRole('button', { name: 'Register passkey' })).toBeVisible()
    })

    test('never ask again suppresses future passkey prompts for the same user', async ({
      page,
    }) => {
      await page.route('**/realms/**/account/credentials**', async (route) => {
        if (route.request().method() !== 'GET') {
          await route.continue()
          return
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: '[]',
        })
      })

      await navigateTo(page, '/dashboard')

      const prompt = passkeyPromptDialog(page)
      await expect(prompt).toBeVisible()
      await prompt.getByRole('checkbox', { name: 'Never ask again' }).check()
      await prompt.getByRole('button', { name: 'Maybe later' }).click()
      await expect(prompt).toBeHidden()

      await page.reload({ waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(1500)
      await expect(prompt).toBeHidden()
    })

    test('register passkey button immediately starts registration flow', async ({ page }) => {
      let challengeRequestCount = 0
      let saveRequestCount = 0
      let saveRequestPayload: Record<string, unknown> | undefined

      await page.route('**/realms/**/account/credentials**', async (route) => {
        if (route.request().method() !== 'GET') {
          await route.continue()
          return
        }

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: '[]',
        })
      })

      await page.route('**/realms/**/passkey/**/challenge', async (route) => {
        if (route.request().method() !== 'GET') {
          await route.continue()
          return
        }

        challengeRequestCount += 1
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ challenge: 'dGVzdC1jaGFsbGVuZ2U' }),
        })
      })

      await page.route('**/realms/**/passkey/**/save', async (route) => {
        if (route.request().method() === 'OPTIONS') {
          await route.fulfill({
            status: 204,
            headers: {
              'access-control-allow-origin': '*',
              'access-control-allow-credentials': 'true',
              'access-control-allow-methods': 'GET,POST,OPTIONS',
              'access-control-allow-headers': '*',
            },
          })
          return
        }

        if (route.request().method() !== 'POST') {
          await route.continue()
          return
        }

        saveRequestCount += 1
        saveRequestPayload = JSON.parse(route.request().postData() || '{}') as Record<
          string,
          unknown
        >
        await route.fulfill({
          status: 204,
        })
      })

      const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)

      try {
        await navigateTo(page, '/dashboard')

        const prompt = passkeyPromptDialog(page)
        await expect(prompt).toBeVisible()
        await prompt.getByRole('button', { name: 'Register passkey' }).click()

        await expect(page.getByText('Passkey registered successfully')).toBeVisible()
        await expect(prompt).toBeHidden()
        expect(challengeRequestCount).toBe(1)
        expect(saveRequestCount).toBe(1)
        expect(saveRequestPayload?.challenge).toBe('dGVzdC1jaGFsbGVuZ2U')
        expect(typeof saveRequestPayload?.credentialId).toBe('string')
        expect(typeof saveRequestPayload?.attestationObject).toBe('string')
      } finally {
        await cdpSession.send('WebAuthn.removeVirtualAuthenticator', { authenticatorId })
        await cdpSession.send('WebAuthn.disable')
      }
    })

    test('passkey settings list and delete registered passkeys', async ({ page }) => {
      let deletedCredentialId: string | undefined
      let accountCredentialsPayload: unknown[] = [
        {
          id: 'passkey-1',
          type: 'webauthn-passwordless',
          userLabel: 'Student Device',
          createdDate: 1_735_920_000_000,
        },
        {
          id: 'password-1',
          type: 'password',
          userLabel: 'Legacy credential',
          createdDate: 1_735_920_000_000,
        },
      ]

      await page.route('**/realms/**/account/credentials**', async (route) => {
        const request = route.request()
        const method = request.method()
        const requestUrl = request.url()

        if (method === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify(accountCredentialsPayload),
          })
          return
        }

        if (method === 'DELETE') {
          const urlParts = requestUrl.split('/')
          deletedCredentialId = decodeURIComponent(urlParts[urlParts.length - 1] || '')
          accountCredentialsPayload = []

          await route.fulfill({
            status: 204,
          })
          return
        }

        await route.continue()
      })

      await navigateTo(page, '/settings/account')

      await expect(page.getByRole('heading', { name: 'Passkeys' })).toBeVisible()
      await expect(page.getByText('Student Device')).toBeVisible()
      await expect(page.getByText('Legacy credential')).toBeHidden()

      const passkeyCard = page
        .locator('.mantine-Paper-root')
        .filter({ has: page.getByText('Student Device') })
        .first()
      await passkeyCard.getByRole('button', { name: 'Delete' }).click()
      await expect(page.getByText('Passkey deleted successfully')).toBeVisible()
      expect(deletedCredentialId).toBe('passkey-1')
      await expect(page.getByText('No passkeys registered yet.')).toBeVisible()
    })
  })
})
