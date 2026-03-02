import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

// ============================================================================
// Self-service account deletion (Settings > Account tab)
//
// NOTE: Destructive tests (actual deletion) check if the user was already
// deleted in a prior run and skip gracefully. The E2E setup script resets
// the database before each run, but this guard handles partial re-runs.
// ============================================================================

/**
 * Helper: navigate to the Account tab and check if the user's account
 * is still active (i.e. the Delete Account heading loads). Returns false
 * if the user was already deleted/deactivated in a prior run.
 */
async function navigateToAccountTab(page: import('@playwright/test').Page): Promise<boolean> {
  await navigateTo(page, '/settings/account')
  const heading = page.getByRole('heading', { name: 'Delete Account' })
  return heading.isVisible({ timeout: 15_000 }).catch(() => false)
}

// ============================================================================
// Full deletion: user with only a rejected application (no retention)
// ============================================================================

test.describe('Account Deletion - Self-Service (Full Deletion)', () => {
  test.use({ storageState: authStatePath('delete_rejected_app') })
  test.describe.configure({ mode: 'serial' })

  test('account tab shows full-deletion preview for user with rejected application', async ({
    page,
  }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    // User with only a rejected application should see full deletion message
    await expect(page.getByText(/permanently deleted/i)).toBeVisible({ timeout: 10_000 })

    // No retention notice should be shown for this user
    await expect(page.getByText('Data Retention Notice', { exact: true })).not.toBeVisible({
      timeout: 3_000,
    })

    // No research group head warning
    await expect(page.getByText('Research Group Head', { exact: true })).not.toBeVisible({
      timeout: 3_000,
    })

    // Delete button should be enabled
    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeVisible()
    await expect(deleteButton).toBeEnabled()
  })

  test('user can delete their own account (full deletion)', async ({ page }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled({ timeout: 10_000 })
    await deleteButton.click()

    // Confirmation modal should appear
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Modal should indicate permanent deletion (not deactivation)
    await expect(dialog.getByText(/permanently deleted/i)).toBeVisible()
    await expect(dialog.getByText(/deactivated/i)).not.toBeVisible({ timeout: 2_000 })

    // Confirm button should be disabled until full name is typed
    const confirmButton = dialog.getByRole('button', { name: 'Yes, Delete My Account' })
    await expect(confirmButton).toBeDisabled()

    // Typing wrong name keeps button disabled
    await dialog.getByRole('textbox').fill('Wrong Name')
    await expect(confirmButton).toBeDisabled()

    // Typing correct name enables button
    await dialog.getByRole('textbox').fill('RejectedApp Deletable')
    await expect(confirmButton).toBeEnabled()
    await confirmButton.click()

    // Should redirect away from settings after logout
    // After deletion, the app logs out and redirects to the homepage or login page
    await expect(page.getByRole('heading', { name: 'Delete Account' })).not.toBeVisible({
      timeout: 30_000,
    })
  })

  test('deleted user can no longer access protected pages', async ({ page }) => {
    // After deletion the session is invalidated — navigating to a protected
    // page should NOT show the Delete Account section.
    await page.goto('/settings/account', { waitUntil: 'domcontentloaded', timeout: 30_000 })
    await expect(page.getByRole('heading', { name: 'Delete Account' })).not.toBeVisible({
      timeout: 15_000,
    })
  })
})

// ============================================================================
// Soft deletion: user with recent thesis (under retention)
// ============================================================================

