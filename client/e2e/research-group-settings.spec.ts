import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const ASE_GROUP_ID = '00000000-0000-4000-a000-000000000001'

test.describe('Research Group Settings - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin can view and configure research group settings', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, `/research-groups/${ASE_GROUP_ID}`)

    // Should show the page heading and General tab by default
    await expect(
      page.getByRole('heading', { name: 'Research Group Settings' }),
    ).toBeVisible({ timeout: 30_000 })
    await expect(page.getByText('General').first()).toBeVisible()
    await expect(page.getByText('Members').first()).toBeVisible()
    await expect(page.getByText('Email Settings').first()).toBeVisible()

    // Group Information card should show ASE data from seed
    await expect(page.getByText('Group Information')).toBeVisible({ timeout: 10_000 })
    await expect(page.getByLabel('Name')).toHaveValue('Applied Software Engineering')
    await expect(page.getByLabel('Abbreviation')).toHaveValue('ASE')

    // Wait for settings cards to load (conditionally rendered after API call)
    await expect(page.getByText('Application Settings')).toBeVisible({ timeout: 15_000 })

    // Scroll down to see all settings cards
    const presentationSettingsText = page.getByText('Presentation Settings')
    await presentationSettingsText.scrollIntoViewIfNeeded()

    // Application Settings card
    await expect(page.getByText(/automatic rejection/i).first()).toBeVisible()

    // Proposal Settings card
    await expect(page.getByText('Proposal Settings')).toBeVisible()
    await expect(page.getByText(/Proposal Phase/i).first()).toBeVisible()

    // Presentation Settings card
    await expect(presentationSettingsText).toBeVisible()
    await expect(page.getByText('Presentation Slot Duration')).toBeVisible()
  })

  test('admin can view Members tab', async ({ page }) => {
    await navigateTo(page, `/research-groups/${ASE_GROUP_ID}`)

    await expect(page.getByText('General').first()).toBeVisible({ timeout: 30_000 })

    // Click Members tab
    await page.getByText('Members').first().click()

    // Should show members assigned to ASE group
    await expect(page.getByText(/Examiner User/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Supervisor User/i).first()).toBeVisible()
  })

  test('admin can view Email Settings tab', async ({ page }) => {
    await navigateTo(page, `/research-groups/${ASE_GROUP_ID}`)

    await expect(page.getByText('General').first()).toBeVisible({ timeout: 30_000 })

    // Click Email Settings tab
    await page.getByText('Email Settings').first().click()

    // Should show email template configuration with specific settings
    await expect(page.getByText('Application Email Content').first()).toBeVisible({ timeout: 10_000 })
  })
})
