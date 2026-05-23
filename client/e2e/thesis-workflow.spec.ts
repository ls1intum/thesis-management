import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, searchAndSelectMultiSelect, selectOption } from './helpers'
import {
  snapshotMailbox,
  waitForNewMessages,
  getSubject,
  getBody,
  getToAddresses,
  assertSentFromApp,
  assertEmailFooter,
} from './mailpit'

test.describe('Thesis Workflow - Examiner creates a thesis', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner can create a new thesis via the browse theses page', async ({ page }) => {
    test.setTimeout(120_000) // Extended timeout — form with server-side search fields

    await navigateTo(page, '/theses')
    await expect(page.getByRole('heading', { name: 'Browse Theses', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    // Click "Create Thesis" button
    await page.getByRole('button', { name: 'Create Thesis' }).click()

    // Modal should open
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })

    // Fill in the thesis creation form
    await page.getByLabel('Thesis Title').fill('E2E Test Thesis: Performance Analysis')

    // Thesis Type
    await selectOption(page, 'Thesis Type', /master/i)

    // Language
    await selectOption(page, 'Thesis Language', /english/i)

    // Student(s) - search and select
    await searchAndSelectMultiSelect(page, 'Student(s)', /student4/i)

    // Supervisor(s) - search and select supervisor
    await searchAndSelectMultiSelect(page, 'Supervisor(s)', /supervisor/i)

    // Examiner - search and select self (examiner)
    await searchAndSelectMultiSelect(page, 'Examiner', /examiner/i)

    // Snapshot mailbox BEFORE creating the thesis
    const beforeIds = await snapshotMailbox('student4@test.local')

    // Click "Create Thesis"
    const createButton = page.getByRole('dialog').getByRole('button', { name: 'Create Thesis' })
    await expect(createButton).toBeEnabled({ timeout: 15_000 })
    await createButton.click()

    // Should navigate to the new thesis detail page
    await expect(page).toHaveURL(/\/theses\//, { timeout: 15_000 })
    await expect(
      page.getByRole('heading', { name: 'E2E Test Thesis: Performance Analysis' }),
    ).toBeVisible({ timeout: 15_000 })

    // --- Email verification ---
    // THESIS_CREATED is sent to the thesis students (THESIS_CREATED template)
    const newEmails = await waitForNewMessages('student4@test.local', beforeIds)
    expect(newEmails.length).toBeGreaterThanOrEqual(1)

    const creationEmail = newEmails.find((e) => getSubject(e) === 'Thesis Created')
    expect(creationEmail, 'Thesis creation email should be sent').toBeDefined()
    assertSentFromApp(creationEmail!)
    assertEmailFooter(creationEmail!)
    expect(getToAddresses(creationEmail!)).toContain('student4@test.local')

    // Body should contain: greeting, thesis title, examiner/supervisor/student names, and link
    const body = getBody(creationEmail!)
    expect(body, 'Should greet the student by first name').toContain('Student4')
    expect(body, 'Should contain the thesis title').toContain(
      'E2E Test Thesis: Performance Analysis',
    )
    expect(body, 'Should contain a link to the thesis').toContain('/theses/')
    // The template shows "Examiner:", "Supervisor:", "Student:" sections
    expect(body, 'Should mention the examiner name').toContain('Examiner')
    expect(body, 'Should reference proposal as next step').toContain('proposal')
  })
})

test.describe('Create Thesis Modal - Examiner prefill displays head name (regression)', () => {
  // examiner2 is the head of the DSA group and is the only listed user in DSA,
  // so opening the Create Thesis modal triggers the single-group prefill path
  // that the bug originally affected.
  test.use({ storageState: authStatePath('examiner2') })

  test('auto-prefilled examiner shows head name, not the raw user ID', async ({ page }) => {
    test.setTimeout(60_000)

    await navigateTo(page, '/theses')
    await expect(page.getByRole('heading', { name: 'Browse Theses', exact: true })).toBeVisible({
      timeout: 30_000,
    })

    await page.getByRole('button', { name: 'Create Thesis' }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible({ timeout: 5_000 })

    // Research group must be auto-prefilled — that's the trigger for the
    // examiner prefill we're verifying here.
    const researchGroupInput = dialog.getByRole('combobox', { name: 'Research Group' })
    await expect(researchGroupInput).not.toHaveValue('', { timeout: 10_000 })

    // The Examiner field should have exactly one pill rendered with the head's
    // human-readable label, not the raw UUID. Without `initialUsers` the pill
    // falls back to the bare user ID (the bug from #952).
    const examinerWrapper = dialog.locator(
      '.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("Examiner"))',
    )
    const pill = examinerWrapper.locator('.mantine-Pill-root').first()
    await expect(pill).toBeVisible({ timeout: 10_000 })

    const pillText = (await pill.innerText()).trim()
    expect(pillText, `Examiner pill should not display a raw UUID, got: ${pillText}`).not.toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
    )
    expect(pillText, `Examiner pill should contain the head's name, got: ${pillText}`).toMatch(
      /Examiner2/i,
    )
  })
})
