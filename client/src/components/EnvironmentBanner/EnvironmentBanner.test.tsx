import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { Environment } from '../../config/types'

const globalConfig: { environment: Environment | undefined } = {
  environment: undefined,
}

vi.mock('../../config/global', () => ({
  get GLOBAL_CONFIG() {
    return globalConfig
  },
}))

// Imported AFTER the mock so isEnvironmentBannerVisible reads the mocked
// GLOBAL_CONFIG getter.
import { isEnvironmentBannerVisible } from './EnvironmentBanner'

describe('isEnvironmentBannerVisible — issue #594', () => {
  beforeEach(() => {
    globalConfig.environment = undefined
  })

  it('returns false when no environment is configured', () => {
    expect(isEnvironmentBannerVisible()).toBe(false)
  })

  it('returns false on production', () => {
    globalConfig.environment = 'production'
    expect(isEnvironmentBannerVisible()).toBe(false)
  })

  it.each(['staging', 'test', 'dev'] as const)('returns true for %s', (env) => {
    globalConfig.environment = env
    expect(isEnvironmentBannerVisible()).toBe(true)
  })
})
