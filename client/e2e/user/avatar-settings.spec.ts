import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from '../helpers'

// 1x1 transparent PNG
const PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=='

// Regression test: the settings form requested the saved picture without the login token. The avatar endpoint only
// serves anonymous requests for publicly listed people, so for students the form showed their initials after a
// reload and they believed their picture had been deleted.
test.describe('Profile picture in the settings', () => {
  test.use({ storageState: authStatePath('student2') })

  test('a saved picture is still shown, and kept, after reloading the settings', async ({
    page,
  }) => {
    test.setTimeout(120_000)

    await navigateTo(page, '/settings')
    await expect(page.getByText('My Information')).toBeVisible({ timeout: 30_000 })

    await page.locator('input[type="file"][accept^="image/"]').setInputFiles({
      name: 'me.png',
      mimeType: 'image/png',
      buffer: Buffer.from(PNG_BASE64, 'base64'),
    })
    await page.getByRole('button', { name: 'Save Avatar' }).click()

    const privacyCheckbox = page.getByRole('checkbox', { name: /privacy/i })
    if (!(await privacyCheckbox.isChecked())) {
      await privacyCheckbox.check()
    }
    await page.getByRole('button', { name: 'Update Information', exact: true }).click()
    await expect(page.getByText('You successfully updated your profile')).toBeVisible({
      timeout: 15_000,
    })

    // a fresh page load takes the picture from the server, not from the file that was just chosen
    await page.reload()
    const picture = page.locator('.mantine-Dropzone-root:has(input[accept^="image/"]) img').first()
    await expect(picture).toBeVisible({ timeout: 30_000 })
    await expect(picture).toHaveAttribute('src', /^blob:/)
    await expect
      .poll(() => picture.evaluate((image: HTMLImageElement) => image.naturalWidth))
      .toBeGreaterThan(0)

    // saving the profile again without choosing a picture must not remove it
    const consent = page.getByRole('checkbox', { name: /privacy/i })
    if (!(await consent.isChecked())) {
      await consent.check()
    }
    await page.getByRole('button', { name: 'Update Information', exact: true }).click()
    await expect(page.getByText('You successfully updated your profile')).toBeVisible({
      timeout: 15_000,
    })
    await page.reload()
    await expect(
      page.locator('.mantine-Dropzone-root:has(input[accept^="image/"]) img').first(),
    ).toBeVisible({
      timeout: 30_000,
    })
  })
})
