import type { Locator, Page } from '@playwright/test'

/**
 * A "scene" is a single documentation screenshot. Each scene:
 *   1. Runs as an authenticated user of the given `role` (or anonymously).
 *   2. Navigates and prepares the page state via `run`.
 *   3. Gets captured to `<repo>/documentation/static/img/screenshots/<filename>.png`.
 *
 * Adding a new screenshot = adding a new Scene object to one of the arrays in
 * `scenes/`. No wiring changes needed.
 */
/**
 * Auth role a scene uses. Anything other than `'anonymous'` must correspond
 * to a Keycloak test user that `client/e2e/auth.setup.ts` logs in.
 *
 * When you need a variant that isn't in this union yet (e.g. `student3`),
 * add it here AND make sure `auth.setup.ts` produces the matching
 * `.auth/<role>.json` file.
 */
export type SceneRole =
  | 'anonymous'
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

export interface Scene {
  /** Basename of the output PNG (no extension). Must be unique across all scenes. */
  filename: string

  /**
   * Human description shown in test output. Keep it short — this is what the
   * screenshot is meant to depict, so a caption update in the guide should be
   * mirrored here.
   */
  description: string

  /** Which auth state to use. `'anonymous'` skips storageState entirely. */
  role: SceneRole

  /**
   * Prepare the page: navigate, expand accordions, open modals, dismiss
   * unrelated overlays. When this resolves, the DOM must already look the way
   * the screenshot should look — the driver just calls `page.screenshot()`.
   */
  run: (page: Page) => Promise<Locator | void>

  /** Full-page or viewport screenshot. Default: viewport only. */
  fullPage?: boolean

  /**
   * Optional viewport override. Defaults to 1440 × 900 (see playwright.config).
   * Handy for landing-page hero shots or mobile-first flows.
   */
  viewport?: { width: number; height: number }

  /** Optional clip region. Ignored when `fullPage` is true. */
  clip?: { x: number; y: number; width: number; height: number }
}
