import { test, expect, TestInfo } from '@playwright/test'
import { authStatePath, navigateTo, navigateToThesisConfig } from '../helpers'

// Anonymization is irreversible. Once a test attempt anonymizes a thesis, a
// Playwright retry against the same row would fail at the "Anonymize Thesis"
// button visibility check (the button is gated on !anonymizedAt). Each test
// attempt therefore uses a different seed thesis, indexed by `testInfo.retry`.
// The retry-slot rows (suffixes a0/b0/a1/b1/a2/b2) are seeded with identical
// state and dates to their base counterparts in seed_dev_test_data.sql.
const OLD_THESIS_IDS = [
  '00000000-0000-4000-d000-000000000010',
  '00000000-0000-4000-d000-0000000000a0',
  '00000000-0000-4000-d000-0000000000b0',
]
const RECENT_THESIS_IDS = [
  '00000000-0000-4000-d000-000000000011',
  '00000000-0000-4000-d000-0000000000a1',
  '00000000-0000-4000-d000-0000000000b1',
]
const ACTIVE_THESIS_IDS = [
  '00000000-0000-4000-d000-000000000012',
  '00000000-0000-4000-d000-0000000000a2',
  '00000000-0000-4000-d000-0000000000b2',
]
// Thesis 1 — always available, examiner has EXAMINER role on it
const EXAMINER_THESIS_ID = '00000000-0000-4000-d000-000000000001'

function thesisIdForAttempt(ids: string[], info: TestInfo): string {
  // Fall through to the last id if a developer raises `retries` beyond the
  // configured slots — better to reuse than crash with an out-of-range lookup.
  return ids[Math.min(info.retry, ids.length - 1)]
}

test.describe('Thesis Delete (Anonymize) - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test.describe.configure({ mode: 'serial' })

  test('admin can anonymize old non-terminal thesis with state warning only', async ({ page }, testInfo) => {
    const thesisId = thesisIdForAttempt(OLD_THESIS_IDS, testInfo)
    await navigateToThesisConfig(page, thesisId)

    // Anonymize Thesis button should be visible for admin on non-anonymized thesis
    const deleteButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await expect(deleteButton).toBeVisible()
    await deleteButton.click()

    // Modal should open with correct title
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('heading', { name: 'Anonymize Thesis' })).toBeVisible()

    // Old GRADED thesis: state warning (not terminal) but NO retention warning (expired)
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible()
    await expect(alert.getByText(/GRADED/i)).toBeVisible()
    await expect(alert.getByText(/retention period/i)).not.toBeVisible({ timeout: 2_000 })

    // Confirmation text should mention anonymization, not deletion
    await expect(dialog.getByText(/anonymize this thesis/i)).toBeVisible()
    await expect(dialog.getByText(/This action cannot be undone/i)).toBeVisible()
    await expect(dialog.getByText(/structural thesis metadata is retained/i)).toBeVisible()

    // Cancel and Anonymize buttons should be present
    await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeVisible()

    // Confirm anonymize
    await dialog.getByRole('button', { name: 'Anonymize Thesis' }).click()

    // Should redirect to /theses list after successful anonymization
    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })

  test('admin can anonymize recent thesis with retention warning', async ({ page }, testInfo) => {
    const thesisId = thesisIdForAttempt(RECENT_THESIS_IDS, testInfo)
    await navigateToThesisConfig(page, thesisId)

    const deleteButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await expect(deleteButton).toBeVisible()
    await deleteButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()

    // Recent finished thesis should show retention warning but NOT state warning
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible()
    await expect(alert.getByText(/retention period/i)).toBeVisible()
    await expect(alert.getByText(/expires on/i)).toBeVisible()
    await expect(alert.getByText(/WRITING|GRADED|SUBMITTED/i)).not.toBeVisible({ timeout: 2_000 })

    // Confirm anonymize despite warning
    await dialog.getByRole('button', { name: 'Anonymize Thesis' }).click()

    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })

  test('admin can anonymize active thesis with state and retention warnings', async ({ page }, testInfo) => {
    const thesisId = thesisIdForAttempt(ACTIVE_THESIS_IDS, testInfo)
    await navigateToThesisConfig(page, thesisId)

    const deleteButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await expect(deleteButton).toBeVisible()
    await deleteButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()

    // Active (WRITING) thesis should show state warning
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible()
    await expect(alert.getByText(/WRITING/i)).toBeVisible()
    // Should also show retention warning since thesis is only 30 days old
    await expect(alert.getByText(/retention period/i)).toBeVisible()

    // Confirm anonymize despite warnings
    await dialog.getByRole('button', { name: 'Anonymize Thesis' }).click()

    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })
})

test.describe('Thesis Delete (Anonymize) - Modal Interactions', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin can cancel anonymization modal without effect', async ({ page }) => {
    // Use thesis 1 which won't be affected by serial delete tests
    await navigateToThesisConfig(page, EXAMINER_THESIS_ID)

    const anonymizeButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await expect(anonymizeButton).toBeVisible({ timeout: 5_000 })
    await anonymizeButton.click()

    // Modal should open
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Click Cancel
    await dialog.getByRole('button', { name: 'Cancel' }).click()

    // Modal should close
    await expect(dialog).not.toBeVisible({ timeout: 3_000 })

    // Anonymize button should still be visible (thesis wasn't anonymized)
    await expect(anonymizeButton).toBeVisible()
  })

  test('anonymization modal closes via X button', async ({ page }) => {
    await navigateToThesisConfig(page, EXAMINER_THESIS_ID)

    const anonymizeButton = page.getByRole('button', { name: 'Anonymize Thesis' })
    await anonymizeButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Close via the modal close button (X)
    await dialog.locator('button.mantine-Modal-close').click()

    // Modal should close and thesis should remain unchanged
    await expect(dialog).not.toBeVisible({ timeout: 3_000 })
  })
})

test.describe('Thesis Delete (Anonymize) - Non-Admin Restrictions', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner does not see Anonymize Thesis button', async ({ page }) => {
    // Use thesis 1 where examiner has EXAMINER role — not affected by admin delete tests
    await navigateToThesisConfig(page, EXAMINER_THESIS_ID)

    // Examiner reaches the config page but does NOT see the Anonymize Thesis button
    await expect(page.getByRole('button', { name: 'Anonymize Thesis' })).not.toBeVisible({
      timeout: 3_000,
    })
  })
})

test.describe('Thesis Delete (Anonymize) - Student Restrictions', () => {
  test.use({ storageState: authStatePath('student') })

  test('student cannot access Configuration page and does not see the link', async ({ page }) => {
    // Visiting the config URL should redirect the student back to the thesis page
    await navigateTo(page, `/theses/${EXAMINER_THESIS_ID}/configuration`)
    await expect(page).toHaveURL(new RegExp(`${EXAMINER_THESIS_ID}$`), { timeout: 15_000 })

    // The Configuration link is hidden from students in the header
    await expect(page.getByRole('link', { name: 'Configuration' })).toBeHidden()

    // Anonymize Thesis button must not exist anywhere on the page
    await expect(page.getByRole('button', { name: 'Anonymize Thesis' })).toBeHidden()
  })
})
