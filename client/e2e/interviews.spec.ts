import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Interviews - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

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
  })

  test('interview process detail page loads', async ({ page }) => {
    // UUID matches the active interview process seeded in seed_dev_test_data.sql
    await navigateTo(page, '/interviews/00000000-0000-4000-e600-000000000001')

    await expect(page.getByRole('heading', { name: /interview management/i })).toBeVisible({
      timeout: 30_000,
    })
    // Should show sections for slots and interviewees
    await expect(page.getByRole('heading', { name: 'Interviewees', exact: true })).toBeVisible()
  })
})

test.describe('Interviews - Advisor', () => {
  test.use({ storageState: authStatePath('advisor') })

  test('interview overview is accessible', async ({ page }) => {
    await navigateTo(page, '/interviews')

    await expect(page.getByRole('heading', { name: 'Interviews', exact: true })).toBeVisible({
      timeout: 15_000,
    })
    await expect(page.getByText(/interview topics/i)).toBeVisible()
  })
})

test.describe('Interviews - Student cannot access', () => {
  test.use({ storageState: authStatePath('student') })

  test('student is redirected away from interviews page', async ({ page }) => {
    await navigateTo(page, '/interviews')

    // Student should not see the interviews management page
    // They should be redirected or see access denied
    await expect(page.getByRole('heading', { name: 'Interviews', exact: true })).toBeHidden()
  })
})
