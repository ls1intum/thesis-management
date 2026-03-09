import { test, expect, Page } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const ASE_GROUP_ID = '00000000-0000-4000-a000-000000000001'
const ASE_SETTINGS_URL = `/research-groups/${ASE_GROUP_ID}`

/**
 * Navigate to ASE settings and wait for content to load.
 */
async function navigateToSettings(page: Page) {
  await navigateTo(page, ASE_SETTINGS_URL)
  await expect(
    page.getByRole('heading', { name: 'Research Group Settings' }),
  ).toBeVisible({ timeout: 30_000 })
}

test.describe('Research Group Settings Editing - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('admin toggles application auto-reject and sees warning alert', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateToSettings(page)

    // Wait for Application Settings card to load
    const appSettingsHeading = page.getByRole('heading', { name: 'Application Settings', level: 3 })
    await expect(appSettingsHeading).toBeVisible({ timeout: 15_000 })
    await appSettingsHeading.scrollIntoViewIfNeeded()

    // Find auto-reject switch: it's the first switch on the page, inside Application Settings
    // Use nth(0) to get the first switch explicitly
    const allSwitchTracks = page.locator('.mantine-Switch-track')
    const autoRejectTrack = allSwitchTracks.nth(0)
    const allSwitchInputs = page.locator('input[role="switch"]')
    const autoRejectInput = allSwitchInputs.nth(0)

    await autoRejectTrack.scrollIntoViewIfNeeded()

    // Ensure switch starts in ON state (restore if needed from prior failed test)
    const isChecked = await autoRejectInput.isChecked()
    if (!isChecked) {
      await autoRejectTrack.click()
      await page.waitForTimeout(500)
    }

    // === Verify ON state ===
    await expect(autoRejectInput).toBeChecked()
    await expect(page.getByText('Disable automatic rejection')).toBeVisible()

    // "Rejection Time Period" section visible
    const rejectionPeriodLabel = page.locator('p').filter({ hasText: /^Rejection Time Period$/ })
    await expect(rejectionPeriodLabel).toBeVisible()

    // NumberInput shows weeks
    const weeksTextbox = page.getByRole('textbox', { name: /weeks/i })
    await expect(weeksTextbox).toBeVisible()

    // Description text visible
    await expect(
      page.getByText(/Automatically reject applications after a specified time period/i).first(),
    ).toBeVisible()

    // === Toggle OFF ===
    await autoRejectTrack.click()
    await page.waitForTimeout(500)

    // Verify switch unchecked
    await expect(autoRejectInput).not.toBeChecked()

    // Verify "Enable automatic rejection" label text
    await expect(page.getByText('Enable automatic rejection')).toBeVisible()

    // Verify warning alert appears
    await expect(page.getByText(/Automatic Rejection Warning/i)).toBeVisible({ timeout: 5_000 })

    // Verify alert text mentions "9:00 AM"
    await expect(page.getByText(/9:00 AM/i)).toBeVisible()

    // Verify "Rejection Time Period" paragraph is hidden
    await expect(rejectionPeriodLabel).toBeHidden()

    // === Toggle back ON (restore) ===
    await autoRejectTrack.click()
    await page.waitForTimeout(500)

    // Verify switch checked
    await expect(autoRejectInput).toBeChecked()

    // Verify "Disable automatic rejection" label
    await expect(page.getByText('Disable automatic rejection')).toBeVisible()

    // Verify warning alert disappears
    await expect(page.getByText(/Automatic Rejection Warning/i)).toBeHidden()

    // Verify "Rejection Time Period" reappears
    await expect(rejectionPeriodLabel).toBeVisible()
  })

  test('admin changes presentation slot duration', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateToSettings(page)

    // Wait for and scroll to "Presentation Settings"
    const presentationSettingsHeading = page.getByRole('heading', {
      name: 'Presentation Settings',
      level: 3,
    })
    await expect(presentationSettingsHeading).toBeVisible({ timeout: 15_000 })
    await presentationSettingsHeading.scrollIntoViewIfNeeded()

    // Verify "Presentation Slot Duration" label
    await expect(page.getByText('Presentation Slot Duration')).toBeVisible()

    // The NumberInput has a textbox with placeholder "Don't enter less than 2 minutes"
    const durationTextbox = page.getByRole('textbox', { name: /minutes/i })
    await expect(durationTextbox).toBeVisible()

    // Clear and type new value (45)
    await durationTextbox.click()
    await durationTextbox.fill('45')

    // Tab out to trigger auto-save, wait for debounce + network
    await page.keyboard.press('Tab')
    await page.waitForTimeout(2000)

    // Reload page and verify value persists (retry navigation once if value didn't save)
    for (let attempt = 0; attempt < 2; attempt++) {
      await navigateToSettings(page)

      const presentationSettingsHeading2 = page.getByRole('heading', {
        name: 'Presentation Settings',
        level: 3,
      })
      await expect(presentationSettingsHeading2).toBeVisible({ timeout: 15_000 })
      await presentationSettingsHeading2.scrollIntoViewIfNeeded()

      const durationTextbox2 = page.getByRole('textbox', { name: /minutes/i })
      const value = await durationTextbox2.inputValue()
      if (value.includes('45')) break
      if (attempt === 0) await page.waitForTimeout(1000)
    }

    const durationTextbox2 = page.getByRole('textbox', { name: /minutes/i })
    await expect(durationTextbox2).toHaveValue(/45/)

    // Restore to 30
    await durationTextbox2.click()
    await durationTextbox2.fill('30')
    await page.keyboard.press('Tab')
    await page.waitForTimeout(1000)
  })

  test('admin toggles proposal phase setting', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateToSettings(page)

    // Wait for and scroll to "Proposal Settings"
    const proposalSettingsHeading = page.getByRole('heading', {
      name: 'Proposal Settings',
      level: 3,
    })
    await expect(proposalSettingsHeading).toBeVisible({ timeout: 15_000 })
    await proposalSettingsHeading.scrollIntoViewIfNeeded()

    // The proposal switch is the second switch on the page (index 1)
    const allSwitchTracks = page.locator('.mantine-Switch-track')
    const proposalTrack = allSwitchTracks.nth(1)
    const allSwitchInputs = page.locator('input[role="switch"]')
    const proposalInput = allSwitchInputs.nth(1)

    await proposalTrack.scrollIntoViewIfNeeded()

    // Ensure switch starts in ON state (restore if needed from prior failed test)
    const isChecked = await proposalInput.isChecked()
    if (!isChecked) {
      await proposalTrack.click()
      await page.waitForTimeout(500)
    }

    // === Verify ON state ===
    await expect(proposalInput).toBeChecked()
    await expect(page.getByText('Disable Proposal Phase')).toBeVisible()

    // Verify description text
    await expect(page.getByText(/Turn off the proposal phase/i).first()).toBeVisible()

    // === Toggle OFF ===
    await proposalTrack.click()
    await page.waitForTimeout(500)

    // Verify switch toggled
    await expect(proposalInput).not.toBeChecked()

    // Verify "Enable Proposal Phase" label
    await expect(page.getByText('Enable Proposal Phase')).toBeVisible()

    // === Toggle back ON (restore) ===
    await proposalTrack.click()
    await page.waitForTimeout(500)

    // Verify "Disable Proposal Phase" label restored
    await expect(proposalInput).toBeChecked()
    await expect(page.getByText('Disable Proposal Phase')).toBeVisible()
  })
})
