import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

const INTERVIEW_PROCESS_ID = '00000000-0000-4000-e600-000000000001' // Topic 3, DSA group, active
const INTERVIEWEE_ID = '00000000-0000-4000-e700-000000000001' // student4, NULL score

test.describe('Interview Workflow', () => {
  test.use({ storageState: authStatePath('examiner2') })

  test('examiner can view interview process and score an interviewee', async ({ page }) => {
    // Navigate directly to the interviewee assessment page
    await navigateTo(page, `/interviews/${INTERVIEW_PROCESS_ID}/interviewee/${INTERVIEWEE_ID}`)

    // Wait for page to load with the interviewee name
    await expect(page.getByRole('heading', { name: /Interview - Student4 User/i })).toBeVisible({
      timeout: 15_000,
    })

    // Verify "Interview Assessment" section title
    await expect(page.getByRole('heading', { name: 'Interview Assessment' })).toBeVisible()

    // Verify "Score" card title is visible
    await expect(page.getByRole('heading', { name: 'Score', exact: true })).toBeVisible()

    // Set score using SegmentedControl - click on "Great Candidate" (score 4)
    await page.getByText('Great Candidate').click()

    // Verify score selection registered — Mantine SegmentedControl sets data-active on the label
    const scoreLabel = page
      .getByText('Great Candidate')
      .locator('xpath=ancestor::label[@data-active]')
    await expect(scoreLabel).toBeVisible({ timeout: 5_000 })

    // Verify "Interview Note" card title is visible
    await expect(page.getByRole('heading', { name: 'Interview Note', exact: true })).toBeVisible()

    // Fill interview note using the ProseMirror editor
    const noteEditor = page.locator('.ProseMirror').first()
    await noteEditor.click()
    const modifier = process.platform === 'darwin' ? 'Meta' : 'Control'
    await page.keyboard.press(`${modifier}+a`)
    await page.keyboard.type(
      'Good understanding of streaming concepts and anomaly detection methods.',
    )

    // Wait for auto-save (debounced at 1000ms) — look for "Saved" indicator
    await expect(page.getByText('Saved')).toBeVisible({ timeout: 10_000 })
  })

  test('examiner can open the add slot modal on interview process page', async ({ page }) => {
    await navigateTo(page, `/interviews/${INTERVIEW_PROCESS_ID}`)

    // Wait for interview management page to load
    await expect(page.getByRole('heading', { name: /interview management/i })).toBeVisible({
      timeout: 15_000,
    })

    // Verify interviewees section is visible
    await expect(page.getByRole('heading', { name: 'Interviewees', exact: true })).toBeVisible({
      timeout: 10_000,
    })

    // Seeded interviewees for process 1: student4 (NULL score) and student5 (scored 45)
    await expect(page.getByText(/Student4/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Student5/i).first()).toBeVisible({ timeout: 10_000 })

    // Click "Add Slot" button to open the modal
    await page.getByRole('button', { name: /Add Slot|^Add$/i }).click()

    // Modal should open with "Add Interview Slot" title
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })
    await expect(dialog.getByText('Add Interview Slot')).toBeVisible()

    // Verify Interview Length SegmentedControl is visible with all options
    await expect(dialog.getByText('Interview Length')).toBeVisible()
    await expect(dialog.getByText('30min').first()).toBeVisible()
    await expect(dialog.getByText('45min').first()).toBeVisible()

    // Verify "Select Dates" section is visible
    await expect(dialog.getByText('Select Dates')).toBeVisible()

    // Verify "Save Slots" button exists and is initially present
    await expect(dialog.getByRole('button', { name: 'Save Slots' })).toBeVisible()

    // Close modal via Cancel/close without saving
    const closeButton = dialog
      .getByRole('button', { name: 'Cancel' })
      .or(dialog.locator('button.mantine-Modal-close'))
    await closeButton.first().click()
    await expect(dialog).not.toBeVisible({ timeout: 3_000 })
  })

  test('interview process page shows interview slots section', async ({ page }) => {
    await navigateTo(page, `/interviews/${INTERVIEW_PROCESS_ID}`)

    await expect(page.getByRole('heading', { name: /interview management/i })).toBeVisible({
      timeout: 15_000,
    })

    // Should show the "Add Slot" button (examiner2 can add slots)
    await expect(page.getByRole('button', { name: /Add Slot|^Add$/i })).toBeVisible({
      timeout: 10_000,
    })

    // Should show Interviewees section header
    await expect(page.getByRole('heading', { name: 'Interviewees', exact: true })).toBeVisible()
  })
})
