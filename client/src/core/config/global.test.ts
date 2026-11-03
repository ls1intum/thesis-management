import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

const KEYS = ['THESIS_PROCESS_URL', 'THESIS_PROCESS_LABEL']

const loadConfig = async (variables: Record<string, string>) => {
  window.RUNTIME_ENVIRONMENT_VARIABLES = variables
  vi.resetModules()
  return (await import('@/core/config/global')).GLOBAL_CONFIG
}

describe('GLOBAL_CONFIG.thesis_process', () => {
  const originalRuntimeVariables = window.RUNTIME_ENVIRONMENT_VARIABLES
  const originalProcessEnv = Object.fromEntries(KEYS.map((key) => [key, process.env[key]]))

  // getEnvironmentVariable reads process.env before the runtime variables.
  beforeEach(() => {
    KEYS.forEach((key) => delete process.env[key])
  })

  afterEach(() => {
    window.RUNTIME_ENVIRONMENT_VARIABLES = originalRuntimeVariables
    for (const [key, value] of Object.entries(originalProcessEnv)) {
      if (value === undefined) delete process.env[key]
      else process.env[key] = value
    }
  })

  test('is undefined when THESIS_PROCESS_URL is not set', async () => {
    expect((await loadConfig({})).thesis_process).toBeUndefined()
  })

  test('uses the default label when THESIS_PROCESS_LABEL is not set', async () => {
    const config = await loadConfig({ THESIS_PROCESS_URL: 'https://wiki.example.com/thesis' })

    expect(config.thesis_process).toEqual({
      url: 'https://wiki.example.com/thesis',
      label: 'Thesis Process',
    })
  })

  test('uses the configured label', async () => {
    const config = await loadConfig({
      THESIS_PROCESS_URL: 'http://wiki.example.com/thesis',
      THESIS_PROCESS_LABEL: 'Our Thesis Process',
    })

    expect(config.thesis_process?.label).toBe('Our Thesis Process')
  })

  test.each(['wiki.example.com/thesis', 'javascript:alert(1)', 'ftp://example.com/thesis'])(
    'ignores %s since it is not an absolute http(s) URL',
    async (url) => {
      expect((await loadConfig({ THESIS_PROCESS_URL: url })).thesis_process).toBeUndefined()
    },
  )
})
