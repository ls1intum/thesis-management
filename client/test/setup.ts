import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// Mantine relies on browser APIs that jsdom does not implement out of the
// box. Polyfill them here so components like Modal, Switch and Select can
// render under Vitest.
//
// See https://mantine.dev/guides/vitest/ for the canonical list — kept
// minimal here, extend as needed when new component tests fail in jsdom.

// 1) matchMedia — Mantine uses this for breakpoints, color-scheme detection, etc.
if (!('matchMedia' in window) || typeof window.matchMedia !== 'function') {
  window.matchMedia = (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  })
}

// 2) ResizeObserver — used by Mantine's Modal, Popover, ScrollArea, etc.
class ResizeObserverMock {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
}
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver
}

// 3) scrollTo — Mantine occasionally calls window.scrollTo / element.scrollTo.
if (typeof window.scrollTo !== 'function') {
  window.scrollTo = () => {}
}

// React Testing Library auto-cleanup between tests.
afterEach(() => {
  cleanup()
})
