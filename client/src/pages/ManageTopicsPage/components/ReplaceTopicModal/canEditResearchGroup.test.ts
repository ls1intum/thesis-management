import { describe, it, expect } from 'vitest'
import { canEditResearchGroup } from './canEditResearchGroup'
import type { ITopic } from '../../../../requests/responses/topic'

const baseTopic = {
  topicId: 't',
  title: 'T',
  createdBy: { userId: 'creator-id' },
  supervisors: [{ userId: 'sup-id' }],
  examiners: [{ userId: 'exam-id' }],
} as unknown as ITopic

describe('canEditResearchGroup — issue #764', () => {
  it('returns false when there is no current user', () => {
    expect(canEditResearchGroup(baseTopic, undefined)).toBe(false)
  })

  it('lets admins edit any topic, even before the topic loads', () => {
    expect(canEditResearchGroup(undefined, { userId: 'a', isAdmin: true })).toBe(true)
    expect(canEditResearchGroup(baseTopic, { userId: 'a', isAdmin: true })).toBe(true)
  })

  it('lets the topic creator edit', () => {
    expect(canEditResearchGroup(baseTopic, { userId: 'creator-id', isAdmin: false })).toBe(true)
  })

  it('lets a topic supervisor edit', () => {
    expect(canEditResearchGroup(baseTopic, { userId: 'sup-id', isAdmin: false })).toBe(true)
  })

  it('lets a topic examiner edit', () => {
    expect(canEditResearchGroup(baseTopic, { userId: 'exam-id', isAdmin: false })).toBe(true)
  })

  it('rejects unrelated users', () => {
    expect(canEditResearchGroup(baseTopic, { userId: 'someone-else', isAdmin: false })).toBe(false)
  })

  it('rejects everyone before the topic loads (unless admin)', () => {
    expect(canEditResearchGroup(undefined, { userId: 'creator-id', isAdmin: false })).toBe(false)
  })
})