test.describe('Account Deletion - Self-Service (Soft Deletion / Retention)', () => {
  test.use({ storageState: authStatePath('delete_recent_thesis') })
  test.describe.configure({ mode: 'serial' })

  test('account tab shows retention notice with thesis count for user with recent thesis', async ({
    page,
  }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    // Should show data retention notice (not full deletion message)
    await expect(page.getByText('Data Retention Notice', { exact: true })).toBeVisible({
      timeout: 10_000,
    })
    await expect(page.getByText(/legal retention requirements/i)).toBeVisible()

    // Verify thesis count is shown in the retention notice
    await expect(page.getByText(/\d+ thesis record/i)).toBeVisible()

    // Verify a retention date is mentioned
    await expect(page.getByText(/retained until/i).first()).toBeVisible()

    // The main message should mention deactivation, not permanent deletion
    await expect(page.getByText(/deactivated/i).first()).toBeVisible()

    // No research group head warning
    await expect(page.getByText('Research Group Head', { exact: true })).not.toBeVisible({
      timeout: 3_000,
    })

    // Delete button should be enabled (soft deletion is allowed)
    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled()
  })

  test('user with recent thesis can soft-delete their account', async ({ page }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled({ timeout: 10_000 })
    await deleteButton.click()

    // Modal should indicate deactivation (not permanent deletion)
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    await expect(
      dialog.getByText(/deactivated, with full deletion after the retention period/i),
    ).toBeVisible()
    await expect(dialog.getByText(/permanently deleted/i)).not.toBeVisible({ timeout: 2_000 })

    // Type the full name to enable confirmation
    await dialog.getByRole('textbox').fill('RecentThesis Retainable')
    await dialog.getByRole('button', { name: 'Yes, Delete My Account' }).click()

    // Should redirect away from settings after logout
    // After deletion, the app logs out and redirects to the homepage or login page
    await expect(page.getByRole('heading', { name: 'Delete Account' })).not.toBeVisible({
      timeout: 30_000,
    })
  })

  test('soft-deleted user can no longer access protected pages', async ({ page }) => {
    // After soft-deletion the session is invalidated — navigating to a protected
    // page should NOT show the Delete Account section.
    await page.goto('/settings/account', { waitUntil: 'domcontentloaded', timeout: 30_000 })
    await expect(page.getByRole('heading', { name: 'Delete Account' })).not.toBeVisible({
      timeout: 15_000,
    })
  })
})

// ============================================================================
// Full deletion: user with old thesis (retention expired)
// ============================================================================

test.describe('Account Deletion - Self-Service (Expired Retention)', () => {
  test.use({ storageState: authStatePath('delete_old_thesis') })
  test.describe.configure({ mode: 'serial' })

  test('account tab shows full deletion for user with old thesis (retention expired)', async ({
    page,
  }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    // Retention has expired — should show full deletion message
    await expect(page.getByText(/permanently deleted/i)).toBeVisible({ timeout: 10_000 })

    // No retention notice should be shown
    await expect(page.getByText('Data Retention Notice', { exact: true })).not.toBeVisible({
      timeout: 3_000,
    })

    // No research group head warning
    await expect(page.getByText('Research Group Head', { exact: true })).not.toBeVisible({
      timeout: 3_000,
    })

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled()
  })

  test('user with old thesis can fully delete their account', async ({ page }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled({ timeout: 10_000 })
    await deleteButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Modal should indicate permanent deletion
    await expect(dialog.getByText(/permanently deleted/i)).toBeVisible()
    await expect(dialog.getByText(/deactivated/i)).not.toBeVisible({ timeout: 2_000 })

    // Type the full name to enable confirmation
    await dialog.getByRole('textbox').fill('OldThesis Deletable')
    await dialog.getByRole('button', { name: 'Yes, Delete My Account' }).click()

    // After deletion, the app logs out and redirects to the homepage or login page
    await expect(page.getByRole('heading', { name: 'Delete Account' })).not.toBeVisible({
      timeout: 30_000,
    })
  })

  test('deleted user with old thesis can no longer access protected pages', async ({ page }) => {
    // After deletion the session is invalidated — navigating to a protected
    // page should NOT show the Delete Account section.
    await page.goto('/settings/account', { waitUntil: 'domcontentloaded', timeout: 30_000 })
    await expect(page.getByRole('heading', { name: 'Delete Account' })).not.toBeVisible({
      timeout: 15_000,
    })
  })
})

// ============================================================================
// Settings page: Account tab visibility
// ============================================================================

test.describe('Settings - Account Tab', () => {
  test.use({ storageState: authStatePath('student') })

  test('account tab is visible on settings page', async ({ page }) => {
    await navigateTo(page, '/settings')

    await expect(page.getByText('Account')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('My Information')).toBeVisible()
    await expect(page.getByText('Notification Settings')).toBeVisible()
  })

  test('navigating to account tab shows deletion content', async ({ page }) => {
    await navigateTo(page, '/settings/account')

    await expect(page.getByRole('heading', { name: 'Delete Account' })).toBeVisible({
      timeout: 15_000,
    })
    // Verify core UI elements are present
    await expect(page.getByRole('button', { name: 'Delete My Account' })).toBeVisible()
    // A preview message should always be rendered
    await expect(page.locator('text=/deleted|deactivated|transfer/i').first()).toBeVisible({
      timeout: 10_000,
    })
  })
})

