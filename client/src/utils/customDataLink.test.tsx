import { describe, it, expect } from 'vitest'
import { buildGithubUrl, renderCustomDataValue } from './customDataLink'

describe('buildGithubUrl — issue #953', () => {
  it('turns a plain username into a github.com URL', () => {
    expect(buildGithubUrl('octocat')).toBe('https://github.com/octocat')
  })

  it('strips a leading @', () => {
    expect(buildGithubUrl('@octocat')).toBe('https://github.com/octocat')
  })

  it('trims whitespace', () => {
    expect(buildGithubUrl('  octocat  ')).toBe('https://github.com/octocat')
  })

  it('accepts full github.com URLs', () => {
    expect(buildGithubUrl('https://github.com/some-user')).toBe('https://github.com/some-user')
  })

  it.each([
    ['non-github https URL', 'https://example.com/octocat'],
    ['evil http subdomain', 'http://evil.example.com'],
    ['javascript: scheme', 'javascript:alert(1)'],
  ])('rejects %s', (_, value) => {
    expect(buildGithubUrl(value)).toBeNull()
  })

  it.each([
    ['trailing hyphen', 'foo-'],
    ['leading hyphen', '-foo'],
    ['consecutive hyphens', 'foo--bar'],
    ['too long', 'a'.repeat(40)],
    ['contains space', 'not a user'],
    ['contains slash', 'user/something'],
  ])('rejects %s', (_, value) => {
    expect(buildGithubUrl(value)).toBeNull()
  })

  it.each([
    ['empty', ''],
    ['whitespace', '   '],
    ['just @', '@'],
  ])('returns null for %s value', (_, value) => {
    expect(buildGithubUrl(value)).toBeNull()
  })
})

describe('renderCustomDataValue — issue #953', () => {
  it('returns the raw string for unknown keys', () => {
    expect(renderCustomDataValue('UNKNOWN', 'value')).toBe('value')
  })

  it('returns the raw string when the value would not produce a link', () => {
    expect(renderCustomDataValue('GITHUB', 'foo-')).toBe('foo-')
  })

  it('returns a ReactElement (not a plain string) when GitHub linking succeeds', () => {
    const result = renderCustomDataValue('GITHUB', 'octocat')
    expect(typeof result).toBe('object')
    expect(result).not.toBeNull()
  })
})
