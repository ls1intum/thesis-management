import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

// Active interview process for topic 3 (Anomaly Detection), DSA group
const ACTIVE_PROCESS_ID = '00000000-0000-4000-e600-000000000001'
const BOOKING_URL = `/interview_booking/${ACTIVE_PROCESS_ID}`

test.describe('Interview Slot Booking - Full Flow', () => {
  // student2 has an INTERVIEWING application and interviewee record for process 1
  // but NO booked slot — ready to book and then cancel
  test.use({ storageState: authStatePath('student2') })
  test.describe.configure({ mode: 'serial' })

  test('student books an available interview slot', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, BOOKING_URL)

    // Should show "Select your Interview Slot" heading (student2 has no booking)
    await expect(
      page.getByRole('heading', { name: 'Select your Interview Slot' }),
    ).toBeVisible({ timeout: 30_000 })

    // Verify Summary heading visible
    await expect(page.getByRole('heading', { name: 'Summary' })).toBeVisible()

    // Verify Thesis Topic card shows "Anomaly Detection"
    await expect(page.getByText('Thesis Topic')).toBeVisible()
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible()

    // Verify "Reserve Interview Slot" button is disabled (no slot selected)
    const reserveButton = page.getByRole('button', { name: 'Reserve Interview Slot' })
    await reserveButton.scrollIntoViewIfNeeded()
    await expect(reserveButton).toBeDisabled()

    // Click on an available slot — slots are rendered as clickable cards with time headings
    // Each slot shows a time range like "03:51 PM - 04:36 PM" as an h6 heading
    const slotTimeHeading = page.getByRole('heading', { level: 6 }).first()
    await expect(slotTimeHeading).toBeVisible({ timeout: 10_000 })
    await slotTimeHeading.click()

    // After clicking a slot, verify the summary updates
    // Wait for the reserve button to become enabled (indicates slot was selected)
    await reserveButton.scrollIntoViewIfNeeded()
    await expect(reserveButton).toBeEnabled({ timeout: 10_000 })

    // Click "Reserve Interview Slot"
    await reserveButton.click()

    // Verify page transitions to "Interview Scheduled" heading
    await expect(
      page.getByRole('heading', { name: 'Interview Scheduled' }),
    ).toBeVisible({ timeout: 30_000 })

    // Verify confirmation text
    await expect(page.getByText('You have booked an interview slot.')).toBeVisible()

    // Verify topic info card
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible()
    await expect(page.getByText('Data Science and Analytics').first()).toBeVisible()

    // Verify "Selected Interview" card shows details
    await expect(page.getByText('Selected Interview')).toBeVisible()
    await expect(page.getByText('Date')).toBeVisible()
    await expect(page.getByText('Location')).toBeVisible()

    // Scroll down to see cancel button
    const cancelButton = page.getByRole('button', { name: 'Cancel Interview' })
    await cancelButton.scrollIntoViewIfNeeded()
    await expect(cancelButton).toBeVisible()
  })

  test('student cancels booked slot and returns to selection', async ({ page }) => {
    test.setTimeout(90_000)

    await navigateTo(page, BOOKING_URL)

    // Should show "Interview Scheduled" (student2 just booked in previous test)
    await expect(
      page.getByRole('heading', { name: 'Interview Scheduled' }),
    ).toBeVisible({ timeout: 30_000 })

    // Click "Cancel Interview"
    const cancelButton = page.getByRole('button', { name: 'Cancel Interview' })
    await cancelButton.scrollIntoViewIfNeeded()
    await cancelButton.click()

    // Verify confirmation modal opens
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })

    // Verify modal title
    await expect(dialog.getByText('Confirm Cancellation')).toBeVisible()

    // Verify modal text
    await expect(
      dialog.getByText(/Are you sure you want to cancel this interview slot booking/i),
    ).toBeVisible()

    // Verify "Booked Slot" section shows slot details
    await expect(dialog.getByText('Booked Slot')).toBeVisible()

    // Click the "Cancel Interview" button (red) in the modal
    const confirmCancelButton = dialog.getByRole('button', { name: 'Cancel Interview' })
    await confirmCancelButton.click()

    // Verify modal closes
    await expect(dialog).toBeHidden({ timeout: 10_000 })

    // Verify page transitions back to "Select your Interview Slot"
    await expect(
      page.getByRole('heading', { name: 'Select your Interview Slot' }),
    ).toBeVisible({ timeout: 30_000 })

    // Verify available slots are shown (topic info visible)
    await expect(page.getByText('Thesis Topic')).toBeVisible()

    // Verify "Reserve Interview Slot" button is disabled (no slot selected yet)
    const reserveButton = page.getByRole('button', { name: 'Reserve Interview Slot' })
    await reserveButton.scrollIntoViewIfNeeded()
    await expect(reserveButton).toBeDisabled()
  })
})