// ============================================================================
// Active thesis triggers retention-based soft deletion (not blocking)
// ============================================================================

test.describe('Account Deletion - Active Thesis Shows Retention Notice', () => {
  // student has an active thesis (WRITING state, recent activity) + a FINISHED thesis
  test.use({ storageState: authStatePath('student') })

  test('account tab shows retention notice for user with active thesis', async ({ page }) => {
    await navigateTo(page, '/settings/account')

    await expect(page.getByRole('heading', { name: 'Delete Account' })).toBeVisible({
      timeout: 15_000,
    })

    // Active thesis warning should NOT be visible (removed in state-independent retention)
    await expect(page.getByText('Active Theses', { exact: true })).not.toBeVisible({
      timeout: 3_000,
    })

    // Should show data retention notice (thesis has recent activity)
    await expect(page.getByText('Data Retention Notice', { exact: true })).toBeVisible({
      timeout: 10_000,
    })
    await expect(page.getByText(/legal retention requirements/i)).toBeVisible()

    // Verify thesis count is displayed in the retention notice
    await expect(page.getByText(/\d+ thesis record/i)).toBeVisible()

    // Verify a retention date is mentioned
    await expect(page.getByText(/retained until/i).first()).toBeVisible()

    // Delete button should be ENABLED (active theses no longer block deletion)
    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled()
  })
})

// ============================================================================
// Research group head blocks deletion
// ============================================================================

test.describe('Account Deletion - Research Group Head Blocks', () => {
  // supervisor is head of ASE research group
  test.use({ storageState: authStatePath('supervisor') })

  test('account tab shows research group head warning and disables delete', async ({ page }) => {
    await navigateTo(page, '/settings/account')

    await expect(page.getByRole('heading', { name: 'Delete Account' })).toBeVisible({
      timeout: 15_000,
    })

    // Research group head warning should be visible
    await expect(page.getByText('Research Group Head', { exact: true })).toBeVisible({
      timeout: 10_000,
    })

    // Message should mention transferring leadership
    await expect(page.getByText(/transfer.*leadership/i).first()).toBeVisible()

    // No retention notice or active thesis warning
    await expect(page.getByText('Data Retention Notice', { exact: true })).not.toBeVisible({
      timeout: 3_000,
    })

    // Delete button should be disabled
    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeDisabled()
  })
})

// ============================================================================
// Confirmation dialog safety checks
// ============================================================================

test.describe('Account Deletion - Confirmation Dialog Safety', () => {
  test.use({ storageState: authStatePath('student') })

  test('cancel button closes modal and resets state', async ({ page }) => {
    await navigateTo(page, '/settings/account')

    await expect(page.getByRole('heading', { name: 'Delete Account' })).toBeVisible({
      timeout: 15_000,
    })

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled({ timeout: 10_000 })
    await deleteButton.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Type partial name
    await dialog.getByRole('textbox').fill('Student')
    const confirmButton = dialog.getByRole('button', { name: 'Yes, Delete My Account' })
    await expect(confirmButton).toBeDisabled()

    // Click cancel
    await dialog.getByRole('button', { name: 'Cancel' }).click()
    await expect(dialog).not.toBeVisible({ timeout: 5_000 })

    // Reopen — the text input should be cleared
    await deleteButton.click()
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })
    await expect(page.getByRole('dialog').getByRole('textbox')).toHaveValue('')
  })
})

// ============================================================================
// Admin user deletion
// ============================================================================

