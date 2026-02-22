import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, selectOption } from './helpers'

// Thesis d000-0003 is in SUBMITTED state, assigned to student3, has abstract text set
const THESIS_ID = '00000000-0000-4000-d000-000000000003'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'Online Anomaly Detection in IoT Sensor Streams'

test.describe('Presentation Workflow - Student creates a presentation draft', () => {
  test.use({ storageState: authStatePath('student3') })

  test('student can create a presentation draft for their thesis', async ({ page }) => {
    await navigateTo(page, THESIS_URL)

    // Wait for thesis page to load
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
      timeout: 15_000,
    })

    // Find the Presentation accordion section and ensure it is visible
    const presentationControl = page.getByRole('button', { name: 'Presentation', exact: true })
    await expect(presentationControl).toBeVisible({ timeout: 10_000 })

    // Click to expand if collapsed
    if ((await presentationControl.getAttribute('aria-expanded')) !== 'true') {
      await presentationControl.click()
    }

    // Click "Create Presentation Draft" button
    await page.getByRole('button', { name: 'Create Presentation Draft' }).click()

    // Modal should open - use first() since the date picker also opens a dialog
    const modal = page.getByRole('dialog').first()
    await expect(modal).toBeVisible({ timeout: 5_000 })

    // The thesis has an abstract, so there should be a blue notice (not red error)
    await expect(
      modal.getByText('Please make sure that the thesis title'),
    ).toBeVisible()

    // Fill in the presentation form
    // Presentation Type - default is INTERMEDIATE, select FINAL
    await selectOption(page, 'Presentation Type', /final/i)

    // Visibility - default is PUBLIC, keep it

    // Location
    await page.getByLabel('Location').fill('Room 01.07.014, Garching Campus')

    // Language
    await selectOption(page, 'Language', /english/i)

    // Scheduled At - click button to open the DateTimePicker calendar dialog
    await page.getByRole('button', { name: 'Scheduled At' }).click()

    // The DateTimePicker opens a second dialog with a calendar
    const datePickerDialog = page.getByRole('dialog').nth(1)
    await expect(datePickerDialog).toBeVisible({ timeout: 5_000 })

    // Select a future date from the calendar (e.g. 25th of the current month)
    await datePickerDialog.getByRole('button', { name: /25 February 2026/i }).click()

    // Set the time using the spinbuttons (hours and minutes)
    const hourSpinbutton = datePickerDialog.getByRole('spinbutton').first()
    const minuteSpinbutton = datePickerDialog.getByRole('spinbutton').nth(1)
    await hourSpinbutton.click()
    await hourSpinbutton.fill('14')
    await minuteSpinbutton.click()
    await minuteSpinbutton.fill('00')

    // Click the submit/checkmark button to confirm date/time selection
    // It's the last button in the date picker dialog (after the time inputs)
    await datePickerDialog.locator('button').last().click()

    // Click "Create Presentation Draft" button in the modal
    const createButton = modal.getByRole('button', { name: 'Create Presentation Draft' })
    await expect(createButton).toBeEnabled({ timeout: 10_000 })
    await createButton.click()

    // Modal should close after successful creation
    await expect(modal).not.toBeVisible({ timeout: 15_000 })
  })
})
