import { Locator, Page, expect } from '@playwright/test'

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
 * Navigate to an entity detail page (application, thesis) and verify
 * it loaded the detail view. Under heavy parallel test load, the server
 * may respond slowly and the client may redirect to the list view.
 * This helper retries navigation up to {@link maxRetries} times if the
 * expected element is not visible after each attempt.
 */
export async function navigateToDetail(
  page: Page,
  path: string,
  expectedLocator: Locator,
  timeout = 15_000,
  maxRetries = 3,
): Promise<boolean> {
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    await navigateTo(page, path)
    // Scroll to top so heading elements are in the viewport for isVisible check
    await page.evaluate(() => window.scrollTo(0, 0))
    const visible = await expectedLocator.isVisible({ timeout }).catch(() => false)
    if (visible) return true
  }
  return false
}

/**
 * Use a specific auth state file for a test.
 */
export function authStatePath(
  role:
    | 'student'
    | 'student2'
    | 'student3'
    | 'student4'
    | 'student5'
    | 'supervisor'
    | 'supervisor2'
    | 'examiner'
    | 'examiner2'
    | 'admin'
    | 'delete_old_thesis'
    | 'delete_recent_thesis'
    | 'delete_rejected_app',
): string {
  return `e2e/.auth/${role}.json`
}

/**
 * Type text into a TipTap/ProseMirror rich text editor identified by its label.
 * Optionally accepts a parent locator to scope the search (e.g., a dialog).
 */
export async function fillRichTextEditor(
  page: Page,
  label: string,
  text: string,
  parent?: Locator,
) {
  const root = parent ?? page
  const wrapper = root.locator(
    `.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("${label}"))`,
  )
  const editor = wrapper.locator('.ProseMirror')
  await editor.click()
  // Select all existing content and replace it
  const modifier = process.platform === 'darwin' ? 'Meta' : 'Control'
  await page.keyboard.press(`${modifier}+a`)
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
export async function searchAndSelectMultiSelect(page: Page, label: string, optionPattern: RegExp) {
  const textbox = page.getByRole('textbox', { name: label })
  const listbox = page.getByRole('listbox', { name: label })
  const option = listbox.getByRole('option', { name: optionPattern }).first()
  const wrapper = page.locator(
    `.mantine-InputWrapper-root:has(.mantine-InputWrapper-label:text("${label}"))`,
  )

  // Open dropdown and wait for options. Under heavy parallel load the server
  // may be slow to respond. The fetch is triggered by onDropdownOpen, so we
  // must close and reopen the dropdown between retries to fire a new fetch.
  // IMPORTANT: Do NOT press Escape or click body — both close Mantine modals.
  let found = false
  for (let attempt = 0; attempt < 3 && !found; attempt++) {
    if (attempt > 0) {
      // Close the dropdown before retrying so onDropdownOpen fires again
      await page.keyboard.press('Tab')
      await page.waitForTimeout(300)
    }
    await textbox.click({ force: true })
    // Give the server ample time to respond before retrying
    found = await option.isVisible({ timeout: 20_000 }).catch(() => false)
  }

  await expect(option).toBeVisible({ timeout: 5_000 })

  // Click the option. Retry with force:true if the standard click doesn't register.
  // Do NOT use evaluate to dispatch synthetic mousedown — it bubbles to the document
  // and triggers Mantine's Modal "click outside" handler, closing the dialog.
  for (let clickAttempt = 0; clickAttempt < 3; clickAttempt++) {
    await option.click({ force: clickAttempt > 0 })
    await page.waitForTimeout(500)
    const hasPill = await wrapper.locator('.mantine-Pill-root').count()
    if (hasPill > 0) break
    // Re-open dropdown for next attempt
    await textbox.click({ force: true })
    await expect(option).toBeVisible({ timeout: 10_000 })
  }

  // Verify selection registered
  await expect(wrapper.locator('.mantine-Pill-root')).toBeVisible({ timeout: 5_000 })
  // Close the dropdown by pressing Tab (blurs input). Do NOT use Escape — it closes modals.
  await page.keyboard.press('Tab')
  await page.waitForTimeout(300)
}

/**
 * Expand a Mantine Accordion section by clicking its control and waiting
 * for the panel content to appear.  Under heavy parallel load the first
 * click sometimes doesn't register, so this helper retries up to
 * {@link maxAttempts} times.
 *
 * @param contentLocator  A locator for an element inside the accordion panel
 *                        that becomes visible only when the panel is expanded.
 */
export async function expandAccordion(
  page: Page,
  sectionLabel: string,
  contentLocator: Locator,
  maxAttempts = 3,
) {
  const item = getAccordionItem(page, sectionLabel)
  const control = item.locator('.mantine-Accordion-control').first()
  await control.waitFor({ state: 'visible', timeout: 10_000 })

  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    // Only click if the accordion is not already expanded (prevent close-reopen toggling)
    const isExpanded = await item
      .evaluate((el) => el.hasAttribute('data-active'))
      .catch(() => false)
    if (!isExpanded) {
      await control.click()
    }
    const visible = await contentLocator.isVisible({ timeout: 8_000 }).catch(() => false)
    if (visible) return
    // Small pause before retrying — the click may need the accordion animation to settle
    await page.waitForTimeout(500)
  }
  // Final assertion so the test fails with a clear message if all attempts failed
  await expect(contentLocator).toBeVisible({ timeout: 5_000 })
}

export function getAccordionItem(page: Page, sectionLabel: string) {
  return page
    .locator('.mantine-Accordion-item')
    .filter({
      has: page.locator('.mantine-Accordion-control').filter({
        has: page.getByText(sectionLabel, { exact: true }),
      }),
    })
    .first()
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
