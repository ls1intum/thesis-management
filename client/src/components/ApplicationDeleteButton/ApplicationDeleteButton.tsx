import { doRequest } from '../../requests/request'
import { IApplication } from '../../requests/responses/application'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'
import { Button, Modal, Stack, Text, type ButtonProps } from '@mantine/core'
import React, { useState } from 'react'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { useNavigate } from 'react-router'
import { useAuthenticationContext } from '../../hooks/authentication'

interface IApplicationDeleteButtonProps extends ButtonProps {
  application: IApplication
}

const ApplicationDeleteButton = (props: IApplicationDeleteButtonProps) => {
  const { application, ...buttonProps } = props

  const auth = useAuthenticationContext()
  const navigate = useNavigate()

  const [confirmationModal, setConfirmationModal] = useState(false)
  const [loading, setLoading] = useState(false)

  if (!auth.user?.groups?.includes('admin')) {
    return <></>
  }

  const onDelete = async () => {
    setLoading(true)

    try {
      const response = await doRequest<void>(`/v2/applications/${application.applicationId}`, {
        method: 'DELETE',
        requiresAuth: true,
      })

      if (response.ok) {
        showSimpleSuccess('Application deleted successfully')
        setConfirmationModal(false)
        navigate('/applications')
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <Button
        {...buttonProps}
        variant='outline'
        loading={loading}
        color='red'
        onClick={() => setConfirmationModal(true)}
      >
        {buttonProps.children ?? 'Delete'}
      </Button>
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
          <Button onClick={onDelete} loading={loading} color='red' fullWidth>
            Delete Application
          </Button>
        </Stack>
      </Modal>
    </>
  )
}

export default ApplicationDeleteButton
