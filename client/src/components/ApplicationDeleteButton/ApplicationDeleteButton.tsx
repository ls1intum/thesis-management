import { doRequest } from '../../requests/request'
import { ApplicationState, IApplication } from '../../requests/responses/application'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'
import { Button, Modal, Stack, Text, Tooltip, type ButtonProps } from '@mantine/core'
import React, { useState } from 'react'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { useAuthenticationContext } from '../../hooks/authentication'

interface IApplicationDeleteButtonProps extends ButtonProps {
  application: IApplication
  onDelete?: () => void
}

const ApplicationDeleteButton = (props: IApplicationDeleteButtonProps) => {
  const { application, onDelete, ...buttonProps } = props

  const auth = useAuthenticationContext()

  const [confirmationModal, setConfirmationModal] = useState(false)
  const [loading, setLoading] = useState(false)

  if (!auth.user?.groups?.includes('admin')) {
    return <></>
  }

  const isAccepted = application.state === ApplicationState.ACCEPTED

  const handleDelete = async () => {
    setLoading(true)

    try {
      const response = await doRequest<void>(`/v2/applications/${application.applicationId}`, {
        method: 'DELETE',
        requiresAuth: true,
      })

      if (response.ok) {
        showSimpleSuccess('Application deleted successfully')
        setConfirmationModal(false)
        onDelete?.()
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setLoading(false)
    }
  }

  const button = (
    <Button
      {...buttonProps}
      variant='outline'
      loading={loading}
      color='red'
      disabled={isAccepted}
      onClick={() => setConfirmationModal(true)}
    >
      {buttonProps.children ?? 'Delete'}
    </Button>
  )

  return (
    <>
      {isAccepted ? (
        <Tooltip label='Accepted applications cannot be deleted because they are linked to a thesis'>
          {button}
        </Tooltip>
      ) : (
        button
      )}
      <Modal
        title='Delete Application'
        opened={confirmationModal}
        onClick={(e) => e.stopPropagation()}
        onClose={() => setConfirmationModal(false)}
        centered
      >
        <Stack>
          <Text>
            Are you sure you want to permanently delete this application? This action cannot be
            undone.
          </Text>
          <Button onClick={handleDelete} loading={loading} color='red' fullWidth>
            Delete Application
          </Button>
        </Stack>
      </Modal>
    </>
  )
}

export default ApplicationDeleteButton
