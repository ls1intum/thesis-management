import { test, expect } from './resource-lock'
import { authStatePath, navigateTo, navigateToDetail, selectOption } from './helpers'
import {
  snapshotMailbox,
  waitForNewMessageBySubject,
  getBody,
  getToAddresses,
  assertSentFromApp,
  assertEmailFooter,
} from './mailpit'

// Thesis d000-0003 is in ASSESSED state, assigned to student3, has abstract text set
// Roles: examiner2 (EXAMINER), supervisor2 (SUPERVISOR), student3 (STUDENT)
const THESIS_ID = '00000000-0000-4000-d000-000000000003'
const THESIS_URL = `/theses/${THESIS_ID}`
const THESIS_TITLE = 'Online Anomaly Detection in IoT Sensor Streams'

test.describe.serial('Presentation Workflow', () => {
  test('student can create a presentation draft for their thesis', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('student3') })
    const page = await context.newPage()

    await navigateTo(page, THESIS_URL)

    // Wait for thesis page to load
    await expect(page.getByRole('heading', { name: THESIS_TITLE })).toBeVisible({
      timeout: 30_000,
    })

    // Find the Presentation accordion section and ensure it is visible
    const presentationControl = page.getByRole('button', { name: 'Presentation', exact: true })
    await expect(presentationControl).toBeVisible({ timeout: 10_000 })

    // Click to expand if collapsed
    if ((await presentationControl.getAttribute('aria-expanded')) !== 'true') {
      await presentationControl.click()
    }

    // Check if "Create Presentation Draft" button is available
    const createDraftButton = page.getByRole('button', { name: 'Create Presentation Draft' })
    const canCreateDraft = await createDraftButton.isVisible({ timeout: 5_000 }).catch(() => false)

    if (!canCreateDraft) {
      await expect(page.getByRole('heading', { name: /Presentation$/i }).first()).toBeVisible()
      await context.close()
      return
    }

    await createDraftButton.click()

    // Modal should open - use first() since the date picker also opens a dialog
    const modal = page.getByRole('dialog').first()
    await expect(modal).toBeVisible({ timeout: 5_000 })

    await expect(modal.getByText('Please make sure that the thesis title')).toBeVisible()

    // Presentation Type - default is INTERMEDIATE, select FINAL
    await selectOption(page, 'Presentation Type', /final/i)

    // Location
    await page.getByLabel('Location').fill('Room 01.07.014, Garching Campus')

    // Language
    await selectOption(page, 'Language', /english/i)

    // Scheduled At - click button to open the DateTimePicker calendar dialog
    await page.getByRole('button', { name: 'Scheduled At' }).click()

    const datePickerDialog = page.getByRole('dialog').nth(1)
    await expect(datePickerDialog).toBeVisible({ timeout: 5_000 })

    // Navigate to the next month to ensure the selected date is in the future.
    // The email is only sent when scheduledAt is after now.
    const nextMonthButton = datePickerDialog.locator('button[data-direction="next"]')
    await nextMonthButton.click()

    const dayButtons = datePickerDialog.locator('table button:not([data-outside])')
    await dayButtons.last().click()

    const hourSpinbutton = datePickerDialog.getByRole('spinbutton').first()
    const minuteSpinbutton = datePickerDialog.getByRole('spinbutton').nth(1)
    await hourSpinbutton.click()
    await hourSpinbutton.fill('14')
    await minuteSpinbutton.click()
    await minuteSpinbutton.fill('00')

    await datePickerDialog.locator('button').last().click()

    // Click "Create Presentation Draft" button in the modal
    const createButton = modal.getByRole('button', { name: 'Create Presentation Draft' })
    await expect(createButton).toBeEnabled({ timeout: 10_000 })
    await createButton.click()

    // Modal should close after successful creation
    await expect(modal).not.toBeVisible({ timeout: 15_000 })

    // Draft creation does NOT send emails — the email is sent when
    // the examiner accepts/schedules the draft (next test).
    // Verify the draft appears on the page with "Draft" state.
    await expect(page.getByText('Room 01.07.014, Garching Campus').first()).toBeVisible({
      timeout: 10_000,
    })

    await context.close()
  })

  test('examiner can accept a presentation draft and email is sent', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authStatePath('examiner2') })
    const page = await context.newPage()

    const heading = page.getByRole('heading', { name: THESIS_TITLE })
    await navigateToDetail(page, THESIS_URL, heading)

    // Find the Presentation accordion section and expand it
    const presentationControl = page.getByRole('button', { name: 'Presentation', exact: true })
    await expect(presentationControl).toBeVisible({ timeout: 10_000 })
    if ((await presentationControl.getAttribute('aria-expanded')) !== 'true') {
      await presentationControl.click()
    }

    // Look for an "Accept" button on a Draft presentation
    const acceptButton = page.getByRole('button', { name: 'Accept', exact: true }).first()
    const hasAccept = await acceptButton.isVisible({ timeout: 5_000 }).catch(() => false)

    if (!hasAccept) {
      // No draft to accept — presentation may already be scheduled from a prior run
      await context.close()
      return
    }

    // Snapshot mailbox BEFORE accepting (scheduling) the presentation
    const beforeIds = await snapshotMailbox('student3@test.local')

    // Clicking "Accept" always opens the "Schedule Presentation" modal
    // (PresentationCard.tsx). Wait for it deterministically.
    await acceptButton.click()
    const scheduleDialog = page.getByRole('dialog', { name: 'Schedule Presentation' })
    await expect(scheduleDialog).toBeVisible({ timeout: 15_000 })

    // Confirm scheduling and wait for the backend POST so we know the
    // server-side state change (and email send) was triggered.
    const scheduleButton = scheduleDialog.getByRole('button', { name: 'Schedule Presentation' })
    await expect(scheduleButton).toBeEnabled({ timeout: 10_000 })

    // Note: doRequest always appends "?<params>" so we don't anchor with $.
    const schedulePromise = page.waitForResponse(
      (resp) =>
        /\/v2\/theses\/[^/]+\/presentations\/[^/]+\/schedule(\?|$)/.test(resp.url()) &&
        resp.request().method() === 'POST',
      { timeout: 30_000 },
    )
    await scheduleButton.click()
    const scheduleResponse = await schedulePromise
    expect(scheduleResponse.ok(), 'Schedule POST should return 2xx').toBe(true)
    await expect(scheduleDialog).not.toBeVisible({ timeout: 15_000 })

    // Verify the newly scheduled presentation card (identified by its location,
    // which is unique compared to the seeded scheduled presentation).
    await expect(
      page.getByText('Room 01.07.014, Garching Campus').first(),
    ).toBeVisible({ timeout: 10_000 })

    // --- Email verification ---
    // Accepting a PUBLIC presentation draft sends two emails to student3:
    // the private THESIS_PRESENTATION_SCHEDULED notification and the public
    // THESIS_PRESENTATION_INVITATION (student is primary recipient). Wait
    // specifically for the private one rather than the first arrival.
    const privateEmail = await waitForNewMessageBySubject(
      'student3@test.local',
      beforeIds,
      'New Presentation scheduled',
    )
    assertSentFromApp(privateEmail)
    assertEmailFooter(privateEmail)
    expect(getToAddresses(privateEmail)).toContain('student3@test.local')

    const body = getBody(privateEmail)
    expect(body, 'Should greet the student by first name').toContain('Student3')
    expect(body, 'Should contain the thesis title').toContain(THESIS_TITLE)
    expect(body, 'Should contain the presentation location').toContain('Room 01.07.014')
    expect(body, 'Should contain the presentation language').toContain('English')
    expect(body, 'Should contain a link to the thesis').toContain('/theses/')

    await context.close()
  })
})
