import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Thesis Configuration - User search filters by role', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('student selector shows students and not supervisors or examiners', async ({ page }) => {
    test.setTimeout(120_000)

    // Navigate to thesis 1 (WRITING state, supervisor is assigned)
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 15_000,
    })

    // Open the Configuration accordion
    await page.getByText('Configuration').click()

    const studentTextbox = page.getByRole('textbox', { name: 'Student(s)' })
    const studentListbox = page.getByRole('listbox', { name: 'Student(s)' })

    // Open the student dropdown with retry loop (server may be slow under parallel load).
    // Each click triggers a new fetch; wait long enough for the server to respond.
    let found = false
    for (let attempt = 0; attempt < 3 && !found; attempt++) {
      await studentTextbox.click({ force: true })
      found = await studentListbox
        .getByRole('option')
        .first()
        .isVisible({ timeout: 20_000 })
        .catch(() => false)
    }
    await expect(studentListbox.getByRole('option').first()).toBeVisible({ timeout: 5_000 })

    // Verify students appear in the dropdown (student2-5 are available; student is already selected as a pill)
    await expect(studentListbox.getByRole('option', { name: /student2/i })).toBeVisible()

    // Verify supervisors and examiners do NOT appear in the student dropdown
    await expect(studentListbox.getByRole('option', { name: /^Examiner/i })).toHaveCount(0)
    await expect(studentListbox.getByRole('option', { name: /^Supervisor/i })).toHaveCount(0)

    // Close dropdown via Tab (do NOT use Escape — it closes Mantine accordions/modals)
    await page.keyboard.press('Tab')
  })
})
