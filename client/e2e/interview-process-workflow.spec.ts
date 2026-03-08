import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const INTERVIEW_PROCESS_ID = '00000000-0000-4000-e600-000000000001' // Topic 3, DSA group, active

test.describe('Interview Process Workflow - Examiner Creates Process', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner can create interview process', async ({ page }) => {
    test.setTimeout(120_000)

    await navigateTo(page, '/interviews')
    await expect(page.getByRole('heading', { name: 'Interviews', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    // Verify the interviews page has expected sections
    await expect(page.getByText(/interview topics/i)).toBeVisible()

    // Click "New Interview Process" button
    await page.getByRole('button', { name: /New Interview Process/i }).click()

    // Verify the creation dialog opens
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    await expect(
      dialog.locator('.mantine-Modal-title').filter({ hasText: /^Create Interview Process$/ }),
    ).toBeVisible()

    // Search for Topic 9
    const searchInput = dialog.getByPlaceholder('Select topic...')
    await expect(searchInput).toBeVisible({ timeout: 10_000 })
    await searchInput.fill('E2E Gap5')

    // Wait for the filtered topic to appear and click it
    const topicItem = dialog.getByText('E2E Gap5: Interview Topic')
    await expect(topicItem).toBeVisible({ timeout: 15_000 })
    await topicItem.click()

    // Verify topic was selected — "Change" button appears
    await expect(dialog.getByRole('button', { name: 'Change' })).toBeVisible({ timeout: 5_000 })

    // Wait for applicants to load
    await expect(dialog.getByText(/Student4/i).first()).toBeVisible({ timeout: 15_000 })
    await expect(dialog.getByText(/Student5/i).first()).toBeVisible({ timeout: 10_000 })

    // Select all applicants
    await dialog.getByRole('button', { name: 'Select All' }).click()

    // Verify selection count
    await expect(dialog.getByText('2 selected')).toBeVisible({ timeout: 5_000 })

    // Click "Create Interview Process"
    await dialog.getByRole('button', { name: 'Create Interview Process' }).click()

    // Verify success notification with exact text
    await expect(page.getByText('Interview process created successfully.')).toBeVisible({
      timeout: 15_000,
    })

    // Verify modal closes
    await expect(dialog).not.toBeVisible({ timeout: 5_000 })
  })
})

test.describe('Interview Process Workflow - Student Views Booking', () => {
  test('student can view interview booking page', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student4') })
    const page = await context.newPage()

    try {
      await navigateTo(page, `/interview_booking/${INTERVIEW_PROCESS_ID}`)

      // student4 has a booked slot — verify booking confirmation heading
      await expect(page.getByRole('heading', { name: 'Interview Scheduled' })).toBeVisible({
        timeout: 30_000,
      })

      // Verify booking confirmation text
      await expect(page.getByText('You have booked an interview slot.')).toBeVisible()

      // Verify thesis topic information is displayed
      await expect(page.getByText('Thesis Topic')).toBeVisible()

      // Verify the "Cancel Interview" button is visible
      await expect(page.getByRole('button', { name: 'Cancel Interview' })).toBeVisible()
    } finally {
      await context.close()
    }
  })
})
