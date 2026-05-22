import { doRequest } from './request'
import type { ApiResponse } from './request'
import type {
  ICombinedSbom,
  IComponentVulnerabilities,
  ISbom,
  IThesisManagementVersion,
} from './responses/dependencyOverview'

const BASE = '/v2/admin/dependencies'

export function fetchCombinedSbom(): Promise<ApiResponse<ICombinedSbom>> {
  return doRequest<ICombinedSbom>(BASE, { method: 'GET', requiresAuth: true })
}

export function fetchServerSbom(): Promise<ApiResponse<ISbom>> {
  return doRequest<ISbom>(`${BASE}/server`, { method: 'GET', requiresAuth: true })
}

export function fetchClientSbom(): Promise<ApiResponse<ISbom>> {
  return doRequest<ISbom>(`${BASE}/client`, { method: 'GET', requiresAuth: true })
}

export function fetchVulnerabilities(): Promise<ApiResponse<IComponentVulnerabilities>> {
  return doRequest<IComponentVulnerabilities>(`${BASE}/vulnerabilities`, {
    method: 'GET',
    requiresAuth: true,
  })
}

export function refreshVulnerabilities(): Promise<ApiResponse<IComponentVulnerabilities>> {
  return doRequest<IComponentVulnerabilities>(`${BASE}/vulnerabilities/refresh`, {
    method: 'GET',
    requiresAuth: true,
  })
}

export function fetchVersionInfo(): Promise<ApiResponse<IThesisManagementVersion>> {
  return doRequest<IThesisManagementVersion>(`${BASE}/version`, {
    method: 'GET',
    requiresAuth: true,
  })
}

export interface ISendVulnerabilityEmailResult {
  sent: boolean
}

export function sendVulnerabilityEmail(): Promise<ApiResponse<ISendVulnerabilityEmailResult>> {
  return doRequest<ISendVulnerabilityEmailResult>(`${BASE}/vulnerabilities/send-email`, {
    method: 'POST',
    requiresAuth: true,
  })
}
