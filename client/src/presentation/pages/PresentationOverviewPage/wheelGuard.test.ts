import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(here, 'PresentationOverviewPage.tsx'), 'utf8')

describe('PresentationOverviewPage — issue #729 (mobile scroll guard)', () => {
  it('early-returns from the wheel useEffect when on a small viewport', () => {
    const wheelEffectRegex =
      /useEffect\(\(\)\s*=>\s*\{[\s\S]*?if\s*\(\s*isSmaller\s*\)\s*\{[\s\S]*?return[\s\S]*?\}[\s\S]*?addEventListener\(['"]wheel['"]/
    expect(source).toMatch(wheelEffectRegex)
  })

  it('depends on isSmaller so the effect re-runs on viewport changes', () => {
    const depRegex =
      /addEventListener\(['"]wheel['"][\s\S]*?removeEventListener\(['"]wheel['"][\s\S]*?\}\s*,\s*\[([^\]]*)\]\s*\)/
    const match = source.match(depRegex)
    expect(match).not.toBeNull()
    if (!match) {
      throw new Error('expected wheel useEffect to match dep regex')
    }
    expect(match[1]).toMatch(/isSmaller/)
  })
})
