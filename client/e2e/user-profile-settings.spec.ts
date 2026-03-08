import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, createTestPdfBuffer } from './helpers'

test.describe('User Profile Settings - Read Only Fields', () => {
  test('student can see readonly and editable fields', async ({ page }) => {
    await navigateTo(page, '/settings')

    // Verify "My Information" tab is visible and active
    await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })

    // Verify readonly fields have expected values and readonly attribute
    const firstNameInput = page.getByLabel('First Name')
    await expect(firstNameInput).toBeVisible()
    await expect(firstNameInput).toHaveValue('Student')
    await expect(firstNameInput).toHaveAttribute('readonly', '')

    const lastNameInput = page.getByLabel('Last Name')
    await expect(lastNameInput).toBeVisible()
    await expect(lastNameInput).toHaveValue('User')
    await expect(lastNameInput).toHaveAttribute('readonly', '')

    const emailInput = page.getByLabel('Email')
    await expect(emailInput).toBeVisible()
    await expect(emailInput).toHaveValue('student@test.local')
    await expect(emailInput).toHaveAttribute('readonly', '')

    // Verify matriculation number is visible and readonly
    const matriculationInput = page.getByLabel(/Matriculation Number/i)
    await expect(matriculationInput).toBeVisible()
    await expect(matriculationInput).toHaveAttribute('readonly', '')

    // Verify editable fields are visible and interactive
    await expect(page.getByRole('textbox', { name: 'Gender', exact: true })).toBeVisible()
    await expect(page.getByRole('textbox', { name: 'Current Study Degree', exact: true })).toBeVisible()
    await expect(page.getByRole('textbox', { name: 'Study Program', exact: true })).toBeVisible()
    await expect(page.getByLabel('Semester in Current Study Program', { exact: true })).toBeVisible()

    // Verify the privacy consent checkbox exists
    await expect(page.getByRole('checkbox', { name: /privacy/i })).toBeVisible()

    // Verify the "Update Information" button exists
    await expect(page.getByRole('button', { name: 'Update Information' })).toBeVisible()

    // Verify file upload areas exist for students
    await expect(page.getByText('Examination Report')).toBeVisible()
    await expect(page.getByText('CV')).toBeVisible()
  })
})

test.describe('User Profile Settings - Update Profile', () => {
  test('student can update profile fields', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student3') })
    const page = await context.newPage()

    try {
      await navigateTo(page, '/settings')

      // Wait for form to load with student3's data
      await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })
      await expect(page.getByLabel('First Name')).toHaveValue('Student3')
      await expect(page.getByLabel('Last Name')).toHaveValue('User')
      await expect(page.getByLabel('Email')).toHaveValue('student3@test.local')

      // Verify first name is readonly (cannot be changed)
      await expect(page.getByLabel('First Name')).toHaveAttribute('readonly', '')

      // Change Gender to Male
      await page.getByRole('textbox', { name: 'Gender', exact: true }).click()
      await page.getByRole('option', { name: 'Male', exact: true }).click()

      // Change Semester
      const semesterInput = page.getByLabel('Semester in Current Study Program', { exact: true })
      await semesterInput.click()
      const modifier = process.platform === 'darwin' ? 'Meta' : 'Control'
      await page.keyboard.press(`${modifier}+a`)
      await page.keyboard.type('3')

      // Accept privacy notice
      const privacyCheckbox = page.getByRole('checkbox', { name: /privacy/i })
      if (!(await privacyCheckbox.isChecked())) {
        await privacyCheckbox.check()
      }
      await expect(privacyCheckbox).toBeChecked()

      // Submit form
      const updateButton = page.getByRole('button', { name: 'Update Information', exact: true })
      await expect(updateButton).toBeEnabled({ timeout: 10_000 })
      await updateButton.click()

      // Verify success notification with exact text from source code
      await expect(page.getByText('You successfully updated your profile')).toBeVisible({
        timeout: 10_000,
      })
    } finally {
      await context.close()
    }
  })
})

test.describe('User Profile Settings - Document Upload', () => {
  test('student can upload documents', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student3') })
    const page = await context.newPage()

    try {
      await navigateTo(page, '/settings')
      await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })
      await expect(page.getByLabel('First Name')).toBeVisible({ timeout: 10_000 })

      const pdfBuffer = createTestPdfBuffer()

      // Upload Examination Report
      const examReportWrapper = page.locator(
        '.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("Examination Report"))',
      )
      const examAlreadyUploaded = await examReportWrapper
        .locator('iframe, .mantine-Card-root')
        .first()
        .isVisible({ timeout: 2_000 })
        .catch(() => false)
      if (!examAlreadyUploaded) {
        const examFileInput = examReportWrapper.locator('input[type="file"]')
        await examFileInput.waitFor({ state: 'attached', timeout: 15_000 })
        await examFileInput.setInputFiles({
          name: 'test-exam-report.pdf',
          mimeType: 'application/pdf',
          buffer: pdfBuffer,
        })
      }

      // Upload CV
      const cvWrapper = page.locator(
        '.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("CV"))',
      )
      const cvAlreadyUploaded = await cvWrapper
        .locator('iframe, .mantine-Card-root')
        .first()
        .isVisible({ timeout: 2_000 })
        .catch(() => false)
      if (!cvAlreadyUploaded) {
        const cvFileInput = cvWrapper.locator('input[type="file"]')
        await cvFileInput.waitFor({ state: 'attached', timeout: 15_000 })
        await cvFileInput.setInputFiles({
          name: 'test-cv.pdf',
          mimeType: 'application/pdf',
          buffer: pdfBuffer,
        })
      }

      // Accept privacy notice
      const privacyCheckbox = page.getByRole('checkbox', { name: /privacy/i })
      if (!(await privacyCheckbox.isChecked())) {
        await privacyCheckbox.check()
      }
      await expect(privacyCheckbox).toBeChecked()

      // Submit form
      const updateButton = page.getByRole('button', { name: 'Update Information', exact: true })
      await expect(updateButton).toBeEnabled({ timeout: 10_000 })
      await updateButton.click()

      // Verify success notification
      await expect(page.getByText('You successfully updated your profile')).toBeVisible({
        timeout: 10_000,
      })
    } finally {
      await context.close()
    }
  })
})
