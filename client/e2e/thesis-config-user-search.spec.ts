import { test, expect } from '@playwright/test'
import { authStatePath, expandAccordion, navigateTo } from './helpers'

test.describe('Thesis Configuration - User search filters by role', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('student selector shows students and not supervisors or examiners', async ({ page }) => {
    test.setTimeout(120_000)

    // Navigate to thesis 1 (WRITING state, supervisor is assigned)
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 15_000,
    })

    // Open the Configuration accordion (retry under heavy parallel load)
    const studentTextbox = page.getByRole('textbox', { name: 'Student(s)' })
    await expandAccordion(page, 'Configuration', studentTextbox)

    const studentListbox = page.getByRole('listbox', { name: 'Student(s)' })

    // Open the student dropdown with retry loop (server may be slow under parallel load).
    // Each dropdown open triggers a new fetch via onDropdownOpen; close and reopen
    // between attempts so the event fires again.
    let found = false
    for (let attempt = 0; attempt < 5 && !found; attempt++) {
      if (attempt > 0) {
        // Close the dropdown before retrying so onDropdownOpen fires again
        await page.keyboard.press('Tab')
        await page.waitForTimeout(500)
      }
      await studentTextbox.click({ force: true })
      found = await studentListbox
        .getByRole('option')
        .first()
        .isVisible({ timeout: 20_000 })
        .catch(() => false)
    }
    await expect(studentListbox.getByRole('option').first()).toBeVisible({ timeout: 10_000 })

    // Verify students appear in the dropdown (student2-5 are available; student is already selected as a pill)
    await expect(studentListbox.getByRole('option', { name: /student2/i })).toBeVisible()
    await expect(studentListbox.getByRole('option', { name: /student3/i })).toBeVisible()

    // Verify student4 and student5 also appear (all student-role users should be listed)
    await expect(studentListbox.getByRole('option', { name: /student4/i })).toBeVisible()
    await expect(studentListbox.getByRole('option', { name: /student5/i })).toBeVisible()

    // Verify supervisors and examiners do NOT appear in the student dropdown
    await expect(studentListbox.getByRole('option', { name: /^Examiner/i })).toHaveCount(0)
    await expect(studentListbox.getByRole('option', { name: /^Supervisor/i })).toHaveCount(0)

    // Close dropdown via Tab (do NOT use Escape — it closes Mantine accordions/modals)
    await page.keyboard.press('Tab')
  })
})

test.describe('Thesis Configuration - Lazy user fetching', () => {
  test.describe('as student', () => {
    test.use({ storageState: authStatePath('student') })

    test('no /v2/users requests are made when loading the thesis page', async ({ page }) => {
      test.setTimeout(60_000)

      // Track all requests to /v2/users
      const userRequests: string[] = []
      page.on('request', (req) => {
        if (req.url().includes('/v2/users')) {
          userRequests.push(req.url())
        }
      })

      await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')
      await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
        timeout: 15_000,
      })

      // Open the Configuration accordion to render the UserMultiSelect components
      await expandAccordion(
        page,
        'Configuration',
        page.locator('.mantine-Accordion-panel').filter({ hasText: 'Thesis Title' }),
      )

      // Students should not trigger any /v2/users requests (selects are disabled
      // and lazy fetching skips the initial load)
      expect(userRequests).toHaveLength(0)
    })
  })

  test.describe('as supervisor', () => {
    test.use({ storageState: authStatePath('supervisor') })

    test('no /v2/users requests are made until a dropdown is opened', async ({ page }) => {
      test.setTimeout(120_000)

      // Track all requests to /v2/users
      const userRequests: string[] = []
      page.on('request', (req) => {
        if (req.url().includes('/v2/users')) {
          userRequests.push(req.url())
        }
      })

      await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')
      await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
        timeout: 15_000,
      })

      // Open the Configuration accordion to render the UserMultiSelect components
      await expandAccordion(
        page,
        'Configuration',
        page.getByRole('textbox', { name: 'Student(s)' }),
      )

      // No /v2/users requests should have been made yet (lazy fetching)
      expect(userRequests).toHaveLength(0)

      // Now open the Student(s) dropdown — this should trigger exactly one fetch
      const studentTextbox = page.getByRole('textbox', { name: 'Student(s)' })
      const studentListbox = page.getByRole('listbox', { name: 'Student(s)' })

      await studentTextbox.click({ force: true })
      await studentListbox
        .getByRole('option')
        .first()
        .isVisible({ timeout: 20_000 })
        .catch(() => false)

      // Exactly one /v2/users request should have been made for the Student(s) dropdown
      const studentUserRequests = userRequests.filter((url) => url.includes('groups=student'))
      expect(studentUserRequests).toHaveLength(1)

      // Close dropdown
      await page.keyboard.press('Tab')
    })
  })
})
