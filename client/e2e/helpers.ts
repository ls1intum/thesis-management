import { Page, expect } from '@playwright/test'

/**
 * Navigate to a page and wait for it to fully load.
 * Waits for the Mantine Loader spinner to disappear.
 */
export async function navigateTo(page: Page, path: string) {
  await page.goto(path, { waitUntil: 'domcontentloaded', timeout: 30_000 })
  await page
    .locator('.mantine-Loader-root')
    .waitFor({ state: 'hidden', timeout: 30_000 })
    .catch(() => {
      // Loader may never appear if the page loads instantly
    })
}

/**
 * Use a specific auth state file for a test.
 */
export function authStatePath(
  role: 'student' | 'student2' | 'student3' | 'advisor' | 'supervisor' | 'admin',
): string {
  return `e2e/.auth/${role}.json`
}

/**
 * Type text into a TipTap/ProseMirror rich text editor identified by its label.
 */
export async function fillRichTextEditor(page: Page, label: string, text: string) {
  const wrapper = page.locator(`.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("${label}"))`)
  const editor = wrapper.locator('.ProseMirror')
  await editor.click()
  await page.keyboard.type(text)
}

/**
 * Select a value from a Mantine Select/ComboBox identified by its label.
 * Uses getByRole('textbox') to avoid matching the listbox element.
 */
export async function selectOption(page: Page, label: string, optionText: string | RegExp) {
  await page.getByRole('textbox', { name: label }).click()
  await page.getByRole('option', { name: optionText }).click()
}

/**
 * Click a Mantine MultiSelect input. Uses force:true to bypass the wrapper
 * div that intercepts pointer events.
 */
export async function clickMultiSelect(page: Page, label: string) {
  await page.getByRole('textbox', { name: label }).click({ force: true })
}

/**
 * Select an option from a UserMultiSelect (server-side search).
 * Opens the dropdown, waits for options to load, then selects the option.
 * Uses evaluate to dispatch a full mouse event chain since Playwright's
 * built-in click doesn't always trigger Mantine's React event handlers
 * in portal-rendered combobox dropdowns.
 */
export async function searchAndSelectMultiSelect(
  page: Page,
  label: string,
  optionPattern: RegExp,
) {
  const textbox = page.getByRole('textbox', { name: label })
  await textbox.click({ force: true })
  const listbox = page.getByRole('listbox', { name: label })
  const option = listbox.getByRole('option', { name: optionPattern }).first()
  await expect(option).toBeVisible({ timeout: 10_000 })
  // First attempt: standard Playwright click
  await option.click()
  await page.waitForTimeout(500)
  // Check if selection registered by looking for a pill
  const wrapper = page.locator(`.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("${label}"))`)
  const hasPill = await wrapper.locator('.mantine-Pill-root').count() > 0
  if (!hasPill) {
    // Fallback: re-open dropdown and use evaluate to dispatch mouse events
    await textbox.click({ force: true })
    const retryOption = listbox.getByRole('option', { name: optionPattern }).first()
    await expect(retryOption).toBeVisible({ timeout: 10_000 })
    await retryOption.evaluate((el) => {
      el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }))
      el.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }))
      el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }))
    })
    await page.waitForTimeout(500)
  }
  // Verify selection registered
  await expect(wrapper.locator('.mantine-Pill-root')).toBeVisible({ timeout: 5_000 })
}

/**
 * Create a minimal valid PDF buffer for file upload tests.
 */
export function createTestPdfBuffer(): Buffer {
  return Buffer.from(
    '%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n' +
      '2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n' +
      '3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\nendobj\n' +
      'xref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n' +
      'trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n206\n%%EOF',
  )
}
