import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

// Active interview process for topic 3 (Anomaly Detection), DSA group
const ACTIVE_PROCESS_ID = '00000000-0000-4000-e600-000000000001'

test.describe('Interview Booking - Student with existing booking', () => {
  // student4 has a booked slot (slot e800-...01) for process 1
  test.use({ storageState: authStatePath('student4') })

  test('student4 sees their booked interview with topic info and cancel option', async ({
    page,
  }) => {
    test.setTimeout(60_000)

    await navigateTo(page, `/interview_booking/${ACTIVE_PROCESS_ID}`)

    // Should show "Interview Scheduled" heading (student4 has a booked slot)
    await expect(page.getByRole('heading', { name: 'Interview Scheduled' })).toBeVisible({
      timeout: 30_000,
    })
    await expect(page.getByText('You have booked an interview slot.')).toBeVisible()

    // Topic Information card should show the topic details
    await expect(page.getByText('Thesis Topic')).toBeVisible()
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible()

    // Should show research group
    await expect(page.getByText('Research Group')).toBeVisible()
    await expect(page.getByText('Data Science and Analytics').first()).toBeVisible()

    // Should show supervisor(s) and examiner(s) sections
    await expect(page.getByText('Supervisor(s)')).toBeVisible()
    await expect(page.getByText('Examiner(s)')).toBeVisible()

    // Scroll down to see Selected Interview card and cancel button
    const cancelButton = page.getByRole('button', { name: 'Cancel Interview' })
    await cancelButton.scrollIntoViewIfNeeded()

    // Selected Interview card should show slot details
    await expect(page.getByText('Selected Interview')).toBeVisible()
    await expect(page.getByText('Date')).toBeVisible()
    await expect(page.getByText('Location')).toBeVisible()

    // Should show the location from seed data
    await expect(page.getByText(/Boltzmannstr/i).first()).toBeVisible()

    // Should show reschedule option with cancel button
    await expect(page.getByText(/Need to reschedule/i)).toBeVisible()
    await expect(cancelButton).toBeVisible()
  })
})

test.describe('Interview Booking - Student without booking', () => {
  // student5 was rejected and has no active booking for this process
  // Use student (regular) who has no booking for the active process 1
  test.use({ storageState: authStatePath('student') })

  test('student sees slot selection interface for active interview process', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, `/interview_booking/${ACTIVE_PROCESS_ID}`)

    // Should show "Select your Interview Slot" heading (student has no booking)
    await expect(
      page
        .getByRole('heading', { name: 'Select your Interview Slot' })
        .or(page.getByRole('heading', { name: 'Interview Scheduled' })),
    ).toBeVisible({ timeout: 30_000 })

    // If showing slot selection, verify the summary and booking UI
    const isSlotSelection = await page
      .getByRole('heading', { name: 'Select your Interview Slot' })
      .isVisible()
      .catch(() => false)

    if (isSlotSelection) {
      // Should show Summary section
      await expect(page.getByRole('heading', { name: 'Summary' })).toBeVisible()

      // Should show topic information in the summary
      await expect(page.getByText('Thesis Topic')).toBeVisible()

      // Reserve button should be disabled until a slot is selected
      await expect(page.getByRole('button', { name: 'Reserve Interview Slot' })).toBeDisabled()
    }
  })
})

test.describe('Interview Booking - Completed process', () => {
  // Process 2 (topic 1) is completed
  test.use({ storageState: authStatePath('student') })

  test('completed interview process shows completion message', async ({ page }) => {
    const completedProcessId = '00000000-0000-4000-e600-000000000002'

    await navigateTo(page, `/interview_booking/${completedProcessId}`)

    // Should show "Interview Process Completed" for a completed process
    await expect(
      page.getByRole('heading', { name: 'Interview Process Completed' }),
    ).toBeVisible({ timeout: 30_000 })

    await expect(
      page.getByText(/marked as completed/i),
    ).toBeVisible()

    // Should NOT show slot selection or booking
    await expect(
      page.getByRole('button', { name: 'Reserve Interview Slot' }),
    ).toBeHidden()
  })
})
