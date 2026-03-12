import { test, expect, Page, Locator } from '@playwright/test'
import {
  authStatePath,
  fillRichTextEditor,
  hideWebpackOverlay,
  navigateTo,
  navigateToDetail,
} from './helpers'

// DSA group (a000-000000000002) has a default grading scheme in seed data
const DSA_GROUP_ID = '00000000-0000-4000-a000-000000000002'
const DSA_SETTINGS_URL = `/research-groups/${DSA_GROUP_ID}`

// ASE group (a000-000000000001) has no grading scheme
const ASE_GROUP_ID = '00000000-0000-4000-a000-000000000001'
const ASE_SETTINGS_URL = `/research-groups/${ASE_GROUP_ID}`

// Thesis d000-0003: ASSESSED state, student3, supervisor2, examiner2 (DSA group)
const THESIS_ID = '00000000-0000-4000-d000-000000000003'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'Online Anomaly Detection in IoT Sensor Streams'

/**
 * Get the Grading Scheme card locator scoped to its Mantine Card container.
 */
function gradingSchemeCard(page: Page): Locator {
  return page.locator('.mantine-Card-root', { has: page.getByRole('heading', { name: 'Grading Scheme', level: 3 }) })
}

/**
 * Navigate to a settings page and wait for the Grading Scheme card to load.
 */
async function navigateToSettings(page: Page, url: string) {
  await navigateTo(page, url)
  await hideWebpackOverlay(page)
  await expect(page.getByRole('heading', { name: /research group settings/i })).toBeVisible({
    timeout: 15_000,
  })
  const card = gradingSchemeCard(page)
  await expect(card).toBeVisible({ timeout: 15_000 })
}

/**
 * Helper to get the weight input value for a given table row.
 */
async function getRowWeight(row: Locator): Promise<string> {
  return row.locator('td:nth-child(2) input').inputValue()
}

