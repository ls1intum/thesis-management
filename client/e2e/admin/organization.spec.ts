import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from '../helpers'

// Seed: schools CIT and MGT with departments; study programs of the five legacy keys; research group
// ASE (…a000-…01) belongs to CIT / Computer Science.
const ASE_GROUP_ID = '00000000-0000-4000-a000-000000000001'

test.describe('Organization - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('lists the seeded schools with departments and portals', async ({ page }) => {
    await navigateTo(page, '/admin/organization')

    await expect(page.getByRole('heading', { name: 'Organization' })).toBeVisible({
      timeout: 30_000,
    })
    const mgt = page.getByTestId('school-MGT')
    await expect(mgt).toBeVisible()
    await expect(page.getByTestId('school-CIT')).toBeVisible()

    await mgt.getByText('TUM School of Management').click()
    await expect(mgt.getByRole('link', { name: 'https://portal.mgt.tum.de/' })).toBeVisible()
    await expect(mgt.getByText('Operations and Technology')).toBeVisible()
  })

  test('lists the study programs and filters them', async ({ page }) => {
    await navigateTo(page, '/admin/organization')
    await page.getByRole('tab', { name: 'Study Programs' }).click()

    await expect(page.getByRole('cell', { name: 'Computer Science', exact: true })).toBeVisible({
      timeout: 30_000,
    })
    await expect(
      page.getByRole('cell', { name: 'Management and Technology', exact: true }),
    ).toBeVisible()

    await page.getByPlaceholder('Search study programs...').fill('management')
    await expect(page.getByRole('cell', { name: 'Computer Science', exact: true })).toBeHidden()
    await expect(
      page.getByRole('cell', { name: 'Management and Technology', exact: true }),
    ).toBeVisible()
  })

  test('admin can create, edit and delete a school, department and study program', async ({
    page,
  }) => {
    const suffix = Date.now().toString(36)
    const schoolName = `E2E School ${suffix}`
    const abbreviation = `E${suffix}`.slice(0, 10).toUpperCase()
    const departmentName = `E2E Department ${suffix}`
    const programName = `E2E Program ${suffix}`

    await navigateTo(page, '/admin/organization')
    await expect(page.getByRole('button', { name: 'Add School' })).toBeVisible({ timeout: 30_000 })

    // school with an invalid portal is rejected on the client
    await page.getByRole('button', { name: 'Add School' }).click()
    let dialog = page.getByRole('dialog', { name: 'Add School' })
    await dialog.getByLabel('Name').fill(schoolName)
    await dialog.getByLabel('Abbreviation').fill(abbreviation)
    await dialog.getByLabel('Thesis portal').fill('javascript:alert(1)')
    await dialog.getByRole('button', { name: 'Create' }).click()
    await expect(dialog.getByText('Please enter a valid http(s) URL')).toBeVisible()

    // valid school
    await dialog.getByLabel('Thesis portal').fill(`https://portal.${suffix}.example.org/`)
    await dialog.getByRole('button', { name: 'Create' }).click()
    await expect(page.getByText('School created')).toBeVisible()

    const school = page.getByTestId(`school-${abbreviation}`)
    await expect(school).toBeVisible()
    await school.getByText(schoolName).click()
    await expect(
      school.getByRole('link', { name: `https://portal.${suffix}.example.org/` }),
    ).toBeVisible()

    // department
    await school.getByRole('button', { name: 'Add Department' }).click()
    dialog = page.getByRole('dialog', { name: /Add Department/ })
    await dialog.getByLabel('Name').fill(departmentName)
    await dialog.getByRole('button', { name: 'Create' }).click()
    await expect(school.getByText(departmentName)).toBeVisible()

    // study program in that school
    await page.getByRole('tab', { name: 'Study Programs' }).click()
    await page.getByRole('button', { name: 'Add Study Program' }).click()
    dialog = page.getByRole('dialog', { name: 'Add Study Program' })
    await dialog.getByLabel('Name').fill(programName)
    await dialog.getByRole('combobox', { name: 'School' }).click()
    await page.getByRole('option', { name: `${schoolName} (${abbreviation})` }).click()
    await dialog.getByRole('button', { name: 'Create' }).click()
    const programRow = page.getByRole('row', { name: new RegExp(programName) })
    await expect(programRow).toBeVisible()
    await expect(programRow.getByText(abbreviation)).toBeVisible()

    // a school that is in use cannot be deleted, the server explains why
    await page.getByRole('tab', { name: 'Schools' }).click()
    await school.getByText(schoolName).click()
    await school.getByRole('button', { name: 'Delete School' }).click()
    await page.getByRole('button', { name: 'Confirm' }).click()
    await expect(
      page.getByText(/still used by departments, study programs or research groups/),
    ).toBeVisible()

    // deactivate the program instead
    await page.getByRole('tab', { name: 'Study Programs' }).click()
    await programRow.getByRole('button', { name: /Edit study program/ }).click()
    dialog = page.getByRole('dialog', { name: 'Edit Study Program' })
    await dialog.getByLabel('Active').uncheck()
    await dialog.getByRole('button', { name: 'Save' }).click()
    await expect(programRow.getByText('Inactive')).toBeVisible()

    // clean up in the order the dependencies require: program, department, then the school
    await programRow.getByRole('button', { name: 'Delete' }).click()
    await page.getByRole('button', { name: 'Confirm' }).click()
    await expect(programRow).toBeHidden()

    await page.getByRole('tab', { name: 'Schools' }).click()
    await school.getByText(schoolName).click()
    await school.getByRole('button', { name: `Delete department ${departmentName}` }).click()
    await page.getByRole('button', { name: 'Confirm' }).click()
    await expect(school.getByText(departmentName)).toBeHidden()

    await school.getByRole('button', { name: 'Delete School' }).click()
    await page.getByRole('button', { name: 'Confirm' }).click()
    await expect(page.getByText('School deleted')).toBeVisible()
    await expect(school).toBeHidden()
  })

  test('the research group form shows school and department of the group', async ({ page }) => {
    await navigateTo(page, `/research-groups/${ASE_GROUP_ID}`)

    await expect(page.getByRole('combobox', { name: 'School' })).toHaveValue(
      'TUM School of Computation, Information and Technology (CIT)',
      { timeout: 30_000 },
    )
    await expect(page.getByRole('combobox', { name: 'Department' })).toHaveValue('Computer Science')
  })
})

test.describe('Organization - Access', () => {
  test.use({ storageState: authStatePath('student') })

  test('students cannot open the administration of the organization', async ({ page }) => {
    await navigateTo(page, '/admin/organization')

    await expect(page.getByText('403 - Unauthorized')).toBeVisible({ timeout: 30_000 })
  })
})
