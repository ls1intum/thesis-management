import React, { useEffect, useState } from 'react'
import { Alert, Button, Group, Loader, Modal, Stack, Text, Title } from '@mantine/core'
import { Warning } from '@phosphor-icons/react'
import { doRequest } from '../../../../requests/request'
import { showSimpleError, showSimpleSuccess } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { useAuthenticationContext } from '../../../../hooks/authentication'

interface IDeletionPreview {
  canBeFullyDeleted: boolean
  hasActiveTheses: boolean
  retentionBlockedThesisCount: number
  earliestFullDeletionDate?: string
  isResearchGroupHead: boolean
  message: string
}

interface IDeletionResult {
  result: string
  message: string
}

const AccountDeletion = () => {
  const [preview, setPreview] = useState<IDeletionPreview | null>(null)
  const [loading, setLoading] = useState(true)
  const [deleting, setDeleting] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const auth = useAuthenticationContext()

  useEffect(() => {
    const fetchPreview = async () => {
      setLoading(true)
      try {
        const response = await doRequest<IDeletionPreview>('/v2/user-deletion/me/preview', {
          method: 'GET',
          requiresAuth: true,
        })
        if (response.ok) {
          setPreview(response.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(response))
        }
      } finally {
        setLoading(false)
      }
    }
    fetchPreview()
  }, [])

  const onDelete = async () => {
    setConfirmOpen(false)
    setDeleting(true)
    try {
      const response = await doRequest<IDeletionResult>('/v2/user-deletion/me', {
        method: 'DELETE',
        requiresAuth: true,
      })
      if (response.ok) {
        showSimpleSuccess(response.data.message)
        auth.logout(window.location.origin)
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setDeleting(false)
    }
  }

  if (loading) {
    return <Loader />
  }

  if (!preview) {
    return <Text>Failed to load deletion preview.</Text>
  }

  const canDelete = !preview.hasActiveTheses && !preview.isResearchGroupHead

  return (
    <Stack>
      <Title order={3}>Delete Account</Title>
      <Text>{preview.message}</Text>

      {preview.isResearchGroupHead && (
        <Alert color='orange' icon={<Warning />} title='Research Group Head'>
          You are currently head of a research group. Transfer leadership to another member before
          deleting your account.
        </Alert>
      )}

      {preview.hasActiveTheses && (
        <Alert color='orange' icon={<Warning />} title='Active Theses'>
          You have active theses that must be completed or dropped before you can delete your
          account.
        </Alert>
      )}

      {!preview.canBeFullyDeleted && canDelete && preview.retentionBlockedThesisCount > 0 && (
        <Alert color='blue' title='Data Retention Notice'>
          Due to legal retention requirements, {preview.retentionBlockedThesisCount} thesis
          record(s) and your profile data will be retained until{' '}
          {preview.earliestFullDeletionDate
            ? new Date(preview.earliestFullDeletionDate).toLocaleDateString()
            : 'the retention period expires'}
          . Your account will be deactivated and non-essential data deleted immediately.
        </Alert>
      )}

      <Group>
        <Button
          color='red'
          disabled={!canDelete}
          loading={deleting}
          onClick={() => setConfirmOpen(true)}
        >
          Delete My Account
        </Button>
      </Group>

      <Modal
        opened={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        title='Confirm Account Deletion'
      >
        <Stack>
          <Alert color='red' icon={<Warning />}>
            This action cannot be undone. Your account and personal data will be{' '}
            {preview.canBeFullyDeleted
              ? 'permanently deleted'
              : 'deactivated, with full deletion after the retention period'}
            .
          </Alert>
          <Text>Are you sure you want to proceed?</Text>
          <Group grow>
            <Button variant='outline' onClick={() => setConfirmOpen(false)}>
              Cancel
            </Button>
            <Button color='red' onClick={onDelete}>
              Yes, Delete My Account
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  )
}

export default AccountDeletion
