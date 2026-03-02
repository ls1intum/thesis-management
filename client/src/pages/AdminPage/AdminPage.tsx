import React, { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Group,
  Loader,
  Modal,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core'
import { Warning } from '@phosphor-icons/react'
import { doRequest } from '../../requests/request'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { IUser } from '../../requests/responses/user'

interface IDataRetentionResult {
  deletedApplications: number
}

interface IAnonymizationResult {
  anonymizedTheses: number
}

interface IDeletionPreview {
  canBeFullyDeleted: boolean
  retentionBlockedThesisCount: number
  earliestFullDeletionDate?: string
  isResearchGroupHead: boolean
  message: string
}

interface IDeletionResult {
  result: string
  message: string
}

interface IPageResponse<T> {
  content: T[]
}

const AdminPage = () => {
  const [loading, setLoading] = useState(false)
  const [anonymizationLoading, setAnonymizationLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<IUser[]>([])
  const [searching, setSearching] = useState(false)
  const [selectedUser, setSelectedUser] = useState<IUser | null>(null)
  const [deletionPreview, setDeletionPreview] = useState<IDeletionPreview | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)

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

  const onRunAnonymization = async () => {
    setAnonymizationLoading(true)

    try {
      const response = await doRequest<IAnonymizationResult>(
        '/v2/data-retention/anonymize-expired-theses',
        {
          method: 'POST',
          requiresAuth: true,
        },
      )

      if (response.ok) {
        if (response.data.anonymizedTheses > 0) {
          showSimpleSuccess(`Anonymized ${response.data.anonymizedTheses} expired thesis/theses`)
        } else {
          showSimpleSuccess('No expired theses found')
        }
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setAnonymizationLoading(false)
    }
  }

  const onSearchUsers = async () => {
    if (!searchQuery.trim()) return
    setSearching(true)
    setSelectedUser(null)
    setDeletionPreview(null)
    try {
      const response = await doRequest<IPageResponse<IUser>>('/v2/users', {
        method: 'GET',
        requiresAuth: true,
        params: { searchQuery: searchQuery.trim(), page: 0, limit: 10 },
      })
      if (response.ok) {
        setSearchResults(response.data.content ?? [])
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setSearching(false)
    }
  }

  const onSelectUser = async (user: IUser) => {
    setSelectedUser(user)
    setPreviewLoading(true)
    try {
      const response = await doRequest<IDeletionPreview>(
        `/v2/user-deletion/${user.userId}/preview`,
        {
          method: 'GET',
          requiresAuth: true,
        },
      )
      if (response.ok) {
        setDeletionPreview(response.data)
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setPreviewLoading(false)
    }
  }

  const onDeleteUser = async () => {
    if (!selectedUser) return
    setConfirmOpen(false)
    setDeleting(true)
    try {
      const response = await doRequest<IDeletionResult>(
        `/v2/user-deletion/${selectedUser.userId}`,
        {
          method: 'DELETE',
          requiresAuth: true,
        },
      )
      if (response.ok) {
        showSimpleSuccess(response.data.message)
        setSelectedUser(null)
        setDeletionPreview(null)
        setSearchResults([])
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setDeleting(false)
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
      <Card withBorder>
        <Stack>
          <Title order={3}>Thesis Anonymization</Title>
          <Text>
            Anonymize theses that have exceeded the 5-year legal retention period. Personal data
            (files, comments, assessments, feedback, role assignments) will be permanently removed
            while preserving the thesis record (title, type, grade, dates) for statistical purposes.
          </Text>
          <Group>
            <Button loading={anonymizationLoading} onClick={onRunAnonymization}>
              Run Anonymization
            </Button>
          </Group>
        </Stack>
      </Card>
      <Card withBorder>
        <Stack>
          <Title order={3}>User Account Deletion</Title>
          <Text>Search for a user to preview and perform account deletion.</Text>
          <Group>
            <TextInput
              placeholder='Search by name, email, or ID...'
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.currentTarget.value)}
              onKeyDown={(e) => e.key === 'Enter' && onSearchUsers()}
              style={{ flex: 1 }}
            />
            <Button loading={searching} onClick={onSearchUsers}>
              Search
            </Button>
          </Group>
          {searchResults.length > 0 && (
            <Stack gap='xs'>
              {searchResults.map((user) => (
                <Button
                  key={user.userId}
                  variant={selectedUser?.userId === user.userId ? 'filled' : 'light'}
                  onClick={() => onSelectUser(user)}
                  justify='flex-start'
                >
                  {user.firstName} {user.lastName} ({user.universityId})
                </Button>
              ))}
            </Stack>
          )}
          {previewLoading && <Loader />}
          {selectedUser && deletionPreview && !previewLoading && (
            <Stack>
              <Text fw={500}>
                Deletion preview for: {selectedUser.firstName} {selectedUser.lastName}
              </Text>
              <Text>{deletionPreview.message}</Text>
              {deletionPreview.isResearchGroupHead && (
                <Alert color='orange' icon={<Warning />}>
                  This user is a research group head.
                </Alert>
              )}
              <Group>
                <Button
                  color='red'
                  disabled={deletionPreview.isResearchGroupHead}
                  loading={deleting}
                  onClick={() => setConfirmOpen(true)}
                >
                  Delete User
                </Button>
              </Group>
            </Stack>
          )}
        </Stack>
      </Card>

      <Modal
        opened={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        title='Confirm User Deletion'
      >
        <Stack>
          <Alert color='red' icon={<Warning />}>
            This will {deletionPreview?.canBeFullyDeleted ? 'permanently delete' : 'deactivate'} the
            account of {selectedUser?.firstName} {selectedUser?.lastName}. This action cannot be
            undone.
          </Alert>
          <Group grow>
            <Button variant='outline' onClick={() => setConfirmOpen(false)}>
              Cancel
            </Button>
            <Button color='red' onClick={onDeleteUser}>
              Confirm Deletion
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}

export default AdminPage
