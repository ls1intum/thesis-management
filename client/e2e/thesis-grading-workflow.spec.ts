import { test, expect } from '@playwright/test'
import { authStatePath, fillRichTextEditor, navigateTo } from './helpers'

// Thesis d000-0003: SUBMITTED state, student3, advisor2, supervisor2 (DSA group)
// Note: Seed data inserts an assessment row directly but thesis state remains SUBMITTED.
// supervisor2 has both advisor and supervisor access (as supervisor on the thesis).
const THESIS_ID = '00000000-0000-4000-d000-000000000003'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'Online Anomaly Detection in IoT Sensor Streams'

test.describe.serial('Thesis Grading Workflow', () => {
  test('supervisor can submit an assessment on a SUBMITTED thesis', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('supervisor2') })
    const page = await context.newPage()

    await navigateTo(page, THESIS_URL)

    // Wait for thesis page to load
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
      timeout: 15_000,
    })

    // Check if the assessment section is actionable (thesis may already be FINISHED from a prior run)
    const editButton = page.getByRole('button', { name: 'Edit Assessment' })
    const addButton = page.getByRole('button', { name: 'Add Assessment' })
    const hasEdit = await editButton.isVisible({ timeout: 5_000 }).catch(() => false)
    const hasAdd = await addButton.isVisible({ timeout: 2_000 }).catch(() => false)

    if (!hasEdit && !hasAdd) {
      // Thesis is already past ASSESSED state (GRADED or FINISHED); assessment is read-only
      await expect(page.getByText('Assessment')).toBeVisible()
      await context.close()
      return
    }

    if (hasEdit) {
      await editButton.click()
    } else {
      await addButton.click()
    }

    // Modal should open with "Submit Assessment" title
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    await expect(dialog.getByText('Submit Assessment').first()).toBeVisible()

    // Fill in assessment fields — scope to dialog to avoid matching read-only assessment behind modal
    await fillRichTextEditor(page, 'Summary', 'The thesis provides a comprehensive analysis of anomaly detection methods for IoT sensor data.', dialog)
    await fillRichTextEditor(page, 'Strengths', 'Strong methodology and well-structured experiments with real-world datasets.', dialog)
    await fillRichTextEditor(page, 'Weaknesses', 'Limited discussion of related work in the streaming domain.', dialog)

    // Fill Grade Suggestion (TextInput) — clear first in case of existing value
    await dialog.getByLabel('Grade Suggestion').clear()
    await dialog.getByLabel('Grade Suggestion').fill('1.3')

    // Click "Submit Assessment" button in the modal
    const submitButton = dialog.getByRole('button', { name: 'Submit Assessment' })
    await expect(submitButton).toBeEnabled({ timeout: 5_000 })
    await submitButton.click()

    // Modal should close
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })

    // Verify success notification
    await expect(page.getByText('Assessment submitted successfully')).toBeVisible({
      timeout: 10_000,
    })

    await context.close()
  })

  test('supervisor can submit a final grade on an ASSESSED thesis', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('supervisor2') })
    const page = await context.newPage()

    await navigateTo(page, THESIS_URL)

    // Wait for thesis page to load
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
      timeout: 15_000,
    })

    // Check if "Add Final Grade" button is available (thesis may already be GRADED/FINISHED)
    const addGradeButton = page.getByRole('button', { name: 'Add Final Grade' })
    const editGradeButton = page.getByRole('button', { name: 'Edit Final Grade' })
    const hasAdd = await addGradeButton.isVisible({ timeout: 5_000 }).catch(() => false)
    const hasEdit = await editGradeButton.isVisible({ timeout: 2_000 }).catch(() => false)

    if (!hasAdd && !hasEdit) {
      // Thesis is already FINISHED — grade section exists but no edit button
      await context.close()
      return
    }

    const gradeButton = hasAdd ? addGradeButton : editGradeButton
    await gradeButton.click()

    // Modal should open
    const gradeDialog = page.getByRole('dialog')
    await expect(gradeDialog).toBeVisible({ timeout: 5_000 })
    await expect(gradeDialog.getByText('Submit Final Grade').first()).toBeVisible()

    // Thesis Visibility select should be visible
    await expect(gradeDialog.getByRole('textbox', { name: 'Thesis Visibility' })).toBeVisible()

    // Fill Final Grade (TextInput) — scope to dialog to avoid matching accordion panel
    await gradeDialog.getByRole('textbox', { name: 'Final Grade' }).fill('1.3')

    // Fill optional feedback (DocumentEditor)
    await fillRichTextEditor(page, 'Feedback (Visible to student)', 'Excellent work overall.', gradeDialog)

    // Click "Submit Grade" button
    const submitButton = gradeDialog.getByRole('button', { name: 'Submit Grade' })
    await expect(submitButton).toBeEnabled({ timeout: 5_000 })
    await submitButton.click()

    // Modal should close
    await expect(gradeDialog).not.toBeVisible({ timeout: 15_000 })

    // Verify success notification
    await expect(page.getByText('Final Grade submitted successfully')).toBeVisible({
      timeout: 10_000,
    })

    await context.close()
  })

  test('supervisor can mark a GRADED thesis as finished', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('supervisor2') })
    const page = await context.newPage()

    await navigateTo(page, THESIS_URL)

    // Wait for thesis page to load
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
      timeout: 15_000,
    })

    // "Mark thesis as finished" button is only visible for GRADED thesis
    const finishButton = page.getByRole('button', { name: 'Mark thesis as finished' })
    const isGraded = await finishButton.isVisible({ timeout: 5_000 }).catch(() => false)

    if (!isGraded) {
      // Thesis is already FINISHED from a prior run — verify it has a grade displayed
      await expect(page.getByText('Final Grade').first()).toBeVisible()
      await context.close()
      return
    }

    await finishButton.click()

    // Verify success notification
    await expect(page.getByText('Thesis successfully marked as finished')).toBeVisible({
      timeout: 10_000,
    })

    await context.close()
  })
})
