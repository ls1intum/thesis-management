import { expect, Page, test } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const PASSKEY_PROMPT_TITLE = 'One click for multiple AET apps'
const NEVER_ASK_AGAIN_STORAGE_KEY = 'passkey_prompt_never_ask_again'
const DISABLE_PASSKEY_PROMPT_STORAGE_KEY = 'passkey_prompt_disabled'

const passkeyPromptDialog = (page: Page) => page.getByRole('dialog', { name: PASSKEY_PROMPT_TITLE })

const disablePasskeyPrompt = async (page: Page) => {
  await page.addInitScript((storageKey) => {
    localStorage.setItem(storageKey, 'true')
  }, DISABLE_PASSKEY_PROMPT_STORAGE_KEY)
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
    await expect(page.getByText('Passkey deleted successfully')).toBeVisible()
  }
}

test.describe('Passkey - Login', () => {
  test.use({ storageState: authStatePath('student5') })

  test('logs in with passkey from the login modal', async ({ page }) => {
    await disablePasskeyPrompt(page)
    const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)
    let authenticateRequestPayload: Record<string, unknown> | undefined

    try {
      await navigateTo(page, '/settings/account')
      await expect(page.getByRole('heading', { name: 'Passkeys' })).toBeVisible()

      const prompt = passkeyPromptDialog(page)
      if (await prompt.isVisible({ timeout: 2_000 }).catch(() => false)) {
        await prompt.getByRole('button', { name: 'Maybe later' }).click()
        await expect(prompt).toBeHidden()
      }

      await deleteExistingPasskeys(page)

      const registerPasskeyButton = page.getByRole('button', {
        name: 'Register Passkey',
        exact: true,
      })
      await expect(registerPasskeyButton).toBeEnabled()
      await registerPasskeyButton.click()
      await waitForVirtualPasskey(cdpSession, authenticatorId)

      await page.route('**/realms/**/passkey/**/authenticate', async (route) => {
        if (route.request().method() === 'POST') {
          authenticateRequestPayload = JSON.parse(route.request().postData() || '{}') as Record<
            string,
            unknown
          >
        }

        await route.continue()
      })

      const header = page.locator('header')
      const loginLink = header.getByRole('link', { name: 'Login', exact: true })

      await page.context().clearCookies()
      await page.evaluate(
        ({ disablePromptStorageKey, neverAskAgainStorageKey }) => {
          localStorage.removeItem('authentication_tokens')
          localStorage.setItem(disablePromptStorageKey, 'true')
          Object.keys(localStorage)
            .filter((key) => key.startsWith(`${neverAskAgainStorageKey}_`))
            .forEach((key) => localStorage.removeItem(key))
          sessionStorage.clear()
        },
        {
          disablePromptStorageKey: DISABLE_PASSKEY_PROMPT_STORAGE_KEY,
          neverAskAgainStorageKey: NEVER_ASK_AGAIN_STORAGE_KEY,
        },
      )
      await page.goto('/', { waitUntil: 'domcontentloaded' })
      await expect(loginLink).toBeVisible({ timeout: 15_000 })
      await loginLink.click()

      const loginModal = page.getByRole('dialog', { name: 'Login' })
      await expect(loginModal).toBeVisible()
      await loginModal.getByRole('button', { name: 'Login with AET Passkey', exact: true }).click()

      await expect(loginModal).toBeHidden()
      await expect(page).toHaveURL(/\/dashboard/)
      await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({
        timeout: 30_000,
      })
      await expect(header.getByText('Login')).toBeHidden()
      expect(typeof authenticateRequestPayload?.userHandle).toBe('string')
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
    await disablePasskeyPrompt(page)
    const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)

    try {
      await navigateTo(page, '/settings/account')
      await expect(page.getByRole('heading', { name: 'Passkeys' })).toBeVisible()

      const prompt = passkeyPromptDialog(page)
      if (await prompt.isVisible({ timeout: 2_000 }).catch(() => false)) {
        await prompt.getByRole('button', { name: 'Maybe later' }).click()
        await expect(prompt).toBeHidden()
      }

      await deleteExistingPasskeys(page)

      const registerPasskeyButton = page.getByRole('button', {
        name: 'Register Passkey',
        exact: true,
      })
      await expect(registerPasskeyButton).toBeEnabled()
      await registerPasskeyButton.click()

      await waitForVirtualPasskey(cdpSession, authenticatorId)
      await expect(page.getByText('Passkey registered successfully')).toBeVisible()
      await expect(page.getByRole('button', { name: 'Delete', exact: true }).first()).toBeVisible()
      await expect(page.getByText('No passkeys registered yet.')).toBeHidden()
    } finally {
      await cdpSession
        .send('WebAuthn.removeVirtualAuthenticator', { authenticatorId })
        .catch(() => undefined)
      await cdpSession.send('WebAuthn.disable').catch(() => undefined)
    }
  })
})

