import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Research Group Management - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin can create a research group', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, '/research-groups')
    await expect(page.getByRole('heading', { name: /research groups/i })).toBeVisible({
      timeout: 15_000,
    })

    // Verify existing groups are visible
    await expect(page.getByText('Applied Software Engineering').first()).toBeVisible()
    await expect(page.getByText('Data Science and Analytics').first()).toBeVisible()

    // Click the "Create Research Group" button
    await page.getByRole('button', { name: /create research group/i }).click()

    // Verify modal appears
    const modal = page.locator('.mantine-Modal-content')
    await expect(modal).toBeVisible({ timeout: 5_000 })

    // Fill in the form fields
    await modal.getByRole('textbox', { name: 'Name' }).fill('E2E Test Group')
    await modal.getByRole('textbox', { name: 'Abbreviation' }).fill('E2E')

    // Select a Group Head
    const groupHeadInput = modal.getByRole('textbox', { name: 'Group Head' })
    await groupHeadInput.fill('admin')
    const autocompleteOption = page.locator('.mantine-Autocomplete-option').first()
    await expect(autocompleteOption).toBeVisible({ timeout: 10_000 })
    await autocompleteOption.click()

    // Submit the form
    await modal.getByRole('button', { name: /create research group/i }).click()

    // Verify success notification with exact text
    await expect(page.getByText('Research group created.')).toBeVisible({ timeout: 10_000 })

    // Verify modal closes
    await expect(modal).toBeHidden({ timeout: 5_000 })

    // Verify the new group appears in the list
    await expect(page.getByText('E2E Test Group').first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText('E2E').first()).toBeVisible()
  })
})

test.describe('Research Group Management - Admin Settings', () => {
  test.use({ storageState: authStatePath('admin') })

  const aseSettingsUrl = '/research-groups/00000000-0000-4000-a000-000000000001'

  test('admin can view and edit group settings', async ({ page }) => {
    await navigateTo(page, aseSettingsUrl)

    // Verify page heading
    await expect(page.getByRole('heading', { name: /research group settings/i })).toBeVisible({
      timeout: 15_000,
    })

    // Verify General tab is active by default
    const generalTab = page.getByRole('tab', { name: 'General' })
    await expect(generalTab).toBeVisible()
    await expect(generalTab).toHaveAttribute('aria-selected', 'true')

    // Verify all three tabs exist
    await expect(page.getByRole('tab', { name: 'Members' })).toBeVisible()
    await expect(page.getByRole('tab', { name: 'Email Settings' })).toBeVisible()

    // Verify Group Information card
    await expect(page.getByRole('heading', { name: 'Group Information', level: 3 })).toBeVisible()
    await expect(page.getByRole('textbox', { name: 'Name' })).toHaveValue(
      'Applied Software Engineering',
    )

    // Verify settings cards are visible
    await expect(page.getByRole('heading', { name: 'Application Settings', level: 3 })).toBeVisible(
      { timeout: 10_000 },
    )
    await expect(page.getByRole('heading', { name: 'Proposal Settings', level: 3 })).toBeVisible()
    await expect(
      page.getByRole('heading', { name: 'Presentation Settings', level: 3 }),
    ).toBeVisible()

    // Verify switch controls are present in settings cards
    await expect(page.getByRole('switch').first()).toBeVisible()
  })

  test('admin can view group members', async ({ page }) => {
    await navigateTo(page, aseSettingsUrl)
    await expect(page.getByRole('heading', { name: /research group settings/i })).toBeVisible({
      timeout: 15_000,
    })

    // Click the Members tab
    await page.getByRole('tab', { name: 'Members' }).click()

    // Verify the Group Members card heading
    await expect(page.getByRole('heading', { name: 'Group Members', level: 3 })).toBeVisible({
      timeout: 10_000,
    })

    // Verify member list loads with expected seeded members
    const membersTable = page.locator('table')
    await expect(membersTable.getByText('Supervisor User', { exact: true })).toBeVisible({
      timeout: 10_000,
    })
    await expect(membersTable.getByText('Examiner User', { exact: true })).toBeVisible()

    // Verify "Add Member" button is visible
    await expect(page.getByRole('button', { name: /add member/i })).toBeVisible()

    // Verify the search input for filtering members
    await expect(page.getByPlaceholder(/search research group member/i)).toBeVisible()
  })
})
