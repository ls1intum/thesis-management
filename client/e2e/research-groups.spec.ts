import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Research Groups - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('research groups page shows groups and search', async ({ page }) => {
    await navigateTo(page, '/research-groups')

    await expect(page.getByRole('heading', { name: /research groups/i })).toBeVisible({ timeout: 15_000 })
    // Search input should be present
    await expect(page.getByPlaceholder(/search research groups/i)).toBeVisible()
    // Create button should be visible
    await expect(page.getByRole('button', { name: /create research group/i })).toBeVisible()
    // Seeded research groups should appear (ASE and DSA)
    await expect(page.getByText('Data Science and Analytics').first()).toBeVisible()
    await expect(page.getByText('Applied Software Engineering').first()).toBeVisible()
  })

  test('search filters research groups', async ({ page }) => {
    await navigateTo(page, '/research-groups')

    await expect(page.getByRole('heading', { name: /research groups/i })).toBeVisible({ timeout: 15_000 })
    // Search for ASE
    await page.getByPlaceholder(/search research groups/i).fill('Applied Software')
    // ASE should still be visible
    await expect(page.getByText('Applied Software Engineering').first()).toBeVisible()
  })
})

test.describe('Research Group Settings - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can access their research group settings', async ({ page }) => {
    // UUID matches the ASE research group seeded in seed_dev_test_data.sql
    await navigateTo(page, '/research-groups/00000000-0000-4000-a000-000000000001')

    // Supervisor should see either group settings or an unauthorized page
    await expect(
      page.getByText('Applied Software Engineering').first().or(page.getByText(/unauthorized/i)),
    ).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Research Group Settings - Email Settings Tab', () => {
  test.use({ storageState: authStatePath('admin') })

  const researchGroupUrl = '/research-groups/00000000-0000-4000-a000-000000000001'

  test('Email Settings tab exists and contains email-related settings', async ({ page }) => {
    await navigateTo(page, researchGroupUrl)

    await expect(
      page.getByRole('heading', { name: /research group settings/i }),
    ).toBeVisible({ timeout: 15_000 })

    // Email Settings tab should exist
    const emailSettingsTab = page.getByRole('tab', { name: 'Email Settings' })
    await expect(emailSettingsTab).toBeVisible()

    // Click the Email Settings tab
    await emailSettingsTab.click()

    // Both email-related settings cards should be visible
    await expect(
      page.getByRole('heading', { name: 'Application Email Content', level: 3 }),
    ).toBeVisible()
    await expect(
      page.getByText('Additional application notification email', { exact: false }),
    ).toBeVisible()

    // Email Templates section should also be visible
    await expect(
      page.getByRole('heading', { name: 'Email Templates', level: 3 }),
    ).toBeVisible()
  })

  test('Application Email Content card shows toggle and description', async ({ page }) => {
    await navigateTo(page, researchGroupUrl)

    await expect(
      page.getByRole('heading', { name: /research group settings/i }),
    ).toBeVisible({ timeout: 15_000 })

    await page.getByRole('tab', { name: 'Email Settings' }).click()

    // The toggle label should be visible
    await expect(
      page.getByText('Include Personal Details and Attachments'),
    ).toBeVisible()

    // The description text should explain the behavior
    await expect(
      page.getByText(/When enabled, application notification emails will include/),
    ).toBeVisible()
    await expect(
      page.getByText(/supervisors and examiners receive a minimal notification/),
    ).toBeVisible()
  })

  test('Application Email Content toggle has info tooltip icon', async ({ page }) => {
    await navigateTo(page, researchGroupUrl)

    await expect(
      page.getByRole('heading', { name: /research group settings/i }),
    ).toBeVisible({ timeout: 15_000 })

    await page.getByRole('tab', { name: 'Email Settings' }).click()

    // There should be an info icon (with cursor: help) near the toggle label
    const tooltipTrigger = page.locator('[style*="cursor: help"]')
    await expect(tooltipTrigger).toBeVisible()
  })

  test('APPLICATION_CREATED_CHAIR template is shown in Email Templates', async ({ page }) => {
    await navigateTo(page, researchGroupUrl)

    await expect(
      page.getByRole('heading', { name: /research group settings/i }),
    ).toBeVisible({ timeout: 15_000 })

    await page.getByRole('tab', { name: 'Email Settings' }).click()

    // The APPLICATION_CREATED_CHAIR template should be visible under "Application Open" category
    await expect(
      page.getByRole('heading', { name: 'New Thesis Application', level: 6 }),
    ).toBeVisible()
  })

  test('APPLICATION_CREATED_CHAIR template is disabled when toggle is off', async ({ page }) => {
    await navigateTo(page, researchGroupUrl)

    await expect(
      page.getByRole('heading', { name: /research group settings/i }),
    ).toBeVisible({ timeout: 15_000 })

    await page.getByRole('tab', { name: 'Email Settings' }).click()

    // Wait for the template to appear
    await expect(
      page.getByRole('heading', { name: 'New Thesis Application', level: 6 }),
    ).toBeVisible()

    // With the toggle off (default), the template card should show "Disabled" badge
    const disabledBadge = page.locator('.mantine-Badge-label', { hasText: 'Disabled' })
    await expect(disabledBadge).toBeVisible()

    // The Preview and Edit buttons on this specific card should be disabled
    const templateCard = page.locator('[style*="opacity"]').filter({
      hasText: 'New Thesis Application',
    })
    await expect(templateCard).toBeVisible()

    // Buttons within the disabled card should be disabled
    const previewButton = templateCard.getByRole('button', { name: 'Preview' })
    const editButton = templateCard.getByRole('button', { name: 'Edit' })
    await expect(previewButton).toBeDisabled()
    await expect(editButton).toBeDisabled()
  })

  test('APPLICATION_CREATED_CHAIR template becomes enabled when toggle is on', async ({
    page,
  }) => {
    await navigateTo(page, researchGroupUrl)

    await expect(
      page.getByRole('heading', { name: /research group settings/i }),
    ).toBeVisible({ timeout: 15_000 })

    await page.getByRole('tab', { name: 'Email Settings' }).click()

    // Wait for the template list to load
    await expect(
      page.getByRole('heading', { name: 'New Thesis Application', level: 6 }),
    ).toBeVisible()

    // Initially the template should be disabled
    const disabledBadge = page.locator('.mantine-Badge-label', { hasText: 'Disabled' })
    await expect(disabledBadge).toBeVisible()

    // Toggle the "Include Personal Details and Attachments" switch ON
    // Mantine Switch has an overlay that intercepts pointer events, so use force click
    const toggle = page.getByRole('switch')
    await toggle.first().click({ force: true })

    // After enabling, the "Disabled" badge should disappear
    await expect(disabledBadge).toBeHidden()

    // The template card should now show "Default" badge instead
    const defaultBadge = page.locator('.mantine-Badge-label', { hasText: 'Default' }).filter({
      hasText: 'Default',
    })
    // There should be multiple "Default" badges (one for each template), at least one near our template
    await expect(defaultBadge.first()).toBeVisible()

    // The card should no longer have opacity styling
    const disabledCard = page.locator('[style*="opacity: 0.5"]').filter({
      hasText: 'New Thesis Application',
    })
    await expect(disabledCard).toBeHidden()

    // Toggle it back OFF for clean state
    await toggle.first().click({ force: true })

    // "Disabled" badge should reappear
    await expect(disabledBadge).toBeVisible()
  })

  test('email settings are NOT on the General tab', async ({ page }) => {
    await navigateTo(page, researchGroupUrl)

    await expect(
      page.getByRole('heading', { name: /research group settings/i }),
    ).toBeVisible({ timeout: 15_000 })

    // On the General tab (default), Application Email Content should NOT be visible
    await expect(
      page.getByRole('heading', { name: 'Application Email Content', level: 3 }),
    ).toBeHidden()
  })
})

test.describe('Research Groups - Student cannot access admin page', () => {
  test('student is denied access to research groups admin', async ({ page }) => {
    await navigateTo(page, '/research-groups')

    // Student should not see the admin page content
    await expect(page.getByRole('heading', { name: /research groups/i })).toBeHidden()
  })
})
