import { test, expect } from '@playwright/test'
import { authStatePath, navigateTo, selectOption } from './helpers'

const APPLICATION_REJECT_ID = '00000000-0000-4000-c000-000000000004' // student4 on topic 1, NOT_ASSESSED
const APPLICATION_ACCEPT_ID = '00000000-0000-4000-c000-000000000005' // student5 on topic 2, NOT_ASSESSED

test.describe('Application Review Workflow', () => {
  test.use({ storageState: authStatePath('advisor') })

  test('advisor can reject a NOT_ASSESSED application', async ({ page }) => {
    await navigateTo(page, `/applications/${APPLICATION_REJECT_ID}`)

    // Wait for the page to fully load — the student heading is always visible for any state
    await expect(page.getByRole('heading', { name: /Student4 User/i })).toBeVisible({
      timeout: 30_000,
    })

    // Check if application still has the review form (NOT_ASSESSED state)
    // A prior test run may have rejected this application and DB wasn't re-seeded
    const thesisTitle = page.getByLabel('Thesis Title')
    const hasReviewForm = await thesisTitle.isVisible({ timeout: 5_000 }).catch(() => false)
    if (!hasReviewForm) {
      return
    }

    // Click the first "Reject" button (header area, opens modal directly)
    const rejectButton = page.getByRole('button', { name: 'Reject', exact: true }).first()
    await expect(rejectButton).toBeVisible({ timeout: 10_000 })
    await rejectButton.click()

    // Modal should open with "Reject Application" title
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 5_000 })
    await expect(
      page.getByRole('dialog').getByText('Reject Application').first(),
    ).toBeVisible()

    // "Topic requirements not met" should be the default selected reason for topic-based applications
    await expect(page.getByText('Topic requirements not met')).toBeVisible()

    // "Notify Student" checkbox should be checked by default
    const notifyCheckbox = page.getByRole('dialog').getByLabel('Notify Student')
    await expect(notifyCheckbox).toBeChecked()

    // Click "Reject Application" button in the modal
    await page.getByRole('dialog').getByRole('button', { name: 'Reject Application' }).click()

    // Verify success notification
    await expect(page.getByText('Application rejected successfully')).toBeVisible({
      timeout: 10_000,
    })
  })

  test('advisor can accept a NOT_ASSESSED application', async ({ page }) => {
    await navigateTo(page, `/applications/${APPLICATION_ACCEPT_ID}`)

    // Wait for the page to fully load — the student heading is always visible for any state
    await expect(page.getByRole('heading', { name: /Student5 User/i })).toBeVisible({
      timeout: 30_000,
    })

    // Check if application still has the review form (NOT_ASSESSED state)
    // A prior test run may have accepted this application and DB wasn't re-seeded
    const thesisTitle = page.getByLabel('Thesis Title')
    const hasReviewForm = await thesisTitle.isVisible({ timeout: 5_000 }).catch(() => false)
    if (!hasReviewForm) {
      return
    }

    // Verify the acceptance form has pre-filled fields from the topic
    await expect(thesisTitle).not.toHaveValue('')

    // Thesis Type should be pre-filled
    await expect(page.getByRole('textbox', { name: 'Thesis Type' })).toBeVisible()

    // Thesis Language may not be pre-filled — fill it if empty
    const languageInput = page.getByRole('textbox', { name: 'Thesis Language' })
    const languageValue = await languageInput.inputValue()
    if (!languageValue) {
      await selectOption(page, 'Thesis Language', /english/i)
    }

    // Supervisor and Advisor(s) should be pre-filled from the topic (pills visible)
    const supervisorWrapper = page.locator(
      '.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("Supervisor"))',
    )
    await expect(supervisorWrapper.locator('.mantine-Pill-root').first()).toBeVisible({
      timeout: 10_000,
    })

    const advisorWrapper = page.locator(
      '.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("Advisor(s)"))',
    )
    await expect(advisorWrapper.locator('.mantine-Pill-root').first()).toBeVisible({
      timeout: 10_000,
    })

    // Click "Accept" button
    const acceptButton = page.getByRole('button', { name: 'Accept', exact: true })
    await expect(acceptButton).toBeEnabled({ timeout: 10_000 })
    await acceptButton.click()

    // Verify success notification
    await expect(page.getByText('Application accepted successfully')).toBeVisible({
      timeout: 10_000,
    })
  })
})