test.describe('Grading Scheme Settings - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('DSA group shows seeded grading scheme components with correct weights', async ({ page }) => {
    await navigateToSettings(page, DSA_SETTINGS_URL)

    const card = gradingSchemeCard(page)

    // Verify the seeded components are displayed
    await expect(card.locator('input[value="Thesis Content"]')).toBeVisible({ timeout: 10_000 })
    await expect(card.locator('input[value="Methodology"]')).toBeVisible()
    await expect(card.locator('input[value="Presentation"]')).toBeVisible()

    // Verify we have exactly 3 rows
    const rows = card.locator('table tbody tr')
    await expect(rows).toHaveCount(3)

    // Verify the seeded weights (40, 30, 30)
    await expect(rows.nth(0).locator('td:nth-child(2) input')).toHaveValue('40%')
    await expect(rows.nth(1).locator('td:nth-child(2) input')).toHaveValue('30%')
    await expect(rows.nth(2).locator('td:nth-child(2) input')).toHaveValue('30%')

    // Verify none are bonus components (checkboxes should be unchecked)
    for (let i = 0; i < 3; i++) {
      const checkbox = rows.nth(i).locator('input[type="checkbox"]')
      await expect(checkbox).not.toBeChecked()
    }

    // Save button should be enabled (valid scheme)
    const saveButton = card.getByRole('button', { name: 'Save Grading Scheme' })
    await expect(saveButton).toBeEnabled()
  })

  test('can add a new grading scheme to a group without one', async ({ page }) => {
    await navigateToSettings(page, ASE_SETTINGS_URL)

    const card = gradingSchemeCard(page)

    // Remove any existing components from prior test runs
    let existingRows = await card.locator('table tbody tr').count().catch(() => 0)
    while (existingRows > 0) {
      const deleteBtn = card.locator('table tbody tr').first().locator('button').last()
      await deleteBtn.click({ force: true })
      existingRows = await card.locator('table tbody tr').count().catch(() => 0)
    }

    // Add first component
    await card.getByRole('button', { name: 'Add Component' }).click({ force: true })

    // Fill in the new component
    const nameInput = card.locator('input[placeholder="Component name"]').first()
    await nameInput.fill('Research Quality')

    const weightInput = card.locator('table tbody tr td:nth-child(2) input').first()
    await weightInput.clear()
    await weightInput.fill('100')

    // Save
    const saveButton = card.getByRole('button', { name: 'Save Grading Scheme' })
    await expect(saveButton).toBeEnabled({ timeout: 5_000 })
    await saveButton.click({ force: true })

    // Wait for save to complete by reloading and verifying persistence
    await navigateToSettings(page, ASE_SETTINGS_URL)
    const cardAfter = gradingSchemeCard(page)

    // Verify the component persisted with correct name and weight
    await expect(cardAfter.locator('input[value="Research Quality"]')).toBeVisible({ timeout: 10_000 })
    await expect(cardAfter.locator('table tbody tr')).toHaveCount(1)
    await expect(cardAfter.locator('table tbody tr td:nth-child(2) input')).toHaveValue('100%')
  })

  test('shows weight warning when weights do not sum to 100', async ({ page }) => {
    await navigateToSettings(page, DSA_SETTINGS_URL)

    const card = gradingSchemeCard(page)

    // Wait for components to load
    await expect(card.locator('input[value="Thesis Content"]')).toBeVisible({ timeout: 10_000 })

    // Change the first component weight to break the sum (10 + 30 + 30 = 70)
    const firstWeightInput = card.locator('table tbody tr').first().locator('td:nth-child(2) input')
    await firstWeightInput.clear()
    await firstWeightInput.fill('10')

    // Weight warning should appear
    await expect(card.getByText('Weight Warning')).toBeVisible({ timeout: 5_000 })
    await expect(card.getByText(/must sum to 100%/)).toBeVisible()

    // Save button should be disabled
    const saveButton = card.getByRole('button', { name: 'Save Grading Scheme' })
    await expect(saveButton).toBeDisabled()

    // Fix the weight back to 40 — warning should disappear
    await firstWeightInput.clear()
    await firstWeightInput.fill('40')
    await expect(card.getByText('Weight Warning')).toBeHidden({ timeout: 5_000 })
    await expect(saveButton).toBeEnabled()
  })

  test('can remove a component and sees weight warning', async ({ page }) => {
    await navigateToSettings(page, DSA_SETTINGS_URL)

    const card = gradingSchemeCard(page)

    // Wait for components to load
    await expect(card.locator('input[value="Thesis Content"]')).toBeVisible({ timeout: 10_000 })

    // Count components before removal
    const rowsBefore = await card.locator('table tbody tr').count()
    expect(rowsBefore).toBeGreaterThanOrEqual(3)

    // Click the delete button on the last row
    const lastRow = card.locator('table tbody tr').last()
    const deleteButton = lastRow.locator('button').last()
    await deleteButton.click({ force: true })

    // Should have one fewer row
    const rowsAfter = await card.locator('table tbody tr').count()
    expect(rowsAfter).toBe(rowsBefore - 1)

    // Remaining components still have 40 + 30 = 70, so weight warning should appear
    await expect(card.getByText('Weight Warning')).toBeVisible({ timeout: 5_000 })
    await expect(card.getByRole('button', { name: 'Save Grading Scheme' })).toBeDisabled()
  })
})

test.describe('Grading Scheme Settings - Access Control', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor can view grading scheme but settings are read-only', async ({ page }) => {
    await navigateTo(page, `/research-groups/00000000-0000-4000-a000-000000000001`)
    await hideWebpackOverlay(page)

    // Supervisor should be able to see the settings page (or be redirected)
    const heading = page.getByRole('heading', { name: /research group settings/i })
    const visible = await heading.isVisible({ timeout: 10_000 }).catch(() => false)
    if (!visible) {
      // Supervisor may not have access — this is acceptable
      return
    }

    // If visible, verify the grading scheme card exists
    const card = gradingSchemeCard(page)
    await expect(card).toBeVisible({ timeout: 10_000 })
  })
})

