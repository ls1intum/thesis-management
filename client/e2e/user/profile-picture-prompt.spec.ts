import { test, expect, type Page, type Response } from '@playwright/test'
import { authStatePath, navigateTo } from '../helpers'

const DISABLE_PROMPT_STORAGE_KEY = 'profile_picture_prompt_disabled'
const PROMPT_TITLE = 'Put a face to your name'

// Matches GET/PUT /v2/user-info, including the empty query string the client appends
const USER_INFO_URL = /\/v2\/user-info\?*$/

// 1x1 transparent PNG
const PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=='

/** Matches a POST to the given user-info sub-path (the client appends an empty query string). */
const isPostTo = (path: string) => (response: Response) =>
  new URL(response.url()).pathname.endsWith(path) && response.request().method() === 'POST'

/** The e2e auth setup disables the prompt; these tests want the real behavior. */
const enablePrompt = async (page: Page) => {
  await page.addInitScript((key) => localStorage.removeItem(key), DISABLE_PROMPT_STORAGE_KEY)
}

/**
 * The decision is persisted in the database and cannot be reset from a test, so the "not yet asked"
 * state is simulated on the user-info response. Requests that change data still hit the real server.
 */
const simulateUserNeverAsked = async (page: Page) => {
  await page.route(USER_INFO_URL, async (route) => {
    if (route.request().method() !== 'GET') {
      return route.continue()
    }

    const response = await route.fetch()
    const user = (await response.json()) as Record<string, unknown>
    delete user.avatar
    delete user.avatarPromptDismissed
    user.avatarPromptDismissed = false

    return route.fulfill({ response, json: user })
  })
}

test.describe('Profile picture prompt', () => {
  test.use({ storageState: authStatePath('student5') })

  test('is shown to a user without picture and can be dismissed for good', async ({ page }) => {
    await enablePrompt(page)
    await simulateUserNeverAsked(page)

    await navigateTo(page, '/dashboard')

    const dialog = page.getByRole('dialog', { name: 'Add a profile picture' })
    await expect(dialog).toBeVisible({ timeout: 30_000 })
    await expect(dialog.getByText(PROMPT_TITLE)).toBeVisible()

    const dismissRequest = page.waitForResponse(isPostTo('/v2/user-info/dismiss-avatar-prompt'))
    await dialog.getByRole('button', { name: 'Maybe not' }).click()

    expect((await dismissRequest).ok()).toBe(true)
    await expect(dialog).toBeHidden()

    // The decision is stored on the server: without the simulation the prompt stays away.
    await page.unroute(USER_INFO_URL)
    await navigateTo(page, '/dashboard')
    await expect(page.getByRole('link', { name: 'Dashboard' }).first()).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByText(PROMPT_TITLE)).toBeHidden()
  })

  test('closing the dialog with Escape also dismisses it', async ({ page }) => {
    await enablePrompt(page)
    await simulateUserNeverAsked(page)

    await navigateTo(page, '/dashboard')

    const dialog = page.getByRole('dialog', { name: 'Add a profile picture' })
    await expect(dialog).toBeVisible({ timeout: 30_000 })

    const dismissRequest = page.waitForResponse(isPostTo('/v2/user-info/dismiss-avatar-prompt'))
    await page.keyboard.press('Escape')

    expect((await dismissRequest).ok()).toBe(true)
    await expect(dialog).toBeHidden()
  })

  test('uploading a picture saves it and closes the dialog', async ({ page }) => {
    await enablePrompt(page)
    await simulateUserNeverAsked(page)

    await navigateTo(page, '/dashboard')

    const dialog = page.getByRole('dialog', { name: 'Add a profile picture' })
    await expect(dialog).toBeVisible({ timeout: 30_000 })

    await dialog.locator('input[type="file"]').setInputFiles({
      name: 'me.png',
      mimeType: 'image/png',
      buffer: Buffer.from(PNG_BASE64, 'base64'),
    })

    const uploadRequest = page.waitForResponse(isPostTo('/v2/user-info/avatar'))
    await page.getByRole('button', { name: 'Save Avatar' }).click()

    const response = await uploadRequest
    expect(response.ok()).toBe(true)
    expect(((await response.json()) as { avatar?: string }).avatar).toBeTruthy()

    await expect(page.getByText(PROMPT_TITLE)).toBeHidden()
  })
})

test.describe('Profile picture prompt - user with picture', () => {
  // The seeded student has a profile picture.
  test.use({ storageState: authStatePath('student') })

  test('is not shown', async ({ page }) => {
    await enablePrompt(page)

    await navigateTo(page, '/dashboard')
    await expect(page.getByRole('link', { name: 'Dashboard' }).first()).toBeVisible({
      timeout: 30_000,
    })

    await expect(page.getByText(PROMPT_TITLE)).toBeHidden()
  })
})
