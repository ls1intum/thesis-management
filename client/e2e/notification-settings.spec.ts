import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Notification Settings - Examiner toggle preferences', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner can toggle notification preferences', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, '/settings/notifications')
    await expect(page.getByText('Notification Settings')).toBeVisible({ timeout: 15_000 })

    // Verify all notification option headings are visible
    await expect(page.getByText('New Applications')).toBeVisible()
    await expect(page.getByText('Application Review Reminder')).toBeVisible()
    await expect(page.getByText('Presentation Invitations')).toBeVisible()
    await expect(page.getByText('Thesis Comments')).toBeVisible()

    // Verify descriptions are shown
    await expect(page.getByText(/summary email on every new application/i)).toBeVisible()
    await expect(page.getByText(/weekly reminder email/i)).toBeVisible()
    await expect(page.getByText(/invitations to public thesis presentations/i)).toBeVisible()
    await expect(page.getByText(/email for every comment/i)).toBeVisible()

    // New Applications uses a Select dropdown (not a toggle)
    // Examiner has 'all' setting — should show "All topics" or similar
    const selectInput = page.locator('.mantine-Select-input').first()
    await expect(selectInput).toBeVisible()

    // Toggle the Presentation Invitations switch
    // Find all switches on the page — they correspond to: Application Review Reminder, Include Suggested Topics, Presentation Invitations, Thesis Comments
    const switches = page.locator('input[type="checkbox"]')
    const switchCount = await switches.count()
    expect(switchCount).toBeGreaterThanOrEqual(3)

    // Toggle Presentation Invitations and verify the switch state changes
    const presentationSwitch = page
      .getByText('Presentation Invitations')
      .locator('xpath=ancestor::div[contains(@class, "mantine-Group")]')
      .locator('input[type="checkbox"]')
    await expect(presentationSwitch).toBeVisible()

    const wasChecked = await presentationSwitch.isChecked()
    await presentationSwitch.click({ force: true })

    // Verify the toggle state changed (Playwright auto-retries until timeout)
    if (wasChecked) {
      await expect(presentationSwitch).not.toBeChecked({ timeout: 10_000 })
    } else {
      await expect(presentationSwitch).toBeChecked({ timeout: 10_000 })
    }

    // Toggle it back to restore original state
    await presentationSwitch.click({ force: true })

    // Verify it's restored
    if (wasChecked) {
      await expect(presentationSwitch).toBeChecked({ timeout: 10_000 })
    } else {
      await expect(presentationSwitch).not.toBeChecked({ timeout: 10_000 })
    }
  })

  test('examiner notification settings show per-thesis notification toggles', async ({ page }) => {
    await navigateTo(page, '/settings/notifications')
    await expect(page.getByText('Notification Settings')).toBeVisible({ timeout: 15_000 })

    // Should show a table of theses with notification toggles
    // Examiner is assigned to theses — should see thesis titles
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Notification Settings - Student preferences', () => {
  test.use({ storageState: authStatePath('student') })

  test('student sees limited notification options', async ({ page }) => {
    await navigateTo(page, '/settings/notifications')
    await expect(page.getByText('Notification Settings')).toBeVisible({ timeout: 15_000 })

    // Student should see Presentation Invitations and Thesis Comments
    await expect(page.getByText('Presentation Invitations')).toBeVisible()
    await expect(page.getByText('Thesis Comments')).toBeVisible()

    // Student should NOT see management-only notification options
    await expect(page.getByText('New Applications')).toBeHidden()
    await expect(page.getByText('Application Review Reminder')).toBeHidden()

    // Student has theses — should see per-thesis toggles
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
  })
})
