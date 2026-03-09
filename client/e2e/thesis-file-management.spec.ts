import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, expandAccordion } from './helpers'

test.describe('Thesis File Management - WRITING thesis', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can view file section with upload controls for WRITING thesis', async ({
    page,
  }) => {
    test.setTimeout(60_000)

    // Thesis 1: WRITING state
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')

    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 30_000,
    })

    // Scroll to the Thesis accordion section (it's below Info which is expanded)
    const thesisAccordion = page.getByRole('button', { name: 'Thesis', exact: true })
    await thesisAccordion.scrollIntoViewIfNeeded()
    await expect(thesisAccordion).toBeVisible({ timeout: 10_000 })

    // The "Thesis" section should contain "Files" sub-section
    await expect(page.getByText('Files').first()).toBeVisible({ timeout: 10_000 })

    // File upload controls should be visible — "Upload Thesis" button for supervisor
    const uploadButton = page.getByRole('button', { name: 'Upload Thesis' })
    await uploadButton.scrollIntoViewIfNeeded()
    await expect(uploadButton).toBeVisible({ timeout: 10_000 })

    // Download button should be visible
    await expect(page.getByRole('button', { name: 'Download' }).first()).toBeVisible()

    // File types table should show required file types
    await expect(page.getByText(/Presentation \(PDF\)/i).first()).toBeVisible()

    // File Upload History section should be visible (scoped to Files accordion)
    const filesSection = page.getByLabel('Files')
    const historyText = filesSection.getByText('File Upload History')
    await historyText.scrollIntoViewIfNeeded()
    await expect(historyText).toBeVisible()

    // Should show at least one file in history (thesis draft uploaded by student in seed data)
    await expect(page.getByText(/Thesis v1/i).first()).toBeVisible({ timeout: 10_000 })

    // Mark Submission as Final button should be present
    const submitButton = page.getByRole('button', { name: 'Mark Submission as Final' })
    await submitButton.scrollIntoViewIfNeeded()
    await expect(submitButton).toBeVisible()
  })
})

test.describe('Thesis File Management - Student view', () => {
  test('student can view file section with upload controls for their WRITING thesis', async ({
    page,
  }) => {
    test.setTimeout(60_000)

    // Thesis 1: WRITING state, student is assigned
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')

    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 30_000,
    })

    // Scroll to the Thesis accordion section
    const thesisAccordion = page.getByRole('button', { name: 'Thesis', exact: true })
    await thesisAccordion.scrollIntoViewIfNeeded()
    await expect(thesisAccordion).toBeVisible({ timeout: 10_000 })

    // Files section should be visible
    await expect(page.getByText('Files').first()).toBeVisible({ timeout: 10_000 })

    // Student should see Upload Thesis button
    const uploadButton = page.getByRole('button', { name: 'Upload Thesis' })
    await uploadButton.scrollIntoViewIfNeeded()
    await expect(uploadButton).toBeVisible({ timeout: 10_000 })

    // File Upload History should be visible for student (scoped to Files accordion)
    const filesSection = page.getByLabel('Files')
    const historyText = filesSection.getByText('File Upload History')
    await historyText.scrollIntoViewIfNeeded()
    await expect(historyText).toBeVisible()

    // Comments section should be accessible
    const commentsButton = page.getByRole('button', { name: 'Comments' })
    await commentsButton.scrollIntoViewIfNeeded()
    await expect(commentsButton).toBeVisible()

    // Mark Submission as Final button should be visible for student in WRITING state
    const submitButton = page.getByRole('button', { name: 'Mark Submission as Final' })
    await submitButton.scrollIntoViewIfNeeded()
    await expect(submitButton).toBeVisible()
  })
})

test.describe('Thesis File Management - Non-WRITING thesis', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('PROPOSAL thesis does not show file upload section', async ({ page }) => {
    // Thesis 2: PROPOSAL state
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000002')

    await expect(page.getByRole('heading', { name: /CI Pipeline Optimization/i })).toBeVisible({
      timeout: 30_000,
    })

    // PROPOSAL state should NOT show the Thesis/Files section (only shows for WRITING+)
    await expect(page.getByRole('button', { name: 'Upload Thesis' })).toBeHidden({ timeout: 5_000 })
    await expect(
      page.getByRole('button', { name: 'Mark Submission as Final' }),
    ).toBeHidden()
  })
})
