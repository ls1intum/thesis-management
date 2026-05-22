import { describe, expect, test, vi, beforeEach } from 'vitest'

// Mock API + notifications before the component imports them.
const apiMock = vi.hoisted(() => ({
  fetchCombinedSbom: vi.fn(),
  fetchServerSbom: vi.fn(),
  fetchClientSbom: vi.fn(),
  fetchVulnerabilities: vi.fn(),
  refreshVulnerabilities: vi.fn(),
  fetchVersionInfo: vi.fn(),
  sendVulnerabilityEmail: vi.fn(),
}))
const notifyMock = vi.hoisted(() => ({
  showSimpleError: vi.fn(),
  showSimpleSuccess: vi.fn(),
}))

vi.mock('../../requests/dependencyOverview', () => apiMock)
vi.mock('../../utils/notification', () => notifyMock)

import { renderWithProviders, screen, userEvent, waitFor } from '../../../test/render'
import DependencyOverviewPage from './DependencyOverviewPage'

const okResponse = <T,>(data: T) => ({ ok: true as const, status: 200, data })
const notFoundResponse = { ok: false as const, status: 404, data: undefined }

const sampleCombinedSbom = {
  server: {
    bomFormat: 'CycloneDX',
    specVersion: '1.6',
    version: 1,
    components: [
      {
        name: 'spring-core',
        group: 'org.springframework',
        version: '6.0.0',
        purl: 'pkg:maven/org.springframework/spring-core@6.0.0',
      },
      {
        name: 'jackson-databind',
        group: 'com.fasterxml',
        version: '2.15.0',
        purl: 'pkg:maven/com.fasterxml/jackson-databind@2.15.0',
      },
    ],
  },
  client: {
    bomFormat: 'CycloneDX',
    specVersion: '1.6',
    version: 1,
    components: [{ name: 'react', version: '19.0.0', purl: 'pkg:npm/react@19.0.0' }],
  },
}

const sampleVulnerabilities = {
  totalVulnerabilities: 1,
  criticalCount: 1,
  highCount: 0,
  mediumCount: 0,
  lowCount: 0,
  lastChecked: '2026-05-22T10:00:00Z',
  vulnerabilities: [
    {
      componentKey: 'pkg:maven/org.springframework/spring-core@6.0.0',
      vulnerabilities: [
        { id: 'CVE-2024-FAKE', severity: 'CRITICAL' as const, summary: 'Boom', fixedIn: ['6.1.0'] },
      ],
    },
  ],
}

const sampleVersion = {
  currentVersion: '4.11.0',
  latestVersion: '5.0.0',
  updateAvailable: true,
  releaseUrl: 'https://example.com/release/5.0.0',
  lastChecked: '2026-05-22T10:00:00Z',
}

describe('DependencyOverviewPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiMock.fetchCombinedSbom.mockResolvedValue(okResponse(sampleCombinedSbom))
    apiMock.fetchVulnerabilities.mockResolvedValue(okResponse(sampleVulnerabilities))
    apiMock.fetchVersionInfo.mockResolvedValue(okResponse(sampleVersion))
    apiMock.refreshVulnerabilities.mockResolvedValue(okResponse(sampleVulnerabilities))
    apiMock.sendVulnerabilityEmail.mockResolvedValue(okResponse({ sent: true }))
    apiMock.fetchServerSbom.mockResolvedValue(okResponse(sampleCombinedSbom.server))
    apiMock.fetchClientSbom.mockResolvedValue(okResponse(sampleCombinedSbom.client))
  })

  test('renders header, action buttons, upgrade banner, and summary cards', async () => {
    renderWithProviders(<DependencyOverviewPage />)

    expect(await screen.findByRole('heading', { name: /^Dependencies$/i })).toBeInTheDocument()
    // The action buttons should be visible once the initial fetches resolve.
    expect(
      await screen.findByRole('button', { name: /refresh vulnerabilities/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /send email/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /server sbom/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /client sbom/i })).toBeInTheDocument()

    // Summary cards: 2 server + 1 client = 3 total components.
    expect(screen.getByText('Total components').parentElement).toHaveTextContent('3')
    expect(screen.getByText('Server').parentElement?.parentElement).toHaveTextContent('2')
    expect(screen.getByText('Client').parentElement?.parentElement).toHaveTextContent('1')

    // Upgrade banner shows both versions and links to the release.
    expect(screen.getByText('4.11.0')).toBeInTheDocument()
    expect(screen.getByText('5.0.0')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /view release notes/i })).toHaveAttribute(
      'href',
      'https://example.com/release/5.0.0',
    )

    // Critical badge in the summary card.
    expect(screen.getByText(/Critical 1/i)).toBeInTheDocument()
  })

  test('shows an unavailable banner when SBOM endpoint returns 404', async () => {
    apiMock.fetchCombinedSbom.mockResolvedValue(notFoundResponse)
    apiMock.fetchVulnerabilities.mockResolvedValue(notFoundResponse)

    renderWithProviders(<DependencyOverviewPage />)

    expect(await screen.findByText(/SBOM not bundled/i)).toBeInTheDocument()
  })

  test('refresh button reloads vulnerabilities via the API', async () => {
    const user = userEvent.setup()
    renderWithProviders(<DependencyOverviewPage />)

    const button = await screen.findByRole('button', { name: /refresh vulnerabilities/i })
    await user.click(button)

    await waitFor(() => expect(apiMock.refreshVulnerabilities).toHaveBeenCalledTimes(1))
    expect(notifyMock.showSimpleSuccess).toHaveBeenCalledWith(
      expect.stringContaining('Refreshed vulnerability data'),
    )
  })

  test('send-email button triggers the email API and shows a success toast', async () => {
    const user = userEvent.setup()
    renderWithProviders(<DependencyOverviewPage />)

    const button = await screen.findByRole('button', { name: /send email/i })
    await user.click(button)

    await waitFor(() => expect(apiMock.sendVulnerabilityEmail).toHaveBeenCalledTimes(1))
    expect(notifyMock.showSimpleSuccess).toHaveBeenCalledWith(
      expect.stringContaining('Vulnerability scan email sent'),
    )
  })
})
