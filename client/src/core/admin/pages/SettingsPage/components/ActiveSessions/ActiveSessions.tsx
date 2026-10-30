import { Badge, Button, Group, Loader, Paper, Stack, Text, Title } from '@mantine/core'
import { useCallback, useEffect, useState } from 'react'
import { doRequest } from '@/core/requests/request'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'

interface IUserSession {
  id: string
  createdAt?: string
  lastUsedAt?: string
  ip?: string
  device?: string
  current?: boolean
}

const formatDate = (value?: string) => (value ? new Date(value).toLocaleString() : 'unknown')

const ActiveSessions = () => {
  const [sessions, setSessions] = useState<IUserSession[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadFailed, setLoadFailed] = useState(false)
  const [endingSessionId, setEndingSessionId] = useState<string>()
  const [isEndingOthers, setIsEndingOthers] = useState(false)

  const loadSessions = useCallback(async () => {
    const response = await doRequest<IUserSession[]>('/v2/auth/sessions', {
      method: 'GET',
      requiresAuth: true,
    })

    if (response.ok) {
      setSessions(response.data)
      setLoadFailed(false)
    } else {
      setLoadFailed(true)
      showSimpleError(getApiResponseErrorMessage(response))
    }
  }, [])

  useEffect(() => {
    void loadSessions().finally(() => setIsLoading(false))
  }, [loadSessions])

  const endSession = async (sessionId: string) => {
    setEndingSessionId(sessionId)

    try {
      const response = await doRequest(`/v2/auth/sessions/${sessionId}`, {
        method: 'DELETE',
        requiresAuth: true,
      })

      if (response.ok) {
        showSimpleSuccess('Session ended.')
        await loadSessions()
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setEndingSessionId(undefined)
    }
  }

  const endOtherSessions = async () => {
    setIsEndingOthers(true)

    try {
      const response = await doRequest('/v2/auth/sessions', {
        method: 'DELETE',
        requiresAuth: true,
      })

      if (response.ok) {
        showSimpleSuccess('All other sessions ended.')
        await loadSessions()
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setIsEndingOthers(false)
    }
  }

  const hasOtherSessions = sessions.some((session) => !session.current)

  return (
    <Stack>
      <Title order={3}>Active sessions</Title>
      <Text>
        These are your active Thesis Management sessions. Ending a session invalidates its current
        access, but a browser still signed in with your university login can sign in again
        automatically. On a shared computer, use Logout before leaving.
      </Text>
      {isLoading && <Loader size='sm' />}
      {!isLoading && loadFailed && sessions.length === 0 && (
        <Text size='sm' c='red'>
          The active sessions could not be loaded. Please try again later.
        </Text>
      )}
      {!isLoading && !loadFailed && sessions.length === 0 && (
        <Text size='sm' c='dimmed'>
          No active sessions found.
        </Text>
      )}
      {sessions.map((session) => (
        <Paper withBorder p='sm' key={session.id}>
          <Group justify='space-between' align='center'>
            <Stack gap={0}>
              <Group gap='xs'>
                <Text fw={500}>{session.device ?? 'Unknown device'}</Text>
                {session.current && <Badge variant='light'>This device</Badge>}
              </Group>
              <Text size='sm' c='dimmed'>
                {session.ip ? `IP ${session.ip} · ` : ''}Signed in {formatDate(session.createdAt)} ·
                Last active {formatDate(session.lastUsedAt)}
              </Text>
            </Stack>
            {!session.current && (
              <Button
                color='red'
                variant='light'
                onClick={() => void endSession(session.id)}
                loading={endingSessionId === session.id}
                aria-label={`End session on ${session.device ?? 'unknown device'}`}
              >
                End session
              </Button>
            )}
          </Group>
        </Paper>
      ))}
      {hasOtherSessions && (
        <Group>
          <Button
            color='red'
            variant='outline'
            onClick={() => void endOtherSessions()}
            loading={isEndingOthers}
          >
            End all other sessions
          </Button>
        </Group>
      )}
    </Stack>
  )
}

export default ActiveSessions
