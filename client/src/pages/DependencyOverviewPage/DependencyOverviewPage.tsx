import React, { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Anchor,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  SegmentedControl,
  SimpleGrid,
  Stack,
  Switch,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { DataTable } from 'mantine-datatable'
import type { DataTableColumn, DataTableSortStatus } from 'mantine-datatable'
import {
  ArrowClockwise,
  Desktop,
  DownloadSimple,
  Envelope,
  Info,
  ShieldCheck,
  ShieldWarning,
  WarningCircle,
} from '@phosphor-icons/react'
import {
  fetchClientSbom,
  fetchCombinedSbom,
  fetchServerSbom,
  fetchVersionInfo,
  fetchVulnerabilities,
  refreshVulnerabilities,
  sendVulnerabilityEmail,
} from '../../requests/dependencyOverview'
import type {
  IComponentVulnerabilities,
  ICombinedSbom,
  ISbomComponent,
  IThesisManagementVersion,
  IVulnerability,
  VulnerabilitySeverity,
} from '../../requests/responses/dependencyOverview'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'

type ComponentSource = 'server' | 'client'
type SourceFilter = 'all' | ComponentSource

interface IComponentRow {
  source: ComponentSource
  name: string
  group: string
  version: string
  type: string
  purl: string
  licenses: string[]
  description: string
  componentKey: string
  vulnerabilities: IVulnerability[]
}

const SEVERITY_RANK: Record<VulnerabilitySeverity, number> = {
  CRITICAL: 4,
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1,
  UNKNOWN: 0,
}

function buildComponentKey(component: ISbomComponent, source: ComponentSource): string {
  if (component.purl) {
    return component.purl
  }
  const ecosystem = source === 'server' ? 'Maven' : 'npm'
  return `${ecosystem}:${component.group ?? ''}/${component.name ?? ''}@${component.version ?? ''}`
}

function severityColor(severity: VulnerabilitySeverity): string {
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

function highestSeverity(vulns: IVulnerability[]): VulnerabilitySeverity | null {
  if (vulns.length === 0) {
    return null
  }
  return vulns.reduce<VulnerabilitySeverity>(
    (acc, v) => (SEVERITY_RANK[v.severity] > SEVERITY_RANK[acc] ? v.severity : acc),
    'UNKNOWN',
  )
}

function downloadJson(filename: string, data: unknown) {
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

const DependencyOverviewPage = () => {
  const [sbom, setSbom] = useState<ICombinedSbom>()
  const [vulnerabilities, setVulnerabilities] = useState<IComponentVulnerabilities>()
  const [versionInfo, setVersionInfo] = useState<IThesisManagementVersion>()
  const [sbomUnavailable, setSbomUnavailable] = useState(false)
  const [loadingSbom, setLoadingSbom] = useState(true)
  const [loadingVulnerabilities, setLoadingVulnerabilities] = useState(false)
  const [sendingEmail, setSendingEmail] = useState(false)
  const [filterText, setFilterText] = useState('')
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>('all')
  const [showOnlyVulnerable, setShowOnlyVulnerable] = useState(false)
  const [sortStatus, setSortStatus] = useState<DataTableSortStatus<IComponentRow>>({
    columnAccessor: 'name',
    direction: 'asc',
  })

  useEffect(() => {
    let cancelled = false

    void (async () => {
      const [sbomRes, vulnRes, versionRes] = await Promise.all([
        fetchCombinedSbom(),
        fetchVulnerabilities(),
        fetchVersionInfo(),
      ])
      if (cancelled) {
        return
      }

      if (sbomRes.ok) {
        setSbom(sbomRes.data)
        setSbomUnavailable(false)
      } else if (sbomRes.status === 404) {
        setSbomUnavailable(true)
      } else {
        showSimpleError(getApiResponseErrorMessage(sbomRes))
      }
      setLoadingSbom(false)

      if (vulnRes.ok) {
        setVulnerabilities(vulnRes.data)
      } else if (vulnRes.status !== 404) {
        showSimpleError(getApiResponseErrorMessage(vulnRes))
      }

      if (versionRes.ok) {
        setVersionInfo(versionRes.data)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [])

  const vulnerabilityIndex = useMemo(() => {
    const index = new Map<string, IVulnerability[]>()
    for (const c of vulnerabilities?.vulnerabilities ?? []) {
      index.set(c.componentKey, c.vulnerabilities)
    }
    return index
  }, [vulnerabilities])

  const allRows = useMemo<IComponentRow[]>(() => {
    const collect = (components: ISbomComponent[] | undefined, source: ComponentSource) =>
      (components ?? []).map<IComponentRow>((component) => {
        const componentKey = buildComponentKey(component, source)
        return {
          source,
          name: component.name ?? '',
          group: component.group ?? '',
          version: component.version ?? '',
          type: component.type ?? '',
          purl: component.purl ?? '',
          licenses: component.licenses ?? [],
          description: component.description ?? '',
          componentKey,
          vulnerabilities: vulnerabilityIndex.get(componentKey) ?? [],
        }
      })

    return [
      ...collect(sbom?.server?.components, 'server'),
      ...collect(sbom?.client?.components, 'client'),
    ]
  }, [sbom, vulnerabilityIndex])

  const serverCount = sbom?.server?.components?.length ?? 0
  const clientCount = sbom?.client?.components?.length ?? 0
  const totalCount = serverCount + clientCount

  const filteredRows = useMemo(() => {
    const needle = filterText.trim().toLowerCase()
    let rows = allRows
    if (sourceFilter !== 'all') {
      rows = rows.filter((row) => row.source === sourceFilter)
    }
    if (showOnlyVulnerable) {
      rows = rows.filter((row) => row.vulnerabilities.length > 0)
    }
    if (needle) {
      rows = rows.filter((row) => {
        const haystack = [
          row.name,
          row.group,
          row.version,
          row.type,
          row.licenses.join(' '),
          row.description,
        ]
          .join(' ')
          .toLowerCase()
        return haystack.includes(needle)
      })
    }

    const direction = sortStatus.direction === 'asc' ? 1 : -1
    const accessor = sortStatus.columnAccessor as keyof IComponentRow
    return [...rows].sort((a, b) => {
      if (accessor === 'vulnerabilities') {
        const aRank = SEVERITY_RANK[highestSeverity(a.vulnerabilities) ?? 'UNKNOWN']
        const bRank = SEVERITY_RANK[highestSeverity(b.vulnerabilities) ?? 'UNKNOWN']
        if (aRank !== bRank) {
          return (aRank - bRank) * direction
        }
        return (a.vulnerabilities.length - b.vulnerabilities.length) * direction
      }
      const aVal = String(a[accessor] ?? '').toLowerCase()
      const bVal = String(b[accessor] ?? '').toLowerCase()
      return aVal.localeCompare(bVal) * direction
    })
  }, [allRows, filterText, sourceFilter, showOnlyVulnerable, sortStatus])

  const handleRefreshVulnerabilities = async () => {
    setLoadingVulnerabilities(true)
    try {
      const response = await refreshVulnerabilities()
      if (response.ok) {
        setVulnerabilities(response.data)
        showSimpleSuccess('Refreshed vulnerability data from OSV.dev')
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setLoadingVulnerabilities(false)
    }
  }

  const handleSendEmail = async () => {
    setSendingEmail(true)
    try {
      const response = await sendVulnerabilityEmail()
      if (response.ok) {
        showSimpleSuccess('Vulnerability scan email sent to the configured admin address')
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setSendingEmail(false)
    }
  }

  const handleDownload = async (source: ComponentSource) => {
    const response = source === 'server' ? await fetchServerSbom() : await fetchClientSbom()
    if (response.ok) {
      downloadJson(`${source}-sbom.json`, response.data)
    } else {
      showSimpleError(getApiResponseErrorMessage(response))
    }
  }

  const upgradeBanner = (() => {
    if (!versionInfo?.updateAvailable) {
      return null
    }
    const critical = vulnerabilities?.criticalCount ?? 0
    const high = vulnerabilities?.highCount ?? 0
    const color = critical > 0 ? 'red' : high > 0 ? 'orange' : 'blue'
    const icon = critical > 0 || high > 0 ? <ShieldWarning /> : <Info />
    return (
      <Alert color={color} icon={icon} title='Update available'>
        <Stack gap={4}>
          <Text size='sm'>
            Current version: <strong>{versionInfo.currentVersion}</strong>
            {' — '}
            Latest version: <strong>{versionInfo.latestVersion}</strong>
          </Text>
          {(critical > 0 || high > 0) && (
            <Text size='sm'>
              High or critical vulnerabilities are present. Upgrading is recommended.
            </Text>
          )}
          {versionInfo.releaseUrl && (
            <Anchor href={versionInfo.releaseUrl} target='_blank' rel='noreferrer' size='sm'>
              View release notes
            </Anchor>
          )}
        </Stack>
      </Alert>
    )
  })()

  const columns: DataTableColumn<IComponentRow>[] = [
    {
      accessor: 'name',
      title: 'Name',
      sortable: true,
      render: (row) => (
        <Stack gap={2}>
          <Text fw={500}>{row.name}</Text>
          {row.description && (
            <Text size='xs' c='dimmed' lineClamp={2}>
              {row.description}
            </Text>
          )}
        </Stack>
      ),
    },
    {
      accessor: 'group',
      title: 'Group',
      sortable: true,
      render: (row) => row.group || <Text c='dimmed'>—</Text>,
    },
    {
      accessor: 'version',
      title: 'Version',
      sortable: true,
      render: (row) => <Badge variant='light'>{row.version}</Badge>,
    },
    {
      accessor: 'vulnerabilities',
      title: 'Security',
      sortable: true,
      render: (row) => {
        if (row.vulnerabilities.length === 0) {
          return (
            <Group gap={4}>
              <ShieldCheck color='var(--mantine-color-green-6)' />
              <Text size='sm' c='dimmed'>
                None
              </Text>
            </Group>
          )
        }
        const top = highestSeverity(row.vulnerabilities) ?? 'UNKNOWN'
        const visible = row.vulnerabilities.slice(0, 3)
        const overflow = row.vulnerabilities.length - visible.length
        return (
          <Stack gap={4}>
            <Group gap={6}>
              <Badge color={severityColor(top)} variant='filled'>
                {top}
              </Badge>
              <Text size='xs' c='dimmed'>
                {row.vulnerabilities.length} issue{row.vulnerabilities.length === 1 ? '' : 's'}
              </Text>
            </Group>
            <Group gap={4}>
              {visible.map((vuln) => (
                <Anchor
                  key={vuln.id}
                  href={`https://osv.dev/vulnerability/${vuln.id}`}
                  target='_blank'
                  rel='noreferrer'
                  size='xs'
                >
                  {vuln.id}
                </Anchor>
              ))}
              {overflow > 0 && (
                <Text size='xs' c='dimmed'>
                  +{overflow} more
                </Text>
              )}
            </Group>
          </Stack>
        )
      },
    },
    {
      accessor: 'licenses',
      title: 'Licenses',
      render: (row) =>
        row.licenses.length === 0 ? (
          <Text c='dimmed'>—</Text>
        ) : (
          <Group gap={4}>
            {row.licenses.map((license) => (
              <Badge key={license} color='teal' variant='light'>
                {license}
              </Badge>
            ))}
          </Group>
        ),
    },
    {
      accessor: 'source',
      title: 'Source',
      render: (row) => (
        <Badge color={row.source === 'server' ? 'blue' : 'pink'} variant='light'>
          {row.source === 'server' ? 'Server' : 'Client'}
        </Badge>
      ),
    },
  ]

  if (loadingSbom) {
    return (
      <Stack align='center' mt='xl'>
        <Loader />
      </Stack>
    )
  }

  return (
    <Stack>
      <Group justify='space-between' align='flex-end'>
        <Stack gap={4}>
          <Title order={1}>Dependencies</Title>
          <Text c='dimmed'>
            All bundled server and client dependencies, with security advisories from{' '}
            <Anchor href='https://osv.dev/' target='_blank' rel='noreferrer'>
              osv.dev
            </Anchor>
            .
          </Text>
        </Stack>
        <Group>
          <Button
            leftSection={<ArrowClockwise />}
            loading={loadingVulnerabilities}
            onClick={() => {
              void handleRefreshVulnerabilities()
            }}
            disabled={sbomUnavailable}
          >
            Refresh vulnerabilities
          </Button>
          <Button
            variant='light'
            leftSection={<Envelope />}
            loading={sendingEmail}
            onClick={() => {
              void handleSendEmail()
            }}
            disabled={sbomUnavailable}
          >
            Send email
          </Button>
          <Button
            variant='subtle'
            leftSection={<DownloadSimple />}
            onClick={() => {
              void handleDownload('server')
            }}
            disabled={!sbom?.server}
          >
            Server SBOM
          </Button>
          <Button
            variant='subtle'
            leftSection={<DownloadSimple />}
            onClick={() => {
              void handleDownload('client')
            }}
            disabled={!sbom?.client}
          >
            Client SBOM
          </Button>
        </Group>
      </Group>

      {upgradeBanner}

      {sbomUnavailable && (
        <Alert color='yellow' icon={<WarningCircle />} title='SBOM not bundled'>
          The currently running build does not include a Software Bill of Materials. Dependency
          listing and vulnerability scanning are disabled. Ensure your build runs the SBOM-bundling
          resource step (the committed <code>server/sbom/bom.json</code> and{' '}
          <code>client/sbom/bom.json</code> files are copied into the jar via{' '}
          <code>processResources</code>).
        </Alert>
      )}

      <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }} spacing='md'>
        <Card withBorder>
          <Text c='dimmed' size='sm'>
            Total components
          </Text>
          <Title order={2}>{totalCount}</Title>
        </Card>
        <Card withBorder>
          <Group justify='space-between'>
            <Stack gap={0}>
              <Text c='dimmed' size='sm'>
                Server
              </Text>
              <Title order={2}>{serverCount}</Title>
            </Stack>
            <ShieldCheck size={32} color='var(--mantine-color-blue-6)' />
          </Group>
        </Card>
        <Card withBorder>
          <Group justify='space-between'>
            <Stack gap={0}>
              <Text c='dimmed' size='sm'>
                Client
              </Text>
              <Title order={2}>{clientCount}</Title>
            </Stack>
            <Desktop size={32} color='var(--mantine-color-pink-6)' />
          </Group>
        </Card>
        <Card withBorder>
          <Text c='dimmed' size='sm'>
            Vulnerabilities
          </Text>
          <Title order={2}>{vulnerabilities?.totalVulnerabilities ?? 0}</Title>
          <Group gap={6} mt={6}>
            {(vulnerabilities?.criticalCount ?? 0) > 0 && (
              <Badge color='red'>Critical {vulnerabilities?.criticalCount}</Badge>
            )}
            {(vulnerabilities?.highCount ?? 0) > 0 && (
              <Badge color='orange'>High {vulnerabilities?.highCount}</Badge>
            )}
            {(vulnerabilities?.mediumCount ?? 0) > 0 && (
              <Badge color='yellow'>Medium {vulnerabilities?.mediumCount}</Badge>
            )}
            {(vulnerabilities?.lowCount ?? 0) > 0 && (
              <Badge color='green'>Low {vulnerabilities?.lowCount}</Badge>
            )}
          </Group>
        </Card>
      </SimpleGrid>

      <Card withBorder>
        <Stack>
          <Group justify='space-between' align='flex-end' wrap='wrap'>
            <TextInput
              placeholder='Search name, group, version, license…'
              value={filterText}
              onChange={(e) => setFilterText(e.currentTarget.value)}
              style={{ flex: 1, minWidth: 240 }}
            />
            <SegmentedControl
              value={sourceFilter}
              onChange={(value: SourceFilter) => setSourceFilter(value)}
              data={[
                { value: 'all', label: `All (${totalCount})` },
                { value: 'server', label: `Server (${serverCount})` },
                { value: 'client', label: `Client (${clientCount})` },
              ]}
            />
            <Switch
              checked={showOnlyVulnerable}
              onChange={(e) => setShowOnlyVulnerable(e.currentTarget.checked)}
              label='Only vulnerable'
              disabled={(vulnerabilities?.totalVulnerabilities ?? 0) === 0}
            />
          </Group>
          <Text size='xs' c='dimmed'>
            Showing {filteredRows.length} of {totalCount} components
            {vulnerabilities?.lastChecked && (
              <>
                {' '}
                · vulnerability data last checked{' '}
                {new Date(vulnerabilities.lastChecked).toLocaleString()}
              </>
            )}
          </Text>
          <DataTable
            records={filteredRows}
            columns={columns}
            sortStatus={sortStatus}
            onSortStatusChange={setSortStatus}
            withTableBorder
            withColumnBorders
            highlightOnHover
            idAccessor='componentKey'
            noRecordsText='No components match the current filters'
            minHeight={filteredRows.length === 0 ? 160 : undefined}
          />
        </Stack>
      </Card>
    </Stack>
  )
}

export default DependencyOverviewPage
