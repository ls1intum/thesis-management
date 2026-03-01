import { Alert, Badge, Button, Group, Stack, Text, Title } from '@mantine/core'
import { useEffect, useState } from 'react'
import { doRequest } from '../../../../requests/request'
import { showSimpleError, showSimpleSuccess } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { downloadFile } from '../../../../utils/blob'

interface DataExportStatus {
  id?: string
  state?: string
  createdAt?: string
  creationFinishedAt?: string
  downloadedAt?: string
  canRequest: boolean
  nextRequestDate?: string
}

const DataExport = () => {
  const [status, setStatus] = useState<DataExportStatus | null>(null)
  const [loading, setLoading] = useState(false)
  const [requesting, setRequesting] = useState(false)
  const [downloading, setDownloading] = useState(false)

  const fetchStatus = async () => {
    setLoading(true)
    try {
      const response = await doRequest<DataExportStatus>('/v2/data-exports/status', {
        method: 'GET',
        requiresAuth: true,
      })
      if (response.ok) {
        setStatus(response.data)
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchStatus()
  }, [])

  const onRequest = async () => {
    setRequesting(true)
    try {
      const response = await doRequest<DataExportStatus>('/v2/data-exports', {
        method: 'POST',
        requiresAuth: true,
      })
      if (response.ok) {
        showSimpleSuccess('Data export requested. You will receive an email when it is ready.')
        await fetchStatus()
      } else if (response.status === 429) {
        showSimpleError('You can only request one data export per 7 days.')
        await fetchStatus()
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setRequesting(false)
    }
  }

  const onDownload = async () => {
    if (!status?.id) return
    setDownloading(true)
    try {
      const response = await doRequest<Blob>(`/v2/data-exports/${status.id}/download`, {
        method: 'GET',
        requiresAuth: true,
        responseType: 'blob',
      })
      if (response.ok) {
        downloadFile(new File([response.data], 'data_export.zip', { type: 'application/zip' }))
        await fetchStatus()
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setDownloading(false)
    }
  }

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return ''
    return new Date(dateStr).toLocaleString()
  }

  const getStateBadge = () => {
    if (!status?.state) return null

    const stateConfig: Record<string, { color: string; label: string }> = {
      REQUESTED: { color: 'blue', label: 'Processing' },
      IN_CREATION: { color: 'blue', label: 'Processing' },
      EMAIL_SENT: { color: 'green', label: 'Ready for Download' },
      EMAIL_FAILED: { color: 'green', label: 'Ready for Download' },
      DOWNLOADED: { color: 'teal', label: 'Downloaded' },
      DELETED: { color: 'gray', label: 'Expired' },
      DOWNLOADED_DELETED: { color: 'gray', label: 'Expired' },
      FAILED: { color: 'red', label: 'Failed' },
    }

    const config = stateConfig[status.state] ?? { color: 'gray', label: status.state }
    return <Badge color={config.color}>{config.label}</Badge>
  }

  const isDownloadable =
    status?.state === 'EMAIL_SENT' ||
    status?.state === 'EMAIL_FAILED' ||
    status?.state === 'DOWNLOADED'

  const isProcessing = status?.state === 'REQUESTED' || status?.state === 'IN_CREATION'

  return (
    <Stack>
      <Title order={3}>Data Export</Title>
      <Text>
        You can request an export of all your personal data stored in the system. This includes your
        profile information, applications, theses, and uploaded documents. The export is generated
        as a ZIP file containing structured JSON data and your uploaded files.
      </Text>

      <Text size='sm' c='dimmed'>
        Exports are processed overnight and you will receive an email when your export is ready. The
        download link is valid for 7 days. You can request a new export every 7 days.
      </Text>

      {status?.state && (
        <Alert variant='light' color='gray' title='Latest Export'>
          <Stack gap='xs'>
            <Group gap='sm'>
              <Text fw={500}>Status:</Text>
              {getStateBadge()}
            </Group>
            {status.createdAt && (
              <Group gap='sm'>
                <Text fw={500}>Requested:</Text>
                <Text>{formatDate(status.createdAt)}</Text>
              </Group>
            )}
            {status.downloadedAt && (
              <Group gap='sm'>
                <Text fw={500}>Downloaded:</Text>
                <Text>{formatDate(status.downloadedAt)}</Text>
              </Group>
            )}
            {isProcessing && (
              <Text size='sm' c='dimmed'>
                Your export is being processed. You will receive an email when it is ready.
              </Text>
            )}
            {status.state === 'FAILED' && (
              <Text size='sm' c='red'>
                The export generation failed. You can request a new export.
              </Text>
            )}
          </Stack>
        </Alert>
      )}

      <Group>
        {isDownloadable && (
          <Button onClick={onDownload} loading={downloading}>
            Download Export
          </Button>
        )}
        <Button
          onClick={onRequest}
          loading={requesting}
          disabled={loading || !status?.canRequest || isProcessing}
          variant={isDownloadable ? 'light' : 'filled'}
        >
          Request Data Export
        </Button>
      </Group>

      {!status?.canRequest && status?.nextRequestDate && !isProcessing && (
        <Text size='sm' c='dimmed'>
          Next export can be requested after {formatDate(status.nextRequestDate)}.
        </Text>
      )}
    </Stack>
  )
}

export default DataExport
