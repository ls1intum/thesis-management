import type { ReactElement, ReactNode } from 'react'
import { MantineProvider } from '@mantine/core'
import type { RenderOptions, RenderResult } from '@testing-library/react'
import { render } from '@testing-library/react'

interface ProvidersProps {
  children: ReactNode
}

const Providers = ({ children }: ProvidersProps) => (
  <MantineProvider defaultColorScheme='light'>{children}</MantineProvider>
)

/**
 * Renders the given UI inside a MantineProvider so Mantine components
 * resolve their theme and color-scheme context. Use this instead of the
 * raw `render` from @testing-library/react in component tests.
 */
export const renderWithProviders = (
  ui: ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>,
): RenderResult => render(ui, { wrapper: Providers, ...options })

// Re-export the rest of the testing-library API for convenience.
export * from '@testing-library/react'
export { default as userEvent } from '@testing-library/user-event'
