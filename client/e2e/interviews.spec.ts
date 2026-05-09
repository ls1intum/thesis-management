import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Interviews - Examiner', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('interview overview shows interview topics and upcoming interviews', async ({ page }) => {
    await navigateTo(page, '/interviews')

    await expect(page.getByRole('heading', { name: 'Interviews', exact: true })).toBeVisible({
      timeout: 30_000,
    })
    // Should show the two panels
    await expect(page.getByText(/interview topics/i)).toBeVisible()
    await expect(
      page.getByRole('heading', { name: 'Upcoming Interviews', exact: true }),
    ).toBeVisible()

    // Should show seeded interview topics — examiner heads ASE group with completed process 2
    // Topic 1 (LLM Code Review) has a completed interview process
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    // Topic 2 (CI Pipeline) has completed process 3
    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 5_000 })

    // Verify "New Interview Process" button is available for examiner
    await expect(page.getByRole('button', { name: /New Interview Process/i })).toBeVisible()
  })

  test('interview process detail page loads with expected sections', async ({ page }) => {
    // UUID matches the active interview process seeded in seed_dev_test_data.sql (Topic 3, DSA)
    await navigateTo(page, '/interviews/00000000-0000-4000-e600-000000000001')

    await expect(page.getByRole('heading', { name: /interview management/i })).toBeVisible({
      timeout: 30_000,
    })
    // Should show sections for slots and interviewees
    await expect(page.getByRole('heading', { name: 'Interviewees', exact: true })).toBeVisible()

    // Should show interviewees section with filter tabs
    await expect(page.getByText(/All/i).first()).toBeVisible()
  })
})

test.describe('Interviews - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('interview overview is accessible with expected layout', async ({ page }) => {
    await navigateTo(page, '/interviews')

    await expect(page.getByRole('heading', { name: 'Interviews', exact: true })).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.getByText(/interview topics/i)).toBeVisible()
    await expect(
      page.getByRole('heading', { name: 'Upcoming Interviews', exact: true }),
    ).toBeVisible()
  })
})

test.describe('Interviews - Examiner2 (DSA group head)', () => {
  test.use({ storageState: authStatePath('examiner2') })

  test('examiner2 sees their active interview process for anomaly detection topic', async ({
    page,
  }) => {
    await navigateTo(page, '/interviews')

    await expect(page.getByRole('heading', { name: 'Interviews', exact: true })).toBeVisible({
      timeout: 15_000,
    })

    // Examiner2 heads DSA group — Topic 3 (Anomaly Detection) has active interview process 1
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Interviews - Student cannot access', () => {
  test.use({ storageState: authStatePath('student') })

  test('student is redirected away from interviews page', async ({ page }) => {
    await navigateTo(page, '/interviews')

    // Student should not see the interviews management page
    await expect(page.getByRole('heading', { name: 'Interviews', exact: true })).toBeHidden()
  })
})
