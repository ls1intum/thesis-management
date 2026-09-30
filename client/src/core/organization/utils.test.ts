import { describe, expect, test } from 'vitest'
import { isHttpUrl } from '@/core/organization/utils'

describe('isHttpUrl', () => {
  test.each(['', '   ', 'https://portal.mgt.tum.de/', 'http://localhost:3100/path?x=1'])(
    'accepts %j',
    (value) => {
      expect(isHttpUrl(value)).toBe(true)
    },
  )

  test.each([
    'javascript:alert(1)',
    'ftp://example.org',
    'portal.mgt.tum.de',
    'not a url',
    'data:text/html,x',
  ])('rejects %j', (value) => {
    expect(isHttpUrl(value)).toBe(false)
  })
})
