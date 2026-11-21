import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  buildComponentKey,
  downloadJson,
  highestSeverity,
  SEVERITY_RANK,
  severityColor,
} from '@/core/admin/pages/DependencyOverviewPage/helpers'
import type {
  ISbomComponent,
  IVulnerability,
} from '@/core/admin/requests/responses/dependencyOverview'

const vuln = (severity: IVulnerability['severity'], id = severity): IVulnerability => ({
  id,
  severity,
})

describe('buildComponentKey', () => {
  it('prefers the purl when available', () => {
    const component: ISbomComponent = {
      group: 'org.example',
      name: 'lib',
      version: '1.0.0',
      purl: 'pkg:maven/org.example/lib@1.0.0',
    }
    expect(buildComponentKey(component, 'server')).toBe('pkg:maven/org.example/lib@1.0.0')
  })

  it('falls back to synthetic Maven key when purl missing on server', () => {
    const component: ISbomComponent = {
      group: 'org.example',
      name: 'lib',
      version: '1.0.0',
    }
    expect(buildComponentKey(component, 'server')).toBe('Maven:org.example/lib@1.0.0')
  })

  it('falls back to synthetic npm key when purl missing on client', () => {
    const component: ISbomComponent = {
      name: 'react',
      version: '19.0.0',
    }
    expect(buildComponentKey(component, 'client')).toBe('npm:/react@19.0.0')
  })

  it('tolerates fully-missing fields', () => {
    const component: ISbomComponent = {}
    expect(buildComponentKey(component, 'server')).toBe('Maven:/@')
  })
})

describe('severityColor', () => {
  it('maps each severity to its Mantine color', () => {
    expect(severityColor('CRITICAL')).toBe('red')
    expect(severityColor('HIGH')).toBe('orange')
    expect(severityColor('MEDIUM')).toBe('yellow')
    expect(severityColor('LOW')).toBe('green')
    expect(severityColor('UNKNOWN')).toBe('gray')
  })
})

describe('SEVERITY_RANK', () => {
  it('orders severities from highest to lowest', () => {
    expect(SEVERITY_RANK.CRITICAL).toBeGreaterThan(SEVERITY_RANK.HIGH)
    expect(SEVERITY_RANK.HIGH).toBeGreaterThan(SEVERITY_RANK.MEDIUM)
    expect(SEVERITY_RANK.MEDIUM).toBeGreaterThan(SEVERITY_RANK.LOW)
    expect(SEVERITY_RANK.LOW).toBeGreaterThan(SEVERITY_RANK.UNKNOWN)
  })
})

describe('highestSeverity', () => {
  it('returns null for empty list', () => {
    expect(highestSeverity([])).toBeNull()
  })

  it('returns CRITICAL when any critical is present', () => {
    expect(highestSeverity([vuln('LOW'), vuln('CRITICAL'), vuln('HIGH')])).toBe('CRITICAL')
  })

  it('returns HIGH when no critical', () => {
    expect(highestSeverity([vuln('LOW'), vuln('MEDIUM'), vuln('HIGH')])).toBe('HIGH')
  })

  it('returns the single severity for a single-element list', () => {
    expect(highestSeverity([vuln('MEDIUM')])).toBe('MEDIUM')
  })

  it('treats UNKNOWN as the lowest', () => {
    expect(highestSeverity([vuln('UNKNOWN'), vuln('LOW')])).toBe('LOW')
  })
})

describe('downloadJson', () => {
  const createdLinks: HTMLAnchorElement[] = []
  let clickSpy: ReturnType<typeof vi.fn>
  let createObjectURL: ReturnType<typeof vi.fn>
  let revokeObjectURL: ReturnType<typeof vi.fn>
  let originalCreateElement: typeof document.createElement

  beforeEach(() => {
    createdLinks.length = 0
    clickSpy = vi.fn()
    createObjectURL = vi.fn(() => 'blob:fake-url')
    revokeObjectURL = vi.fn()
    Object.defineProperty(globalThis.URL, 'createObjectURL', {
      configurable: true,
      value: createObjectURL,
    })
    Object.defineProperty(globalThis.URL, 'revokeObjectURL', {
      configurable: true,
      value: revokeObjectURL,
    })

    // Intercept anchor creation so we can assert filename and href without driving the JSDOM
    // download flow (which would otherwise navigate the test page).
    originalCreateElement = document.createElement.bind(document)
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      const element = originalCreateElement(tagName)
      if (tagName.toLowerCase() === 'a') {
        const anchor = element as HTMLAnchorElement
        anchor.click = clickSpy as unknown as () => void
        createdLinks.push(anchor)
      }
      return element
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('creates an object URL and triggers a click with the correct filename', () => {
    downloadJson('server-sbom.json', { hello: 'world' })

    expect(createObjectURL).toHaveBeenCalledTimes(1)
    expect(clickSpy).toHaveBeenCalledTimes(1)
    expect(createdLinks).toHaveLength(1)
    expect(createdLinks[0].download).toBe('server-sbom.json')
    expect(createdLinks[0].href).toContain('blob:fake-url')
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake-url')
  })

  it('pretty-prints the JSON with 2-space indent', () => {
    let capturedBody: BlobPart[] | undefined
    const RealBlob = globalThis.Blob
    class BlobSpy extends RealBlob {
      constructor(parts: BlobPart[], options?: BlobPropertyBag) {
        super(parts, options)
        capturedBody = parts
      }
    }
    vi.stubGlobal('Blob', BlobSpy)

    downloadJson('client-sbom.json', { a: 1, b: [2, 3] })

    expect(capturedBody).toBeDefined()
    const body = String(capturedBody?.[0] ?? '')
    expect(body).toContain('\n  "a": 1')
    expect(body).toContain('\n  "b": [')
  })
})
