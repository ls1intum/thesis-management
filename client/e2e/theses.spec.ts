import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo } from './helpers'

test.describe('Theses - Browse (Student)', () => {
  test('browse theses page shows heading and table', async ({ page }) => {
    await navigateTo(page, '/theses')

    await expect(page.getByRole('heading', { name: /browse theses/i })).toBeVisible({ timeout: 15_000 })
    // Student should NOT see create thesis button
    await expect(page.getByRole('button', { name: /create/i })).toBeHidden()
  })
})

test.describe('Theses - Browse (Supervisor)', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('browse theses page shows create button for management', async ({ page }) => {
    await navigateTo(page, '/theses')

    await expect(page.getByRole('heading', { name: /browse theses/i })).toBeVisible({ timeout: 15_000 })
    // Supervisor should see create thesis button
    await expect(page.getByRole('button', { name: /create/i }).first()).toBeVisible()
  })
})

test.describe('Theses - Overview (Supervisor)', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('theses overview shows management view', async ({ page }) => {
    await navigateTo(page, '/overview')

    await expect(page.getByRole('heading', { name: /theses overview/i })).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Theses - Detail page (Advisor)', () => {
  test.use({ storageState: authStatePath('advisor') })

  test('thesis detail shows sections for WRITING thesis', async ({ page }) => {
    // Thesis 1: WRITING state, "Automated Code Review Using LLMs"
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')

    // Title
    await expect(
      page.getByRole('heading', { name: /automated code review/i }),
    ).toBeVisible({ timeout: 15_000 })

    // Key thesis sections should be visible (accordion sections)
    // Key thesis sections should be visible (accordion sections)
    await expect(page.getByText('Configuration')).toBeVisible()
    await expect(page.getByText('Involved Persons')).toBeVisible()
  })
})

test.describe('Theses - Detail page (Student)', () => {
  test('student can view their own thesis', async ({ page }) => {
    // Student is assigned to thesis 1 (WRITING)
    await navigateTo(page, '/theses/00000000-0000-4000-d000-000000000001')

    await expect(
      page.getByRole('heading', { name: /automated code review/i }),
    ).toBeVisible({ timeout: 15_000 })
  })
})

test.describe('Theses - Dashboard integration', () => {
  test.use({ storageState: authStatePath('supervisor') })

  test('supervisor dashboard shows My Theses section', async ({ page }) => {
    await navigateTo(page, '/dashboard')

    await expect(page.getByRole('heading', { name: /dashboard/i })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('heading', { name: /my theses/i })).toBeVisible()
  })
})
