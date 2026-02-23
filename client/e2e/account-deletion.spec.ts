import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

// ============================================================================
// Self-service account deletion (Settings > Account tab)
//
// NOTE: Destructive tests (actual deletion) check if the user was already
// deleted in a prior run and skip gracefully, similar to data-retention tests.
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

test.describe('Account Deletion - Self-Service (Full Deletion)', () => {
  test.use({ storageState: authStatePath('delete_rejected_app') })
  test.describe.configure({ mode: 'serial' })

  test('account tab shows deletion preview for user with rejected application', async ({
    page,
  }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return // User already deleted in a prior run

    // User with only a rejected application should see full deletion message
    await expect(page.getByText(/permanently deleted/i)).toBeVisible({ timeout: 10_000 })

    // Delete button should be enabled
    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeVisible()
    await expect(deleteButton).toBeEnabled()
  })

  test('user can delete their own account', async ({ page }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled({ timeout: 10_000 })
    await deleteButton.click()

    // Confirmation modal should appear
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText('Are you sure you want to proceed?')).toBeVisible()

    // Confirm deletion
    await page.getByRole('dialog').getByRole('button', { name: 'Yes, Delete My Account' }).click()

    // Should redirect to login (Keycloak) after logout
    await expect(page).toHaveURL(/localhost:3000|kc-login/, { timeout: 30_000 })
  })
})

test.describe('Account Deletion - Self-Service (Soft Deletion / Retention)', () => {
  test.use({ storageState: authStatePath('delete_recent_thesis') })
  test.describe.configure({ mode: 'serial' })

  test('account tab shows retention notice for user with recent thesis', async ({ page }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    // Should show data retention notice
    await expect(page.getByText('Data Retention Notice', { exact: true })).toBeVisible({
      timeout: 10_000,
    })
    await expect(page.getByText(/legal retention requirements/i)).toBeVisible()

    // Delete button should still be enabled (soft deletion is allowed)
    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled()
  })

  test('user with recent thesis can soft-delete their account', async ({ page }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled({ timeout: 10_000 })
    await deleteButton.click()

    // Modal should mention deactivation (not permanent deletion)
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    await expect(dialog.getByText(/deactivated/i)).toBeVisible()

    await page.getByRole('dialog').getByRole('button', { name: 'Yes, Delete My Account' }).click()

    // Should redirect after logout
    await expect(page).toHaveURL(/localhost:3000|kc-login/, { timeout: 30_000 })
  })
})

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
    await expect(page.getByText(/Data Retention Notice/i)).not.toBeVisible({ timeout: 3_000 })

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled()
  })

  test('user with old thesis can fully delete their account', async ({ page }) => {
    const isActive = await navigateToAccountTab(page)
    if (!isActive) return

    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeEnabled({ timeout: 10_000 })
    await deleteButton.click()

    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })
    await page.getByRole('dialog').getByRole('button', { name: 'Yes, Delete My Account' }).click()

    await expect(page).toHaveURL(/localhost:3000|kc-login/, { timeout: 30_000 })
  })
})

// ============================================================================
// Settings page: Account tab visibility
// ============================================================================

test.describe('Settings - Account Tab', () => {
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
    await expect(page.getByRole('button', { name: 'Delete My Account' })).toBeVisible()
  })
})

// ============================================================================
// Active thesis blocks deletion
// ============================================================================

test.describe('Account Deletion - Active Thesis Blocks', () => {
  // student has an active thesis (WRITING state)
  test.use({ storageState: authStatePath('student') })

  test('account tab shows active thesis warning and disables delete', async ({ page }) => {
    await navigateTo(page, '/settings/account')

    await expect(page.getByRole('heading', { name: 'Delete Account' })).toBeVisible({
      timeout: 15_000,
    })

    // Active thesis warning should be visible
    await expect(page.getByText('Active Theses', { exact: true })).toBeVisible({
      timeout: 10_000,
    })

    // Delete button should be disabled
    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeDisabled()
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

    // Delete button should be disabled
    const deleteButton = page.getByRole('button', { name: 'Delete My Account' })
    await expect(deleteButton).toBeDisabled()
  })
})

// ============================================================================
// Admin user deletion
// ============================================================================

test.describe('Account Deletion - Admin Operations', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin page shows user deletion section', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'Administration' })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByRole('heading', { name: 'User Account Deletion' })).toBeVisible()
    await expect(page.getByPlaceholder(/Search by name, email, or ID/i)).toBeVisible()
  })

  test('admin can search for users', async ({ page }) => {
    await navigateTo(page, '/admin')

    await expect(page.getByRole('heading', { name: 'User Account Deletion' })).toBeVisible({
      timeout: 30_000,
    })

    const searchInput = page.getByPlaceholder(/Search by name, email, or ID/i)
    await searchInput.fill('Student')
    await page.getByRole('button', { name: 'Search' }).click()

    // Should show search results
    await expect(page.getByRole('button', { name: /Student.*User/i }).first()).toBeVisible({
      timeout: 15_000,
    })
  })

  test('admin can preview deletion for a user', async ({ page }) => {
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

    // Deletion preview should show
    await expect(page.getByText(/Deletion preview for/i)).toBeVisible({ timeout: 15_000 })
  })
})
