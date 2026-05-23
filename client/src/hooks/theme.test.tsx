import type { ReactNode } from 'react'
import { MantineProvider, useMantineTheme } from '@mantine/core'
import { renderHook } from '@testing-library/react'
import { afterEach, describe, expect, test, vi } from 'vitest'
import { useHighlightedBackgroundColor } from './theme'

const makeWrapper =
  (defaultColorScheme: 'light' | 'dark' | 'auto') =>
  ({ children }: { children: ReactNode }) => (
    <MantineProvider defaultColorScheme={defaultColorScheme}>{children}</MantineProvider>
  )

// Resolve the expected color values against the same theme the hook sees,
// so the test stays correct if Mantine's default palette ever shifts.
const getThemeColors = (defaultColorScheme: 'light' | 'dark' | 'auto' = 'light') => {
  const { result } = renderHook(() => useMantineTheme(), {
    wrapper: makeWrapper(defaultColorScheme),
  })
  return result.current.colors
}

describe('useHighlightedBackgroundColor', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  test('returns the light unselected color when color scheme is light', () => {
    const colors = getThemeColors('light')
    const { result } = renderHook(() => useHighlightedBackgroundColor(false), {
      wrapper: makeWrapper('light'),
    })
    expect(result.current).toBe(colors.gray[1])
  })

  test('returns the light primary color when selected and color scheme is light', () => {
    const colors = getThemeColors('light')
    const { result } = renderHook(() => useHighlightedBackgroundColor(true), {
      wrapper: makeWrapper('light'),
    })
    expect(result.current).toBe(colors.blue[4])
  })

  test('returns the dark unselected color when color scheme is dark', () => {
    const colors = getThemeColors('dark')
    const { result } = renderHook(() => useHighlightedBackgroundColor(false), {
      wrapper: makeWrapper('dark'),
    })
    expect(result.current).toBe(colors.dark[6])
  })

  test('returns the dark primary color when selected and color scheme is dark', () => {
    const colors = getThemeColors('dark')
    const { result } = renderHook(() => useHighlightedBackgroundColor(true), {
      wrapper: makeWrapper('dark'),
    })
    expect(result.current).toBe(colors.blue[6])
  })

  // Regression: prior to the fix the hook compared `useMantineColorScheme().colorScheme`
  // against 'dark'. With the system default ('auto') that comparison was always false
  // and the panel rendered with the light background even when the OS was in dark mode.
  // See issue #1016.
  test('resolves auto to dark when the system prefers a dark color scheme', () => {
    vi.stubGlobal(
      'matchMedia',
      (query: string) =>
        ({
          matches: query.includes('prefers-color-scheme: dark'),
          media: query,
          onchange: null,
          addListener: () => {},
          removeListener: () => {},
          addEventListener: () => {},
          removeEventListener: () => {},
          dispatchEvent: () => false,
        }) as MediaQueryList,
    )

    const colors = getThemeColors('auto')
    const { result } = renderHook(() => useHighlightedBackgroundColor(false), {
      wrapper: makeWrapper('auto'),
    })
    expect(result.current).toBe(colors.dark[6])
  })

  test('resolves auto to light when the system prefers a light color scheme', () => {
    vi.stubGlobal(
      'matchMedia',
      (query: string) =>
        ({
          matches: false,
          media: query,
          onchange: null,
          addListener: () => {},
          removeListener: () => {},
          addEventListener: () => {},
          removeEventListener: () => {},
          dispatchEvent: () => false,
        }) as MediaQueryList,
    )

    const colors = getThemeColors('auto')
    const { result } = renderHook(() => useHighlightedBackgroundColor(false), {
      wrapper: makeWrapper('auto'),
    })
    expect(result.current).toBe(colors.gray[1])
  })
})