test.describe('Account Deletion - Admin Operations', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin page shows user deletion section with search', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByRole('heading', { name: 'User Account Deletion' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Data Retention' })).toBeVisible()
    await expect(page.getByPlaceholder(/Search by name, email, or ID/i)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Search' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Run Cleanup' })).toBeVisible()
  })

  test('admin can search for users and see results', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'User Account Deletion' })).toBeVisible({
      timeout: 30_000,
    })

    const searchInput = page.getByPlaceholder(/Search by name, email, or ID/i)
    await searchInput.fill('Student')
    await page.getByRole('button', { name: 'Search' }).click()

    // Should show multiple student search results
    const results = page.getByRole('button', { name: /Student.*User/i })
    await expect(results.first()).toBeVisible({ timeout: 15_000 })
    const count = await results.count()
    expect(count).toBeGreaterThanOrEqual(1)
  })

  test('admin preview for retention-blocked user shows retention message', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'User Account Deletion' })).toBeVisible({
      timeout: 30_000,
    })

    // Search for student5 (has DROPPED_OUT thesis → retention blocked)
    const searchInput = page.getByPlaceholder(/Search by name, email, or ID/i)
    await searchInput.fill('Student5')
    await page.getByRole('button', { name: 'Search' }).click()

    const userButton = page.getByRole('button', { name: /Student5.*User/i })
    await expect(userButton).toBeVisible({ timeout: 15_000 })
    await userButton.click()

    // Deletion preview should show with user's name
    await expect(page.getByText(/Deletion preview for.*Student5/i)).toBeVisible({
      timeout: 15_000,
    })

    // student5 has a recent DROPPED_OUT thesis — should mention retention
    await expect(page.getByText(/retained until/i)).toBeVisible({ timeout: 5_000 })

    // No "active theses" alert (removed)
    await expect(page.getByText(/has active theses/i)).not.toBeVisible({ timeout: 3_000 })

    // Delete button should be enabled (retention doesn't block, just defers)
    const deleteButton = page.getByRole('button', { name: 'Delete User' })
    await expect(deleteButton).toBeEnabled()
  })

  test('admin preview for user with active thesis shows no active-thesis alert', async ({
    page,
  }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'User Account Deletion' })).toBeVisible({
      timeout: 30_000,
    })

    // Search for student (has active WRITING thesis)
    const searchInput = page.getByPlaceholder(/Search by name, email, or ID/i)
    await searchInput.fill('student')
    await page.getByRole('button', { name: 'Search' }).click()

    const userButton = page.getByRole('button', { name: /Student User/i }).first()
    await expect(userButton).toBeVisible({ timeout: 15_000 })
    await userButton.click()

    // Deletion preview should show
    await expect(page.getByText(/Deletion preview for.*Student/i)).toBeVisible({
      timeout: 15_000,
    })

    // "active theses" alert should NOT appear (removed in state-independent retention)
    await expect(page.getByText(/has active theses/i)).not.toBeVisible({ timeout: 3_000 })

    // Should show retention info (active thesis has recent activity)
    await expect(page.getByText(/retained until/i)).toBeVisible({ timeout: 5_000 })

    // Delete button should be enabled (active theses no longer block admin deletion)
    const deleteButton = page.getByRole('button', { name: 'Delete User' })
    await expect(deleteButton).toBeEnabled()
  })

  test('admin preview for research group head shows alert and disables delete', async ({
    page,
  }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'User Account Deletion' })).toBeVisible({
      timeout: 30_000,
    })

    // Search for supervisor (research group head)
    const searchInput = page.getByPlaceholder(/Search by name, email, or ID/i)
    await searchInput.fill('Supervisor User')
    await page.getByRole('button', { name: 'Search' }).click()

    const userButton = page.getByRole('button', { name: /Supervisor User/i }).first()
    await expect(userButton).toBeVisible({ timeout: 15_000 })
    await userButton.click()

    // Deletion preview should show
    await expect(page.getByText(/Deletion preview for.*Supervisor/i)).toBeVisible({
      timeout: 15_000,
    })

    // Research group head alert should be visible
    await expect(page.getByText(/research group head/i)).toBeVisible({ timeout: 5_000 })

    // Delete button should be disabled for research group heads
    const deleteButton = page.getByRole('button', { name: 'Delete User' })
    await expect(deleteButton).toBeDisabled()
  })
})

// ============================================================================
// Route protection: non-admin cannot access admin page
// ============================================================================

test.describe('Account Deletion - Route Protection', () => {
  test.use({ storageState: authStatePath('advisor') })

  test('advisor cannot access admin page directly', async ({ page }) => {
    await navigateTo(page, '/admin')

    // Should not see the admin page content
    await expect(page.getByRole('heading', { name: 'Administration' })).not.toBeVisible({
      timeout: 10_000,
    })
  })
})
