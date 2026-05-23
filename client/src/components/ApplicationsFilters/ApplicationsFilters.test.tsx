import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderWithProviders, screen, userEvent } from '../../../test/render'
import ApplicationsFilters from './ApplicationsFilters'
import type { IApplicationsFilters } from '../../providers/ApplicationsProvider/context'

const setFilters = vi.fn()
const setSort = vi.fn()
let filters: IApplicationsFilters = { includeSuggestedTopics: true }

vi.mock('../../providers/ApplicationsProvider/hooks', () => ({
  useApplicationsContext: () => ({
    topics: {},
    filters,
    setFilters: (updater: (prev: IApplicationsFilters) => IApplicationsFilters) => {
      const next = typeof updater === 'function' ? updater(filters) : updater
      filters = next
      setFilters(next)
    },
    sort: { column: 'createdAt', direction: 'asc' },
    setSort,
  }),
}))

describe('ApplicationsFilters — issue #948 suggested-topics toggle', () => {
  beforeEach(() => {
    setFilters.mockReset()
    setSort.mockReset()
    filters = { includeSuggestedTopics: true }
  })

  it('renders an "Include suggested topics" switch defaulting to checked', () => {
    renderWithProviders(<ApplicationsFilters />)
    const toggle = screen.getByRole('switch', { name: /include suggested topics/i })
    expect(toggle).toBeChecked()
  })

  it('treats undefined includeSuggestedTopics as on', () => {
    filters = {}
    renderWithProviders(<ApplicationsFilters />)
    expect(screen.getByRole('switch', { name: /include suggested topics/i })).toBeChecked()
  })

  it('flips the filter to false when the user turns the switch off', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ApplicationsFilters />)
    await user.click(screen.getByRole('switch', { name: /include suggested topics/i }))
    expect(setFilters).toHaveBeenCalledWith(
      expect.objectContaining({ includeSuggestedTopics: false }),
    )
  })
})
