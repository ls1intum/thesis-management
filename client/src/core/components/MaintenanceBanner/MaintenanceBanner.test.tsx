import { describe, it, expect, vi, beforeEach } from 'vitest'

const globalConfig: { maintenance_message: string | undefined } = {
  maintenance_message: undefined,
}

vi.mock('@/core/config/global', () => ({
  get GLOBAL_CONFIG() {
    return globalConfig
  },
}))

// Imported AFTER the mock so isMaintenanceBannerVisible reads the mocked GLOBAL_CONFIG getter.
import { isMaintenanceBannerVisible } from '@/core/components/MaintenanceBanner/MaintenanceBanner'

describe('isMaintenanceBannerVisible', () => {
  beforeEach(() => {
    globalConfig.maintenance_message = undefined
  })

  it('returns false when no message is configured', () => {
    expect(isMaintenanceBannerVisible()).toBe(false)
  })

  it('returns false for a blank message', () => {
    globalConfig.maintenance_message = '   '
    expect(isMaintenanceBannerVisible()).toBe(false)
  })

  it('returns true when a message is configured', () => {
    globalConfig.maintenance_message = 'We are updating the service.'
    expect(isMaintenanceBannerVisible()).toBe(true)
  })
})
