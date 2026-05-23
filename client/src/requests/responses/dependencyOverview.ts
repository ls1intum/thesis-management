export interface ISbomComponent {
  group?: string
  name?: string
  version?: string
  type?: string
  purl?: string
  licenses?: string[]
  description?: string
}

export interface ISbomMetadata {
  timestamp?: string
  componentName?: string
  version?: string
}

export interface ISbom {
  bomFormat?: string
  specVersion?: string
  serialNumber?: string
  version?: number
  metadata?: ISbomMetadata
  components?: ISbomComponent[]
}

export interface ICombinedSbom {
  server?: ISbom
  client?: ISbom
}

export type VulnerabilitySeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN'

export interface IVulnerability {
  id: string
  summary?: string
  details?: string
  severity: VulnerabilitySeverity
  cvssScore?: number
  aliases?: string[]
  fixedIn?: string[]
  references?: string[]
}

export interface IComponentWithVulnerabilities {
  componentKey: string
  vulnerabilities: IVulnerability[]
}

export interface IComponentVulnerabilities {
  vulnerabilities?: IComponentWithVulnerabilities[]
  totalVulnerabilities: number
  criticalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  lastChecked?: string
}

export interface IThesisManagementVersion {
  currentVersion: string
  latestVersion?: string
  updateAvailable: boolean
  releaseUrl?: string
  releaseNotes?: string
  lastChecked?: string
}
