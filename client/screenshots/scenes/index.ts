import type { Scene } from '../types'
import { studentScenes } from './student'
import { staffScenes } from './staff'

/**
 * Central registry of all screenshots the platform knows how to capture.
 *
 * Order matters only for readability in the test report — Playwright itself
 * groups scenes by role (see `capture.spec.ts`).
 */
export const scenes: Scene[] = [...studentScenes, ...staffScenes]

/**
 * Fail-fast guard: two scenes must never share a filename, otherwise the
 * second one silently overwrites the first on disk.
 */
const seen = new Set<string>()
for (const scene of scenes) {
  if (seen.has(scene.filename)) {
    throw new Error(
      `Duplicate screenshot filename: ${scene.filename}. Each Scene.filename must be unique.`,
    )
  }
  seen.add(scene.filename)
}
