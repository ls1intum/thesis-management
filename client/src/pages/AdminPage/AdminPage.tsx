import React, { useState } from 'react'
import { Button, Card, Group, Stack, Text, Title } from '@mantine/core'
import { doRequest } from '../../requests/request'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'

interface IDataRetentionResult {
  deletedApplications: number
}

const AdminPage = () => {
  const [loading, setLoading] = useState(false)

  const onRunCleanup = async () => {
    setLoading(true)

    try {
      const response = await doRequest<IDataRetentionResult>(
        '/v2/data-retention/cleanup-rejected-applications',
        {
          method: 'POST',
          requiresAuth: true,
        },
      )

      if (response.ok) {
        if (response.data.deletedApplications > 0) {
          showSimpleSuccess(
            `Deleted ${response.data.deletedApplications} expired rejected application(s)`,
          )
        } else {
          showSimpleSuccess('No expired applications found')
        }
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Stack>
      <Title order={1}>Administration</Title>
      <Card withBorder>
        <Stack>
          <Title order={3}>Data Retention</Title>
          <Text>
            Manually trigger the data retention cleanup to permanently delete rejected applications
            that have exceeded the configured retention period.
          </Text>
          <Group>
            <Button loading={loading} onClick={onRunCleanup}>
              Run Cleanup
            </Button>
          </Group>
        </Stack>
      </Card>
    </Stack>
  )
}

export default AdminPage
