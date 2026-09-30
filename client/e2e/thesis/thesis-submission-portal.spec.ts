import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, navigateToThesisConfig } from '../helpers'

// Dedicated seed theses (not shared with other specs, this spec changes the study program of one):
// thesis 30 (student5, Management and Technology -> School of Management, portal.mgt.tum.de),
// thesis 31 (student3, Games Engineering -> CIT, portal.cit.tum.de).
const THESIS_MGT = '00000000-0000-4000-d000-000000000030'
const THESIS_CIT = '00000000-0000-4000-d000-000000000031'

const MGT_PORTAL = 'https://portal.mgt.tum.de/'
const CIT_PORTAL = 'https://portal.cit.tum.de/'

// The supervisor test changes the study program of THESIS_CIT, which the student test reads. The config
// enables fullyParallel, so run the tests of this file one after another.
test.describe.configure({ mode: 'default' })

test.describe('Thesis submission portal - student', () => {
  test.describe('School of Management', () => {
    test.use({ storageState: authStatePath('student5') })

    test('links to the portal of the school of the study program', async ({ page }) => {
      await navigateTo(page, `/theses/${THESIS_MGT}`)

      const reminder = page
        .getByRole('alert')
        .filter({ hasText: 'not the official submission website' })
      await expect(reminder.getByRole('link', { name: 'here' })).toHaveAttribute(
        'href',
        MGT_PORTAL,
        {
          timeout: 30_000,
        },
      )
    })
  })

  test.describe('School of Computation, Information and Technology', () => {
    test.use({ storageState: authStatePath('student3') })

    test('links to the CIT portal', async ({ page }) => {
      await navigateTo(page, `/theses/${THESIS_CIT}`)

      const reminder = page
        .getByRole('alert')
        .filter({ hasText: 'not the official submission website' })
      await expect(reminder.getByRole('link', { name: 'here' })).toHaveAttribute(
        'href',
        CIT_PORTAL,
        {
          timeout: 30_000,
        },
      )
    })
  })
})

test.describe('Thesis submission portal - supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('changing the study program of a thesis changes the portal link', async ({ page }) => {
    test.setTimeout(120_000)

    await navigateToThesisConfig(page, THESIS_CIT)

    const studyProgram = page.getByRole('combobox', { name: 'Study Program' })
    await expect(studyProgram).toHaveValue('Games Engineering', { timeout: 15_000 })
    await expect(page.getByRole('link', { name: CIT_PORTAL })).toBeVisible()

    try {
      // an unsaved edit of another field must survive the separate save of the study program
      const title = page.getByLabel('Thesis Title')
      const unsavedTitle = `${await title.inputValue()} (unsaved edit)`
      await title.fill(unsavedTitle)

      await studyProgram.click()
      await page.getByRole('option', { name: 'Management and Technology' }).click()

      await expect(page.getByText('Study program updated successfully')).toBeVisible()
      await expect(studyProgram).toHaveValue('Management and Technology')
      await expect(page.getByRole('link', { name: MGT_PORTAL })).toBeVisible()
      await expect(title).toHaveValue(unsavedTitle)
    } finally {
      // restore the seeded state so other specs (and retries) see the original thesis
      await studyProgram.click()
      await page.getByRole('option', { name: 'Games Engineering' }).click()
      await expect(studyProgram).toHaveValue('Games Engineering')
      await expect(page.getByRole('link', { name: CIT_PORTAL })).toBeVisible()
    }
  })
})