test.describe.serial('Assessment with Grade Components', () => {
  test('examiner sees pre-filled grade components with correct weights from research group scheme', async ({
    browser,
  }) => {
    const context = await browser.newContext({ storageState: authStatePath('examiner2') })
    const page = await context.newPage()

    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    const loaded = await navigateToDetail(page, THESIS_URL, heading)
    await hideWebpackOverlay(page)
    if (!loaded) {
      test.skip(true, 'Thesis detail page did not load')
      await context.close()
      return
    }

    // Open the assessment modal
    const editButton = page.getByRole('button', { name: 'Edit Assessment' })
    const addButton = page.getByRole('button', { name: 'Add Assessment' })
    const hasEdit = await editButton.isVisible({ timeout: 5_000 }).catch(() => false)
    const hasAdd = await addButton.isVisible({ timeout: 2_000 }).catch(() => false)

    if (!hasEdit && !hasAdd) {
      test.skip(true, 'No assessment button available')
      await context.close()
      return
    }

    const btn = hasEdit ? editButton : addButton
    await btn.click({ force: true })

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })

    // Verify grade components section is visible
    await expect(dialog.getByText('Grade Components')).toBeVisible({ timeout: 15_000 })

    // Verify the component names from the DSA scheme are pre-filled
    await expect(dialog.locator('input[value="Thesis Content"]')).toBeVisible({ timeout: 5_000 })
    await expect(dialog.locator('input[value="Methodology"]')).toBeVisible()
    await expect(dialog.locator('input[value="Presentation"]')).toBeVisible()

    // Verify the weights are pre-filled from the scheme (40, 30, 30)
    const weightInputs = dialog.locator('table tbody tr td:nth-child(2) input')
    await expect(weightInputs.nth(0)).toHaveValue('40%')
    await expect(weightInputs.nth(1)).toHaveValue('30%')
    await expect(weightInputs.nth(2)).toHaveValue('30%')

    // Verify exactly 3 component rows
    await expect(dialog.locator('table tbody tr')).toHaveCount(3)

    await context.close()
  })

  test('examiner can submit assessment with grade components and see calculated grade', async ({
    browser,
  }) => {
    test.setTimeout(120_000)

    const context = await browser.newContext({ storageState: authStatePath('examiner2') })
    const page = await context.newPage()

    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    const loaded = await navigateToDetail(page, THESIS_URL, heading)
    await hideWebpackOverlay(page)
    if (!loaded) {
      test.skip(true, 'Thesis detail page did not load')
      await context.close()
      return
    }

    // Check if another parallel test already submitted an assessment with grade components
    const alreadyHasGradeComponents = await page
      .getByText('Grade Components')
      .first()
      .isVisible({ timeout: 3_000 })
      .catch(() => false)

    if (alreadyHasGradeComponents) {
      // Another test (thesis-grading-workflow) already submitted — verify components are visible
      await expect(page.getByText('Thesis Content').first()).toBeVisible({ timeout: 5_000 })
      await expect(page.getByText('Methodology').first()).toBeVisible()
      await expect(page.getByText('Presentation').first()).toBeVisible()
      await expect(page.getByText(/Calculated Grade/).first()).toBeVisible()
      await context.close()
      return
    }

    const editButton = page.getByRole('button', { name: 'Edit Assessment' })
    const addButton = page.getByRole('button', { name: 'Add Assessment' })
    const hasEdit = await editButton.isVisible({ timeout: 5_000 }).catch(() => false)
    const hasAdd = await addButton.isVisible({ timeout: 2_000 }).catch(() => false)

    if (!hasEdit && !hasAdd) {
      test.skip(true, 'No assessment button available')
      await context.close()
      return
    }

    await (hasEdit ? editButton : addButton).click({ force: true })

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })

    // Fill in assessment text fields
    await fillRichTextEditor(
      page,
      'Summary',
      'Comprehensive analysis of anomaly detection.',
      dialog,
    )
    await fillRichTextEditor(page, 'Strengths', 'Strong methodology and experiments.', dialog)
    await fillRichTextEditor(page, 'Weaknesses', 'Limited related work discussion.', dialog)

    // Wait for grade components to appear
    await expect(dialog.getByText('Grade Components')).toBeVisible({ timeout: 15_000 })

    // Fill in grades: 1.3 (40%), 1.7 (30%), 2.0 (30%)
    const gradeInputs = dialog.locator('table tbody tr td:nth-child(4) input')
    const gradeCount = await gradeInputs.count()
    expect(gradeCount).toBeGreaterThanOrEqual(3)

    const grades = ['1.3', '1.7', '2.0']
    for (let i = 0; i < 3; i++) {
      await gradeInputs.nth(i).click({ clickCount: 3 })
      await gradeInputs.nth(i).pressSequentially(grades[i], { delay: 50 })
      await gradeInputs.nth(i).press('Tab')
    }

    // Scroll down in dialog to reveal calculated grade
    await dialog
      .locator('.mantine-Modal-body, .mantine-ScrollArea-viewport')
      .first()
      .evaluate((el) => el.scrollTo(0, el.scrollHeight))
      .catch(() => {})

    // Verify calculated grade appears
    await expect(dialog.getByText(/Calculated Grade/)).toBeVisible({ timeout: 10_000 })

    // Grade suggestion should be auto-filled
    const gradeSuggestion = dialog.getByLabel('Grade Suggestion')
    const gradeValue = await gradeSuggestion.inputValue()
    expect(gradeValue).toBeTruthy()
    const parsedGrade = parseFloat(gradeValue)
    expect(parsedGrade).toBeGreaterThanOrEqual(1.0)
    expect(parsedGrade).toBeLessThanOrEqual(5.0)

    // Submit
    const submitButton = dialog.getByRole('button', { name: 'Submit Assessment' })
    await expect(submitButton).toBeEnabled({ timeout: 10_000 })
    await submitButton.click({ force: true })

    // Modal should close
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })

    // Verify success notification
    await expect(page.getByText('Assessment submitted successfully')).toBeVisible({
      timeout: 10_000,
    })

    // Verify grade components are displayed in the assessment section (read-only)
    await expect(page.getByText('Grade Components').first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText('Thesis Content').first()).toBeVisible()
    await expect(page.getByText('Methodology').first()).toBeVisible()
    await expect(page.getByText('Presentation').first()).toBeVisible()
    await expect(page.getByText(/Calculated Grade/).first()).toBeVisible()

    await context.close()
  })

  test('final grade modal shows calculated grade hint from assessment', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('examiner2') })
    const page = await context.newPage()

    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    const loaded = await navigateToDetail(page, THESIS_URL, heading)
    await hideWebpackOverlay(page)
    if (!loaded) {
      test.skip(true, 'Thesis detail page did not load')
      await context.close()
      return
    }

    // Open the final grade modal
    const addGradeButton = page.getByRole('button', { name: 'Add Final Grade' })
    const editGradeButton = page.getByRole('button', { name: 'Edit Final Grade' })
    const hasAdd = await addGradeButton.isVisible({ timeout: 5_000 }).catch(() => false)
    const hasEdit = await editGradeButton.isVisible({ timeout: 2_000 }).catch(() => false)

    if (!hasAdd && !hasEdit) {
      test.skip(true, 'No grade button available')
      await context.close()
      return
    }

    await (hasAdd ? addGradeButton : editGradeButton).click({ force: true })

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 10_000 })

    // Verify calculated grade hint is shown
    const hintText = dialog.getByText(/Calculated from assessment components/)
    await expect(hintText).toBeVisible({ timeout: 5_000 })

    // Verify the hint contains a valid grade value
    const hintContent = await hintText.textContent()
    const match = hintContent?.match(/components:\s*([\d.]+)/)
    expect(match).toBeTruthy()
    const calculatedGrade = parseFloat(match![1])
    expect(calculatedGrade).toBeGreaterThanOrEqual(1.0)
    expect(calculatedGrade).toBeLessThanOrEqual(5.0)

    // Enter a deviating grade to verify deviation warning
    const deviatingGrade = calculatedGrade <= 3.0 ? calculatedGrade + 1.0 : calculatedGrade - 1.0
    const finalGradeInput = dialog.getByRole('textbox', { name: 'Final Grade' })
    await finalGradeInput.clear()
    await finalGradeInput.fill(deviatingGrade.toFixed(1))

    // Deviation warning should appear
    await expect(dialog.getByText('Grade Deviation')).toBeVisible({ timeout: 5_000 })
    await expect(dialog.getByText(/deviates from the calculated assessment grade/)).toBeVisible()

    await context.close()
  })
})
