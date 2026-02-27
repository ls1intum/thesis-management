import { test, expect } from '@playwright/test'
import { authStatePath, navigateToDetail } from './helpers'

const OLD_THESIS_ID = '00000000-0000-4000-d000-000000000010'
const RECENT_THESIS_ID = '00000000-0000-4000-d000-000000000011'
const ACTIVE_THESIS_ID = '00000000-0000-4000-d000-000000000012'
// Thesis 1 — always available, supervisor has SUPERVISOR role on it
const SUPERVISOR_THESIS_ID = '00000000-0000-4000-d000-000000000001'

test.describe('Thesis Delete (Anonymize) - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test.describe.configure({ mode: 'serial' })

  test('admin can delete old non-terminal thesis with state warning only', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Historical Analysis of Compiler/i })
    const loaded = await navigateToDetail(page, `/theses/${OLD_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    // Open Configuration accordion
    await page.getByText('Configuration').click()

    // Delete Thesis button should be visible for admin on non-anonymized thesis
    const deleteButton = page.getByRole('button', { name: 'Delete Thesis' })
    await expect(deleteButton).toBeVisible({ timeout: 5_000 })
    await deleteButton.click()

    // Modal should open with correct title
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    await expect(dialog.getByRole('heading', { name: 'Delete Thesis' })).toBeVisible()

    // Old GRADED thesis: state warning (not terminal) but NO retention warning (expired)
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible({ timeout: 5_000 })
    await expect(alert.getByText(/GRADED/i)).toBeVisible()
    await expect(alert.getByText(/retention period/i)).not.toBeVisible({ timeout: 2_000 })

    // Confirmation text should be present
    await expect(
      dialog.getByText(/permanently delete this thesis/i),
    ).toBeVisible()
    await expect(dialog.getByText(/This action cannot be undone/i)).toBeVisible()

    // Cancel and Delete buttons should be present
    await expect(dialog.getByRole('button', { name: 'Cancel' })).toBeVisible()

    // Confirm delete
    await dialog.getByRole('button', { name: 'Delete Thesis' }).click()

    // Should redirect to /theses list after successful deletion
    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })

  test('admin can delete recent thesis with retention warning', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Machine Learning Approaches/i })
    const loaded = await navigateToDetail(page, `/theses/${RECENT_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    await page.getByText('Configuration').click()

    const deleteButton = page.getByRole('button', { name: 'Delete Thesis' })
    await expect(deleteButton).toBeVisible({ timeout: 5_000 })
    await deleteButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Recent finished thesis should show retention warning but NOT state warning
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible({ timeout: 5_000 })
    await expect(alert.getByText(/retention period/i)).toBeVisible()
    await expect(alert.getByText(/expires on/i)).toBeVisible()

    // Confirm delete despite warning
    await dialog.getByRole('button', { name: 'Delete Thesis' }).click()

    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })

  test('admin can delete active thesis with state and retention warnings', async ({ page }) => {
    const heading = page.getByRole('heading', { name: /Real-Time Anomaly Detection/i })
    const loaded = await navigateToDetail(page, `/theses/${ACTIVE_THESIS_ID}`, heading, 30_000)
    expect(loaded).toBeTruthy()

    await page.getByText('Configuration').click()

    const deleteButton = page.getByRole('button', { name: 'Delete Thesis' })
    await expect(deleteButton).toBeVisible({ timeout: 5_000 })
    await deleteButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Active (WRITING) thesis should show state warning
    const alert = dialog.locator('.mantine-Alert-root')
    await expect(alert).toBeVisible({ timeout: 5_000 })
    await expect(alert.getByText(/WRITING/i)).toBeVisible()
    // Should also show retention warning since thesis is only 30 days old
    await expect(alert.getByText(/retention period/i)).toBeVisible()

    // Confirm delete despite warnings
    await dialog.getByRole('button', { name: 'Delete Thesis' }).click()

    await expect(page).toHaveURL(/\/theses(?:\?|$)/, { timeout: 15_000 })
  })
})

test.describe('Thesis Delete (Anonymize) - Non-Admin Restrictions', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor does not see Delete Thesis button', async ({ page }) => {
    // Use thesis 1 where supervisor has SUPERVISOR role — not affected by admin delete tests
    const heading = page.getByRole('heading', { name: /Automated Code Review/i })
    const loaded = await navigateToDetail(
      page,
      `/theses/${SUPERVISOR_THESIS_ID}`,
      heading,
      30_000,
    )
    expect(loaded).toBeTruthy()

    // Open Configuration accordion
    await page.getByText('Configuration').click()

    // Supervisor should see Update button (they have advisor access) but NOT Delete Thesis
    await expect(page.getByRole('button', { name: 'Update' })).toBeVisible({ timeout: 5_000 })
    await expect(page.getByRole('button', { name: 'Delete Thesis' })).not.toBeVisible({
      timeout: 3_000,
    })
  })
})
