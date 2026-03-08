import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Authentication - Unauthenticated', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('protected route redirects to Keycloak login', async ({ page }) => {
    await page.goto('/dashboard')

    // Should redirect to Keycloak login page
    await expect(page).toHaveURL(/\/realms\/thesis-management\//)
    await expect(page.locator('#kc-login')).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('#username')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible()
  })

  test('public landing page is accessible without login', async ({ page }) => {
    await navigateTo(page, '/')

    await expect(page.getByText('Find a Thesis Topic')).toBeVisible()
    // Should show Login button in header, not user menu
    await expect(page.locator('header').getByText('Login')).toBeVisible()
  })

  test('about page is accessible without login', async ({ page }) => {
    await navigateTo(page, '/about')

    await expect(
      page.getByRole('heading', { name: 'Thesis Management', exact: true }),
    ).toBeVisible()
  })
})

test.describe('Authentication - Student', () => {
  test('can access dashboard and sees correct nav items', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    // Student should see these nav items
    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Submit Application' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Browse Theses' })).toBeVisible()
    // Student should NOT see management nav items
    await expect(page.getByRole('link', { name: 'Review Applications' })).toBeHidden()
    await expect(page.getByRole('link', { name: 'Manage Topics' })).toBeHidden()
    await expect(page.getByRole('link', { name: 'Theses Overview' })).toBeHidden()
    await expect(page.getByRole('link', { name: 'Research Groups' })).toBeHidden()
    await expect(page.getByRole('link', { name: 'Interviews' })).toBeHidden()
  })

  test('header shows user menu when authenticated', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    // Header should show app title and user avatar (not Login button)
    await expect(page.getByText('Thesis Management').first()).toBeVisible()
    await expect(page.locator('header').getByText('Login')).toBeHidden()

    // Footer links should be visible on authenticated pages too
    await expect(page.getByText('About')).toBeVisible()
    await expect(page.getByText('Imprint')).toBeVisible()
    await expect(page.getByText('Privacy')).toBeVisible()
  })
})

test.describe('Authentication - Supervisor', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('sees management nav items but not admin items', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    // Supervisor should see management items
    await expect(page.getByRole('link', { name: 'Review Applications' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Manage Topics' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Theses Overview' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Interviews' })).toBeVisible()
    // Supervisor should NOT see Submit Application (hidden from supervisor/examiner)
    await expect(page.getByRole('link', { name: 'Submit Application' })).toBeHidden()
    // Supervisor should NOT see admin-only items
    await expect(page.getByRole('link', { name: 'Research Groups' })).toBeHidden()
  })
})

test.describe('Authentication - Examiner', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('sees management nav items but not admin items', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    // Examiner should see management items
    await expect(page.getByRole('link', { name: 'Review Applications' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Manage Topics' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Theses Overview' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Interviews' })).toBeVisible()
    // Examiner should NOT see Submit Application
    await expect(page.getByRole('link', { name: 'Submit Application' })).toBeHidden()
    // Examiner should NOT see admin-only items
    await expect(page.getByRole('link', { name: 'Research Groups' })).toBeHidden()
  })
})

test.describe('Authentication - Admin', () => {
  test.use({ storageState: authStatePath('admin') })

  test('sees all nav items including Research Groups', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Review Applications' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Manage Topics' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Browse Theses' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Theses Overview' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Research Groups' })).toBeVisible()

    // Admin should NOT see Submit Application (not a student)
    await expect(page.getByRole('link', { name: 'Submit Application' })).toBeHidden()
  })
})
