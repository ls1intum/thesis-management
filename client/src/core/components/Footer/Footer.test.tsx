import { beforeEach, describe, expect, test, vi } from 'vitest'
import { MemoryRouter } from 'react-router'
import type { IGlobalConfig } from '@/core/config/types'

const globalConfig: Pick<IGlobalConfig, 'chair_name' | 'chair_url' | 'thesis_process'> = {
  chair_name: 'Test Chair',
  chair_url: 'https://chair.example.com',
  thesis_process: undefined,
}

vi.mock('@/core/config/global', () => ({
  get GLOBAL_CONFIG() {
    return globalConfig
  },
}))

import { renderWithProviders, screen } from '@/../test/render'
import Footer from '@/core/components/Footer/Footer'

const renderFooter = () =>
  renderWithProviders(
    <MemoryRouter>
      <Footer />
    </MemoryRouter>,
  )

describe('Footer', () => {
  beforeEach(() => {
    globalConfig.thesis_process = undefined
  })

  test('links to the configured thesis process page', () => {
    globalConfig.thesis_process = {
      url: 'https://wiki.example.com/thesis',
      label: 'Our Thesis Process',
    }

    renderFooter()

    const link = screen.getByRole('link', { name: 'Our Thesis Process' })
    expect(link).toHaveAttribute('href', 'https://wiki.example.com/thesis')
    expect(link).toHaveAttribute('target', '_blank')
  })

  test('omits the thesis process link when not configured', () => {
    renderFooter()

    expect(screen.queryByRole('link', { name: /thesis process/i })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Docs' })).toBeInTheDocument()
  })
})
