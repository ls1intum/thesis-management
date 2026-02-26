import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const ALREADY_ANONYMIZED_THESIS_ID = '00000000-0000-4000-d000-000000000009'

test.describe('Thesis Anonymization - Admin Operations', () => {
  test.use({ storageState: authStatePath('admin') })

  test.describe.configure({ mode: 'serial' })

  test('admin can trigger thesis anonymization from admin page', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible({
      timeout: 30_000,
    })

    await expect(page.getByRole('heading', { name: 'Thesis Anonymization' })).toBeVisible()

    await expect(page.getByText(/Anonymize theses that have exceeded/i)).toBeVisible()

    const anonymizeButton = page.getByRole('button', { name: 'Run Anonymization' })
    await expect(anonymizeButton).toBeVisible()
    await anonymizeButton.click()

    await expect(
      page.getByText(/Anonymized \d+ expired thesis|No expired theses found/),
    ).toBeVisible({ timeout: 15_000 })
  })

  test('anonymized thesis shows banner on detail page', async ({ page }) => {
    await navigateTo(page, `/theses/${ALREADY_ANONYMIZED_THESIS_ID}`)

    await expect(page.getByText(/This thesis was anonymized on/i)).toBeVisible({
      timeout: 30_000,
    })

    await expect(page.getByText(/per data retention policy/i)).toBeVisible()
  })
})

test.describe('Thesis Anonymization - Non-Admin Restrictions', () => {
  test.use({ storageState: authStatePath('student') })

  test('student cannot access admin page', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).not.toBeVisible({
      timeout: 10_000,
    })
  })
})
