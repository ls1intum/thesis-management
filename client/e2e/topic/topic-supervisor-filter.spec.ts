import { test, expect } from '@playwright/test'
import { navigateTo } from '../helpers'

// Seed data (see server/.../seed_dev_test_data.sql):
//   Topic 1 "Automated Code Review"        supervisor = Supervisor User
//   Topic 2 "Continuous Integration ..."   supervisors = Supervisor User + Supervisor2 User
//   Topic 3 "Anomaly Detection ..."        supervisor = Supervisor2 User

test.describe('Topics - Supervisor URL filter', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('?supervisor= query param filters to matching supervisor only', async ({ page }) => {
    await navigateTo(page, '/?supervisor=Supervisor%20User')

    await expect(page.getByText(/Topics supervised by Supervisor User/i)).toBeVisible({
      timeout: 15_000,
    })

    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText(/Anomaly Detection/i)).toBeHidden({ timeout: 5_000 })
  })

  test('/supervisor/:name path route filters the same way', async ({ page }) => {
    await navigateTo(page, '/supervisor/Supervisor%20User')

    await expect(page.getByText(/Topics supervised by Supervisor User/i)).toBeVisible({
      timeout: 15_000,
    })

    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText(/Anomaly Detection/i)).toBeHidden({ timeout: 5_000 })
  })

  test('filtering by a different supervisor swaps the visible topics', async ({ page }) => {
    await navigateTo(page, '/supervisor/Supervisor2%20User')

    await expect(page.getByText(/Topics supervised by Supervisor2 User/i)).toBeVisible({
      timeout: 15_000,
    })

    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText(/Automated Code Review/i)).toBeHidden({ timeout: 5_000 })
  })

  test('supervisor filter is case-insensitive', async ({ page }) => {
    await navigateTo(page, '/?supervisor=supervisor%20user')

    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(/Anomaly Detection/i)).toBeHidden({ timeout: 5_000 })
  })

  test('unknown supervisor name shows no topics', async ({ page }) => {
    await navigateTo(page, '/supervisor/Nobody%20Here')

    await expect(page.getByText(/Topics supervised by Nobody Here/i)).toBeVisible({
      timeout: 15_000,
    })

    // None of the seeded topics should be visible
    await expect(page.getByText(/Automated Code Review/i)).toBeHidden({ timeout: 5_000 })
    await expect(page.getByText(/Continuous Integration/i)).toBeHidden({ timeout: 5_000 })
    await expect(page.getByText(/Anomaly Detection/i)).toBeHidden({ timeout: 5_000 })
  })

  test('without supervisor filter, all open topics are visible', async ({ page }) => {
    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/Continuous Integration/i).first()).toBeVisible({ timeout: 5_000 })
    await expect(page.getByText(/Anomaly Detection/i).first()).toBeVisible({ timeout: 5_000 })
  })
})
