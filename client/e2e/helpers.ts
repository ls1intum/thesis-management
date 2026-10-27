import { BrowserContext, Locator, Page, expect } from '@playwright/test'

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
 * Navigate to an entity detail page (application, thesis) and assert that the
 * detail view rendered. Throws via the contained `expect` if the expected
 * locator never appears within `timeout` — callers that previously branched
 * on a boolean return value should remove that branching.
 *
 * The helper deliberately does not retry: it waits a single bounded interval
 * and either succeeds or fails the test. Silent skips on slow loads are
 * unacceptable because they hide real product/server regressions.
 */
export async function navigateToDetail(
  page: Page,
  path: string,
  expectedLocator: Locator,
  timeout = 60_000,
): Promise<void> {
  await navigateTo(page, path)
  await page.evaluate(() => window.scrollTo(0, 0))
  await expect(expectedLocator).toBeVisible({ timeout })
}

/**
 * Navigate to the standalone Configuration page for a thesis and wait for the
 * "Update" button to become visible so tests can start interacting with the
 * form. Only supervisors/examiners/admins reach this page; students are
 * redirected to /theses/:id by the page component itself.
 */
export async function navigateToThesisConfig(page: Page, thesisId: string) {
  await navigateTo(page, `/theses/${thesisId}/configuration`)
  await expect(page.getByRole('button', { name: 'Update' })).toBeVisible({ timeout: 15_000 })
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
    | 'passkey_user'
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
 * Mantine v9 promotes the input to role="combobox" (W3C combobox pattern).
 */
export async function selectOption(page: Page, label: string, optionText: string | RegExp) {
  await page.getByRole('combobox', { name: label }).click()
  await page.getByRole('option', { name: optionText }).click()
}

/**
 * Click a Mantine MultiSelect input. Uses force:true to bypass the wrapper
 * div that intercepts pointer events.
 */
export async function clickMultiSelect(page: Page, label: string) {
  await page.getByRole('combobox', { name: label }).click({ force: true })
}

/**
 * Select an option from a UserMultiSelect (server-side search).
 * Opens the dropdown, waits for options to load, then selects the option.
 * Uses evaluate to dispatch a full mouse event chain since Playwright's
 * built-in click doesn't always trigger Mantine's React event handlers
 * in portal-rendered combobox dropdowns.
 */
export async function searchAndSelectMultiSelect(page: Page, label: string, optionPattern: RegExp) {
  const textbox = page.getByRole('combobox', { name: label })
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
 * Pass a string `section` to locate the accordion by its label text, or pass
 * a `Locator` when the caller has already resolved the specific item (useful
 * when multiple accordions share the same label — e.g. the Overview and
 * Writing sections each expose a "Comments" accordion).
 *
 * @param contentLocator  A locator for an element inside the accordion panel
 *                        that becomes visible only when the panel is expanded.
 */
export async function expandAccordion(
  page: Page,
  section: string | Locator,
  contentLocator: Locator,
  maxAttempts = 3,
) {
  const item = typeof section === 'string' ? getAccordionItem(page, section) : section
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
    // Scroll to the content locator so it enters the viewport
    await contentLocator.scrollIntoViewIfNeeded().catch(() => {})
    const visible = await contentLocator.isVisible({ timeout: 8_000 }).catch(() => false)
    if (visible) return
    // Small pause before retrying — the click may need the accordion animation to settle
    await page.waitForTimeout(500)
  }
  // Final scroll + assertion so the test fails with a clear message if all attempts failed
  await contentLocator.scrollIntoViewIfNeeded().catch(() => {})
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
 * Locate the "Comments" accordion inside the merged Overview section (the
 * supervisor-only one that carries the "Not visible to student" badge). This
 * disambiguates from the WRITING section's separate "Comments" accordion,
 * which uses the same label but has no badge.
 */
export function getSupervisorCommentsAccordion(page: Page) {
  return page
    .locator('.mantine-Accordion-item')
    .filter({ hasText: 'Comments' })
    .filter({ hasText: 'Not visible to student' })
    .first()
}

/**
 * Permanently suppress the webpack-dev-server error overlay that intercepts pointer events.
 * The overlay can appear at any time (e.g., ResizeObserver errors during accordion expansion),
 * so this installs a MutationObserver that auto-hides it whenever it's added to the DOM.
 * Safe to call multiple times — the observer is only installed once per page.
 */
export async function hideWebpackOverlay(page: Page) {
  await page.evaluate(() => {
    if ((window as any).__webpackOverlayObserver) return

    const hide = () => {
      const iframe = document.getElementById('webpack-dev-server-client-overlay')
      if (iframe) (iframe as HTMLElement).style.display = 'none'
    }

    hide()

    const observer = new MutationObserver(hide)
    observer.observe(document.body, { childList: true, subtree: true })
    ;(window as any).__webpackOverlayObserver = observer
  })
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

// KEYCLOAK_HOST is the variable the CI workflow exports (server, client build and tests share it)
const KEYCLOAK_ORIGIN = process.env.KEYCLOAK_HOST ?? 'http://localhost:8181'
const SERVER_ORIGIN = process.env.SERVER_URL ?? 'http://localhost:8180'

export { KEYCLOAK_ORIGIN, SERVER_ORIGIN }

/**
 * Logs a test user in without the user interface: takes a Keycloak access token (the dev realm allows the
 * password grant, and every seed user's password is its username) and exchanges it for an application
 * session. The session cookie lands in the context's cookie jar, so pages of that context are logged in
 * with a session of their own that no other test shares.
 */
export async function loginWithApi(context: BrowserContext, username: string) {
  // A fresh context has none of the "do not ask again" flags that the shared login setup stores, so the passkey
  // and profile picture prompts would open modals that cover the page under test.
  await context.addInitScript((keys: string[]) => {
    keys.forEach((key) => localStorage.setItem(key, 'true'))
  }, ['passkey_prompt_disabled', 'profile_picture_prompt_disabled'])

  const tokenResponse = await context.request.post(
    `${KEYCLOAK_ORIGIN}/realms/thesis-management/protocol/openid-connect/token`,
    {
      form: {
        grant_type: 'password',
        client_id: 'thesis-management-app',
        username,
        password: username,
      },
    },
  )
  expect(tokenResponse.ok()).toBe(true)
  const { access_token: accessToken } = (await tokenResponse.json()) as { access_token: string }

  const sessionResponse = await context.request.post(`${SERVER_ORIGIN}/api/v2/auth/session`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })
  expect(sessionResponse.ok()).toBe(true)
}

/** Finds the application session cookie of a context (`__Host-sid` over https, `sid` over plain http). */
export async function getSessionCookie(context: BrowserContext) {
  return (await context.cookies()).find(
    (cookie) => cookie.name === 'sid' || cookie.name === '__Host-sid',
  )
}

/** Ends the context's own session on the server, so tests do not pile up sessions of the seed users. */
export async function logoutWithApi(context: BrowserContext) {
  const session = await context.request.get(`${SERVER_ORIGIN}/api/v2/auth/session`)

  if (!session.ok()) {
    return
  }

  const { csrfToken } = (await session.json()) as { csrfToken: string }
  await context.request.delete(`${SERVER_ORIGIN}/api/v2/auth/session`, {
    headers: { 'X-CSRF-Token': csrfToken },
  })
}

/**
 * Create a valid PDF buffer with a real "Abstract" section so the server's abstract
 * extractor can confidently extract it. The body contains a line-end hyphenation
 * ("com-" / "prehensive") that must be rejoined into "comprehensive", and a
 * "1 Introduction" heading that bounds the abstract.
 */
export function createAbstractTestPdfBuffer(): Buffer {
  const content =
    'BT /F1 14 Tf 72 720 Td (Abstract) Tj ET\n' +
    'BT /F1 10 Tf 72 695 Td (This thesis presents a com-) Tj ET\n' +
    'BT /F1 10 Tf 72 680 Td (prehensive evaluation of automated review systems.) Tj ET\n' +
    'BT /F1 14 Tf 72 650 Td (1 Introduction) Tj ET\n' +
    'BT /F1 10 Tf 72 625 Td (The introduction begins here with more detail.) Tj ET\n'

  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    `<< /Length ${Buffer.byteLength(content)} >>\nstream\n${content}endstream`,
  ]

  let pdf = '%PDF-1.4\n'
  const offsets: number[] = []
  objects.forEach((body, index) => {
    offsets.push(Buffer.byteLength(pdf))
    pdf += `${index + 1} 0 obj\n${body}\nendobj\n`
  })

  const xrefOffset = Buffer.byteLength(pdf)
  pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`
  offsets.forEach((offset) => {
    pdf += `${String(offset).padStart(10, '0')} 00000 n \n`
  })
  pdf += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`

  return Buffer.from(pdf, 'latin1')
}