test.describe('Passkey - Unauthenticated', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('login modal offers passkey and password methods', async ({ page }) => {
    await navigateTo(page, '/')
    await page.getByRole('link', { name: 'Login' }).click()

    await expect(page.getByRole('dialog', { name: 'Login' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Login with AET Passkey' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Login with TUM-Login' })).toBeVisible()
  })
})

test.describe('Passkey - Authenticated Student', () => {
  test.use({ storageState: authStatePath('student') })

  test('prompts passkey registration after login when no passkey is registered', async ({
    page,
  }) => {
    await page.addInitScript((storageKey) => {
      localStorage.setItem(storageKey, 'false')
    }, DISABLE_PASSKEY_PROMPT_STORAGE_KEY)

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
    await expect(
      prompt.getByText('One passkey for fast, secure sign-in across multiple apps.'),
    ).toBeVisible()
    await expect(prompt.getByRole('link', { name: 'AET' })).toHaveAttribute(
      'href',
      'https://aet.cit.tum.de/',
    )
    await expect(prompt.getByText('Thesis Management')).toBeVisible()
    await expect(prompt.getByText('TUM Apply')).toBeVisible()
    await expect(prompt.getByText('Prompt')).toBeVisible()
    await expect(prompt.getByText('Never ask again')).toBeVisible()
    await expect(prompt.getByRole('button', { name: 'Register Passkey' })).toBeVisible()
  })

  test('never ask again suppresses future passkey prompts for the same user', async ({ page }) => {
    await page.addInitScript((storageKey) => {
      localStorage.setItem(storageKey, 'false')
    }, DISABLE_PASSKEY_PROMPT_STORAGE_KEY)

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
    await expect
      .poll(async () =>
        page.evaluate((storageKeyPrefix) => {
          const matchingKeys = Object.keys(localStorage).filter((key) =>
            key.startsWith(`${storageKeyPrefix}_`),
          )
          return matchingKeys.some((key) => localStorage.getItem(key) === 'true')
        }, NEVER_ASK_AGAIN_STORAGE_KEY),
      )
      .toBe(true)

    await page.reload({ waitUntil: 'domcontentloaded' })
    await expect(prompt).toBeHidden({ timeout: 5000 })
  })

  test('settings register passkey button immediately starts registration flow', async ({
    page,
  }) => {
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
            'access-control-allow-origin': 'http://localhost:3100',
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
      saveRequestPayload = JSON.parse(route.request().postData() || '{}') as Record<string, unknown>
      await route.fulfill({
        status: 204,
      })
    })

    const { cdpSession, authenticatorId } = await setupVirtualAuthenticator(page)

    try {
      await navigateTo(page, '/settings/account')
      await expect(page.getByRole('heading', { name: 'Passkeys' })).toBeVisible()

      const registerPasskeyButton = page.getByRole('button', {
        name: 'Register Passkey',
        exact: true,
      })
      await expect(registerPasskeyButton).toBeEnabled()
      await registerPasskeyButton.click()

      await expect(page.getByText('Passkey registered successfully')).toBeVisible()
      await expect(passkeyPromptDialog(page)).toBeHidden()
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
