import { describe, expect, test } from 'vitest'
import { GLOBAL_CONFIG } from '@/core/config/global'
import { getThesisSubmissionPortalUrl } from '@/thesis/utils/thesis'

describe('getThesisSubmissionPortalUrl', () => {
  test('uses the portal of the thesis school when there is one', () => {
    expect(
      getThesisSubmissionPortalUrl({ submissionPortalUrl: 'https://portal.mgt.tum.de/' }),
    ).toBe('https://portal.mgt.tum.de/')
  })

  test('falls back to the instance default', () => {
    expect(getThesisSubmissionPortalUrl({})).toBe(GLOBAL_CONFIG.thesis_portal_url)
    expect(GLOBAL_CONFIG.thesis_portal_url).toMatch(/^https?:\/\//)
  })
})
