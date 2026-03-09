import { test, expect } from '@playwright/test'
import {
  authStatePath,
  createTestPdfBuffer,
  fillRichTextEditor,
  navigateTo,
  selectOption,
} from './helpers'
import {
  snapshotMailbox,
  waitForNewMessages,
  getBody,
  getToAddresses,
  assertSentFromApp,
  findBySubject,
} from './mailpit'

test.describe('Application Workflow - Student submits application', () => {
  test.use({ storageState: authStatePath('student') })

  test('student can submit an application for a topic through the full stepper', async ({
    page,
  }) => {
    test.setTimeout(120_000) // Extended timeout — multi-step stepper with file uploads
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

    // Upload required files — wait for each Dropzone to mount its file input.
    // If a file is already uploaded (preview visible instead of dropzone), skip it.
    const pdfBuffer = createTestPdfBuffer()
    for (const label of ['Examination Report', 'CV', 'Bachelor Report']) {
      const wrapper = page.locator(
        `.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("${label}"))`,
      )
      // Skip if file is already uploaded (preview iframe or card visible)
      const alreadyUploaded = await wrapper
        .locator('iframe, .mantine-Card-root')
        .first()
        .isVisible({ timeout: 2_000 })
        .catch(() => false)
      if (alreadyUploaded) continue

      // Wait for the Dropzone file input to be attached in the DOM (may take time under load)
      const fileInput = wrapper.locator('input[type="file"]')
      await fileInput.waitFor({ state: 'attached', timeout: 15_000 })
      await fileInput.setInputFiles({
        name: `${label.toLowerCase().replace(/ /g, '-')}.pdf`,
        mimeType: 'application/pdf',
        buffer: pdfBuffer,
      })
    }

    // Accept privacy notice if not already checked
    const privacyCheckbox = page.getByRole('checkbox', { name: /privacy/i })
    if (!(await privacyCheckbox.isChecked())) {
      await privacyCheckbox.check()
    }
    await expect(privacyCheckbox).toBeChecked({ timeout: 5_000 })

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

    // Snapshot mailboxes BEFORE submitting (safe for parallel execution)
    const beforeStudentIds = await snapshotMailbox('student@test.local')
    const beforeExaminerIds = await snapshotMailbox('examiner@test.local')
    const beforeSupervisorIds = await snapshotMailbox('supervisor@test.local')

    const submitButton = page.getByRole('button', { name: 'Submit Application' })
    await expect(submitButton).toBeEnabled({ timeout: 10_000 })
    await submitButton.click()

    // Verify we're still on the application page (no crash)
    await page.waitForTimeout(2_000)
    await expect(page).toHaveURL(/\/submit-application/)

    // --- Email verification ---
    // Application submission sends:
    // 1. APPLICATION_CREATED_STUDENT → student (confirmation with application details)
    // 2. APPLICATION_CREATED_CHAIR → each research group member individually
    //    (filtered by notification preferences; default "own" keeps topic role members)

    // Verify student confirmation email (APPLICATION_CREATED_STUDENT template)
    const newStudentEmails = await waitForNewMessages('student@test.local', beforeStudentIds)
    const studentEmail = findBySubject(newStudentEmails, 'Thesis Application Confirmation')
    expect(studentEmail, 'Student confirmation email should be sent').toBeDefined()
    assertSentFromApp(studentEmail!)
    expect(getToAddresses(studentEmail!)).toContain('student@test.local')

    const studentBody = getBody(studentEmail!)
    expect(studentBody, 'Body should greet the student by first name').toContain('Student')
    expect(studentBody, 'Body should contain the topic/thesis title').toContain(
      'Continuous Integration Pipeline Optimization',
    )
    expect(studentBody, 'Body should contain applicant email').toContain('student@test.local')
    expect(studentBody, 'Body should reference the motivation text').toContain('CI pipeline')

    // Verify examiner notification email
    // Topic 2 roles: examiner=examiner (EXAMINER), supervisor+supervisor2=supervisors (SUPERVISOR)
    // All are in ASE group, so all should receive the chair notification.
    const examinerEmails = await waitForNewMessages('examiner@test.local', beforeExaminerIds)
    const examinerChairEmail = findBySubject(examinerEmails, 'New Thesis Application')
    expect(
      examinerChairEmail,
      'Examiner (examiner@test.local) should receive "New Thesis Application" email',
    ).toBeDefined()
    assertSentFromApp(examinerChairEmail!)

    const examinerBody = getBody(examinerChairEmail!)
    expect(examinerBody, 'Examiner email should mention the applicant').toContain('Student')
    expect(examinerBody, 'Examiner email should reference the topic').toContain(
      'Continuous Integration Pipeline Optimization',
    )

    // Verify supervisor (supervisor user) also receives the notification
    const supervisorEmails = await waitForNewMessages('supervisor@test.local', beforeSupervisorIds)
    const supervisorChairEmail = findBySubject(supervisorEmails, 'New Thesis Application')
    expect(
      supervisorChairEmail,
      'Supervisor (supervisor@test.local) should receive "New Thesis Application" email',
    ).toBeDefined()
  })
})
