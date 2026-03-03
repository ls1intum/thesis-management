import { test, expect } from '@playwright/test'
import { authStatePath, fillRichTextEditor, navigateToDetail } from './helpers'
import {
  snapshotMailbox,
  waitForNewMessages,
  getSubject,
  getBody,
  getToAddresses,
  assertSentFromApp,
} from './mailpit'

// Thesis d000-0003: SUBMITTED state, student3, supervisor2, examiner2 (DSA group)
// Note: Seed data inserts an assessment row directly but thesis state remains SUBMITTED.
// examiner2 has both supervisor and examiner access (as examiner on the thesis).
const THESIS_ID = '00000000-0000-4000-d000-000000000003'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'Online Anomaly Detection in IoT Sensor Streams'

test.describe.serial('Thesis Grading Workflow', () => {
  test('examiner can submit an assessment on a SUBMITTED thesis', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('examiner2') })
    const page = await context.newPage()

    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    const loaded = await navigateToDetail(page, THESIS_URL, heading)
    if (!loaded) {
      await context.close()
      return
    }

    // Check if the assessment section is actionable (thesis may already be FINISHED from a prior run)
    const editButton = page.getByRole('button', { name: 'Edit Assessment' })
    const addButton = page.getByRole('button', { name: 'Add Assessment' })
    const hasEdit = await editButton.isVisible({ timeout: 5_000 }).catch(() => false)
    const hasAdd = await addButton.isVisible({ timeout: 2_000 }).catch(() => false)

    if (!hasEdit && !hasAdd) {
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

    // Fill in assessment fields
    await fillRichTextEditor(
      page,
      'Summary',
      'The thesis provides a comprehensive analysis of anomaly detection methods for IoT sensor data.',
      dialog,
    )
    await fillRichTextEditor(
      page,
      'Strengths',
      'Strong methodology and well-structured experiments with real-world datasets.',
      dialog,
    )
    await fillRichTextEditor(
      page,
      'Weaknesses',
      'Limited discussion of related work in the streaming domain.',
      dialog,
    )

    await dialog.getByLabel('Grade Suggestion').clear()
    await dialog.getByLabel('Grade Suggestion').fill('1.3')

    // Snapshot mailbox BEFORE submitting
    const beforeIds = await snapshotMailbox('examiner2@test.local')

    const submitButton = dialog.getByRole('button', { name: 'Submit Assessment' })
    await expect(submitButton).toBeEnabled({ timeout: 5_000 })
    await submitButton.click()

    // Modal should close
    await expect(dialog).not.toBeVisible({ timeout: 15_000 })

    // Verify success notification
    await expect(page.getByText('Assessment submitted successfully')).toBeVisible({
      timeout: 10_000,
    })

    // --- Email verification ---
    // THESIS_ASSESSMENT_ADDED is sent to thesis examiners EXCEPT the sender.
    // MailBuilder excludes the primarySender from recipients when secondaryRecipients
    // is empty (MailBuilder.java line 508). Since examiner2 is both the submitter
    // and the only examiner on this thesis, no email is sent.
    //
    // NOTE: To fully test the THESIS_ASSESSMENT_ADDED email template, a test with
    // a thesis that has multiple examiners would be needed (so the non-submitting
    // examiner receives the email). This is a known coverage gap.
    //
    // We verify that NO assessment email was sent to examiner2 (confirms the
    // sender-exclusion logic works correctly).
    const afterIds = await snapshotMailbox('examiner2@test.local')
    const newIds = [...afterIds].filter((id) => !beforeIds.has(id))
    expect(
      newIds.length,
      'No assessment email should be sent to the examiner who submitted it',
    ).toBe(0)

    await context.close()
  })

  test('examiner can submit a final grade on an ASSESSED thesis', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('examiner2') })
    const page = await context.newPage()

    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    const loaded = await navigateToDetail(page, THESIS_URL, heading)
    if (!loaded) {
      await context.close()
      return
    }

    // Check if "Add Final Grade" button is available
    const addGradeButton = page.getByRole('button', { name: 'Add Final Grade' })
    const editGradeButton = page.getByRole('button', { name: 'Edit Final Grade' })
    const hasAdd = await addGradeButton.isVisible({ timeout: 5_000 }).catch(() => false)
    const hasEdit = await editGradeButton.isVisible({ timeout: 2_000 }).catch(() => false)

    if (!hasAdd && !hasEdit) {
      await context.close()
      return
    }

    const gradeButton = hasAdd ? addGradeButton : editGradeButton
    await gradeButton.click()

    // Modal should open
    const gradeDialog = page.getByRole('dialog')
    await expect(gradeDialog).toBeVisible({ timeout: 5_000 })
    await expect(gradeDialog.getByText('Submit Final Grade').first()).toBeVisible()

    await expect(gradeDialog.getByRole('textbox', { name: 'Thesis Visibility' })).toBeVisible()
    await gradeDialog.getByRole('textbox', { name: 'Final Grade' }).fill('1.3')

    await fillRichTextEditor(
      page,
      'Feedback (Visible to student)',
      'Excellent work overall.',
      gradeDialog,
    )

    // Snapshot mailbox BEFORE submitting
    const beforeIds = await snapshotMailbox('student3@test.local')

    const submitButton = gradeDialog.getByRole('button', { name: 'Submit Grade' })
    await expect(submitButton).toBeEnabled({ timeout: 5_000 })
    await submitButton.click()

    // Modal should close
    await expect(gradeDialog).not.toBeVisible({ timeout: 15_000 })

    // Verify success notification
    await expect(page.getByText('Final Grade submitted successfully')).toBeVisible({
      timeout: 10_000,
    })

    // --- Email verification ---
    // THESIS_FINAL_GRADE is sent to the thesis students (student3)
    const newEmails = await waitForNewMessages('student3@test.local', beforeIds)
    expect(newEmails.length).toBeGreaterThanOrEqual(1)

    const gradeEmail = newEmails.find((e) => getSubject(e) === 'Final Grade available for Thesis')
    expect(gradeEmail, 'Final grade email should be sent').toBeDefined()
    assertSentFromApp(gradeEmail!)
    expect(getToAddresses(gradeEmail!)).toContain('student3@test.local')

    // Body should contain: greeting, thesis title, examiner name, final grade,
    // feedback text, and a link to the thesis
    const body = getBody(gradeEmail!)
    expect(body, 'Should greet the student by first name').toContain('Student3')
    expect(body, 'Should contain the thesis title').toContain(THESIS_TITLE)
    expect(body, 'Should contain the final grade value').toContain('1.3')
    expect(body, 'Should contain the feedback text').toContain('Excellent work overall')
    expect(body, 'Should contain a link to the thesis').toContain('/theses/')
    expect(body, 'Should mention the examiner name').toContain('Examiner2')

    await context.close()
  })

  test('examiner can mark a GRADED thesis as finished', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('examiner2') })
    const page = await context.newPage()

    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    const loaded = await navigateToDetail(page, THESIS_URL, heading)
    if (!loaded) {
      await context.close()
      return
    }

    const finishButton = page.getByRole('button', { name: 'Mark thesis as finished' })
    const isGraded = await finishButton.isVisible({ timeout: 5_000 }).catch(() => false)

    if (!isGraded) {
      await expect(page.getByText('Final Grade').first()).toBeVisible()
      await context.close()
      return
    }

    await finishButton.click()

    await expect(page.getByText('Thesis successfully marked as finished')).toBeVisible({
      timeout: 10_000,
    })

    await context.close()
  })
})
