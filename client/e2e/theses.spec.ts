import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Theses - Browse (Student)', () => {
  test('browse theses page shows heading and table', async ({ page }) => {
    await navigateTo(page, '/theses')

    await expect(page.getByRole('heading', { name: /browse theses/i })).toBeVisible({
      timeout: 15_000,
    })
    // Student should NOT see create thesis button
    await expect(page.getByRole('button', { name: /create/i })).toBeHidden()

    // Should show thesis data from seed — student is assigned to thesis 1 (WRITING)
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })

    // Student is also assigned to thesis 4 (FINISHED) — should see it
    await expect(page.getByText(/Monolith to Microservices/i).first()).toBeVisible({
      timeout: 5_000,
    })
  })
})

test.describe('Theses - Browse (Examiner)', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('browse theses page shows create button and thesis list', async ({ page }) => {
    await navigateTo(page, '/theses')

    await expect(page.getByRole('heading', { name: /browse theses/i })).toBeVisible({
      timeout: 30_000,
    })
    // Examiner should see create thesis button
    await expect(page.getByRole('button', { name: /create/i }).first()).toBeVisible({
      timeout: 10_000,
    })

    // Should show seeded theses assigned to examiner
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(/CI Pipeline Optimization/i).first()).toBeVisible({
      timeout: 5_000,
    })
  })
})

test.describe('Theses - Overview (Examiner)', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('theses overview shows management view with thesis data', async ({ page }) => {
    await navigateTo(page, '/overview')

    await expect(page.getByRole('heading', { name: /theses overview/i })).toBeVisible({
      timeout: 15_000,
    })
    // Should show thesis entries from seed data
    await expect(page.getByText(/Automated Code Review/i).first()).toBeVisible({ timeout: 10_000 })

    // Should also show thesis 2 (PROPOSAL state)
    await expect(page.getByText(/CI Pipeline Optimization/i).first()).toBeVisible({
      timeout: 5_000,
    })
  })
})

test.describe('Theses - Detail page (Supervisor)', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('thesis detail shows all sections for WRITING thesis', async ({ page }) => {
    // Thesis 1: WRITING state, "Automated Code Review Using LLMs"
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')

    // Title
    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 15_000,
    })

    // Key thesis sections should be visible (accordion sections)
    await expect(page.getByText('Configuration')).toBeVisible()
    await expect(page.getByText('Involved Persons')).toBeVisible()

    // Thesis accordion sections should be present for supervisor
    await expect(
      page.getByRole('button', { name: 'Proposal', exact: true }),
    ).toBeVisible()
    await expect(
      page.getByRole('button', { name: 'Presentation', exact: true }),
    ).toBeVisible()
    // Supervisor should see comments section
    await expect(page.getByRole('button', { name: /supervisor comments/i })).toBeVisible()
  })

  test('thesis detail shows PROPOSAL state thesis', async ({ page }) => {
    // Thesis 2: PROPOSAL state, "CI Pipeline Optimization"
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000002')

    await expect(page.getByRole('heading', { name: /CI Pipeline Optimization/i })).toBeVisible({
      timeout: 15_000,
    })

    // Should show thesis sections
    await expect(page.getByText('Configuration')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Proposal', exact: true })).toBeVisible()
  })
})

test.describe('Theses - Detail page (Student)', () => {
  test('student can view their own thesis with expected content', async ({ page }) => {
    // Student is assigned to thesis 1 (WRITING)
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')

    await expect(page.getByRole('heading', { name: /automated code review/i })).toBeVisible({
      timeout: 15_000,
    })

    // Should show thesis accordion sections visible to students
    await expect(page.getByRole('button', { name: 'Configuration' })).toBeVisible()
    await expect(
      page.getByRole('button', { name: 'Proposal', exact: true }),
    ).toBeVisible()
    await expect(
      page.getByRole('button', { name: 'Presentation', exact: true }),
    ).toBeVisible()

    // Student should see the Comments section
    await expect(page.getByRole('button', { name: 'Comments' })).toBeVisible()
  })
})

test.describe('Theses - Detail page (Examiner2)', () => {
  test.use({ storageState: authStatePath('examiner2') })

  test('examiner2 can view thesis 3 (SUBMITTED state)', async ({ page }) => {
    // Thesis 3: SUBMITTED state, "Online Anomaly Detection in IoT Sensor Streams"
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000003')

    await expect(page.getByRole('heading', { name: /anomaly detection/i })).toBeVisible({
      timeout: 15_000,
    })

    // Should show thesis sections for SUBMITTED state
    await expect(page.getByText('Configuration')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Proposal', exact: true })).toBeVisible()
  })
})

test.describe('Theses - Dashboard integration', () => {
  test.use({ storageState: authStatePath('examiner') })

  test('examiner dashboard shows My Theses section with thesis data', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /my theses/i })).toBeVisible()

    // Should show thesis from seed data where examiner is assigned
    await expect(page.getByText(/Automated Code Review/i)).toBeVisible({ timeout: 10_000 })
  })
})
