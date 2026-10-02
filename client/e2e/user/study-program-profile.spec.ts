import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from '../helpers'

// Seed: student4 studies "Other".
test.describe('Study program in the profile', () => {
  test.use({ storageState: authStatePath('student4') })

  test('student can pick a study program from the configured list and it is stored', async ({
    page,
  }) => {
    test.setTimeout(120_000)

    await navigateTo(page, '/settings')
    await expect(page.getByText('My Information')).toBeVisible({ timeout: 15_000 })

    const studyProgram = page.getByRole('combobox', { name: 'Study Program', exact: true })
    await expect(studyProgram).toHaveValue('Other', { timeout: 15_000 })

    const save = async () => {
      const privacyCheckbox = page.getByRole('checkbox', { name: /privacy/i })
      if (!(await privacyCheckbox.isChecked())) {
        await privacyCheckbox.check()
      }
      await page.getByRole('button', { name: 'Update Information', exact: true }).click()
      await expect(page.getByText('You successfully updated your profile')).toBeVisible({
        timeout: 15_000,
      })
    }

    try {
      await studyProgram.click()
      // options are grouped by school
      await expect(
        page.getByText('TUM School of Computation, Information and Technology'),
      ).toBeVisible()
      await page.getByRole('option', { name: 'Information Systems' }).click()
      await save()

      await page.reload()
      await expect(page.getByRole('combobox', { name: 'Study Program', exact: true })).toHaveValue(
        'Information Systems',
        {
          timeout: 30_000,
        },
      )
    } finally {
      await page.getByRole('combobox', { name: 'Study Program', exact: true }).click()
      await page.getByRole('option', { name: 'Other', exact: true }).click()
      await save()
    }
  })
})
