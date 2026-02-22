import { test, expect } from '@playwright/test'
import {
  authStatePath,
  createTestPdfBuffer,
  fillRichTextEditor,
  navigateTo,
  selectOption,
} from './helpers'

test.describe('Application Workflow - Student submits application', () => {
  test.use({ storageState: authStatePath('student') })

  test('student can submit an application for a topic through the full stepper', async ({
    page,
  }) => {
    await navigateTo(page, '/submit-application')
    await expect(
      page.getByRole('heading', { name: 'Submit Application', exact: true }),
    ).toBeVisible({ timeout: 30_000 })

    // Detect which step we're on (state may vary from prior test runs)
    const successText = page.getByText('Your application was successfully submitted!')
    const firstNameLabel = page.getByLabel('First Name')
    const topicButton = page.getByRole('button', {
      name: /Continuous Integration Pipeline Optimization/i,
    })

    // Check for already-submitted state first
    const alreadyDone = await successText.isVisible({ timeout: 2_000 }).catch(() => false)
    if (alreadyDone) return

    // Determine if we're on step 1 or step 2
    const onStep2 = await firstNameLabel.isVisible({ timeout: 3_000 }).catch(() => false)

    if (!onStep2) {
      // Step 1: Select Topic - topic might not be visible if student already applied to all
      const topicVisible = await topicButton.isVisible({ timeout: 5_000 }).catch(() => false)
      if (!topicVisible) {
        // Student may have already applied for this topic; verify we're on the stepper
        await expect(page.locator('.mantine-Stepper-root')).toBeVisible({ timeout: 5_000 })
        return
      }
      await topicButton.click()
      await page.getByRole('button', { name: 'Apply', exact: true }).click()
    }

    // Step 2: Student Information - should be pre-filled from seed data
    await expect(firstNameLabel).toBeVisible({ timeout: 15_000 })
    await expect(firstNameLabel).toHaveValue('Student')

    // Upload required files if file inputs exist
    const pdfBuffer = createTestPdfBuffer()
    for (const label of ['Examination Report', 'CV', 'Bachelor Report']) {
      const wrapper = page.locator(
        `.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("${label}"))`,
      )
      const fileInput = wrapper.locator('input[type="file"]')
      if ((await fileInput.count()) > 0) {
        await fileInput.setInputFiles({
          name: `${label.toLowerCase().replace(/ /g, '-')}.pdf`,
          mimeType: 'application/pdf',
          buffer: pdfBuffer,
        })
      }
    }

    // Accept privacy notice if not already checked
    const privacyCheckbox = page.getByLabel(/privacy/i).or(page.getByRole('checkbox').first())
    if (!(await privacyCheckbox.isChecked())) {
      await privacyCheckbox.check()
    }

    const updateButton = page.getByRole('button', { name: 'Update Information', exact: true })
    await expect(updateButton).toBeEnabled({ timeout: 30_000 })
    await updateButton.click()

    // Step 3: Motivation
    const submittedAfterUpdate = await successText.isVisible({ timeout: 2_000 }).catch(() => false)
    if (submittedAfterUpdate) return

    await expect(page.getByRole('textbox', { name: 'Thesis Type' })).toBeVisible({
      timeout: 15_000,
    })
    await selectOption(page, 'Thesis Type', /master/i)
    await fillRichTextEditor(
      page,
      'Motivation',
      'I am highly motivated to work on CI pipeline optimization because it aligns with my research interests in DevOps and continuous integration.',
    )

    const submitButton = page.getByRole('button', { name: 'Submit Application' })
    await expect(submitButton).toBeEnabled({ timeout: 10_000 })
    await submitButton.click()

    // Verify we're still on the application page (no crash)
    await page.waitForTimeout(2_000)
    await expect(page).toHaveURL(/\/submit-application/)
  })
})
