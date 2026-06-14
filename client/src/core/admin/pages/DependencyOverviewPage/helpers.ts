import type {
  ISbomComponent,
  IVulnerability,
  VulnerabilitySeverity,
} from '@/core/admin/requests/responses/dependencyOverview'

export type ComponentSource = 'server' | 'client'
export type SourceFilter = 'all' | ComponentSource

export const SEVERITY_RANK: Record<VulnerabilitySeverity, number> = {
  CRITICAL: 4,
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1,
  UNKNOWN: 0,
}

/**
 * Build a stable lookup key for a component. Prefers the purl; falls back to a
 * synthetic key matching the one the server emits so the two sides can join.
 */
export function buildComponentKey(component: ISbomComponent, source: ComponentSource): string {
  if (component.purl) {
    return component.purl
  }
  const ecosystem = source === 'server' ? 'Maven' : 'npm'
  return `${ecosystem}:${component.group ?? ''}/${component.name ?? ''}@${component.version ?? ''}`
}

export function severityColor(severity: VulnerabilitySeverity): string {
  switch (severity) {
    case 'CRITICAL':
      return 'red'
    case 'HIGH':
      return 'orange'
    case 'MEDIUM':
      return 'yellow'
    case 'LOW':
      return 'green'
    default:
      return 'gray'
  }
}

/**
 * The highest severity across a vulnerability list, or null if the list is empty.
 */
export function highestSeverity(vulns: IVulnerability[]): VulnerabilitySeverity | null {
  if (vulns.length === 0) {
    return null
  }
  return vulns.reduce<VulnerabilitySeverity>(
    (acc, v) => (SEVERITY_RANK[v.severity] > SEVERITY_RANK[acc] ? v.severity : acc),
    'UNKNOWN',
  )
}

/**
 * Trigger a browser download of `data` serialized as pretty-printed JSON.
 * Exported separately so the page component can stay lean and so the logic can be
 * unit-tested without a full DOM render.
 */
export function downloadJson(filename: string, data: unknown): void {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
