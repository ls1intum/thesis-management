import { doRequest } from '../../requests/request'
import { ApplicationState, IApplication } from '../../requests/responses/application'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'
import { Button, Checkbox, Modal, Select, Stack, Text } from '@mantine/core'
import React, { useEffect, useState } from 'react'
import { ButtonProps } from '@mantine/core/lib/components/Button/Button'
import { useApplicationsContextUpdater } from '../../contexts/ApplicationsProvider/hooks'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { isNotEmpty, useForm } from '@mantine/form'

interface IApplicationRejectButtonProps extends ButtonProps {
  application: IApplication
  onUpdate: (application: IApplication) => unknown
}

const ApplicationRejectButton = (props: IApplicationRejectButtonProps) => {
  const { application, onUpdate, ...buttonProps } = props

  const updateApplication = useApplicationsContextUpdater()

  const [confirmationModal, setConfirmationModal] = useState(false)
  const [loading, setLoading] = useState(false)

  const form = useForm<{
    notifyUser: boolean
    reason: string | null
  }>({
    mode: 'controlled',
    initialValues: {
      notifyUser: true,
      reason: null,
    },
    validateInputOnBlur: true,
    validate: {
      reason: isNotEmpty('Reason is required'),
    },
  })

  useEffect(() => {
    form.reset()
  }, [confirmationModal])

  if (application.state !== ApplicationState.NOT_ASSESSED) {
    return <></>
  }

  return (
    <Button
      {...buttonProps}
      variant='outline'
      loading={loading}
      color='red'
      onClick={() => setConfirmationModal(true)}
    >
      <Modal
        title='Reject Application'
        opened={confirmationModal}
        onClick={(e) => e.stopPropagation()}
        onClose={() => setConfirmationModal(false)}
      >
        <form
          onSubmit={form.onSubmit(async (values) => {
            setLoading(true)

            try {
              const response = await doRequest<IApplication>(
                `/v2/applications/${application.applicationId}/reject`,
                {
                  method: 'PUT',
                  requiresAuth: true,
                  data: {
                    reason: values.reason,
                    notifyUser: values.notifyUser,
                  },
                },
              )

              if (response.ok) {
                showSimpleSuccess('Application rejected successfully')

                updateApplication(response.data)
                onUpdate(response.data)
              } else {
                showSimpleError(getApiResponseErrorMessage(response))
              }
            } finally {
              setLoading(false)
            }
          })}
        >
          <Stack>
            <Text>Please specify a reason why you want to reject the student</Text>
            <Select
              label='Reason'
              required
              data={[
                {
                  value: 'NO_CAPACITY',
                  label: 'No capacity at the moment',
                },
                application.topic
                  ? {
                      value: 'FAILED_REQUIREMENTS',
                      label: 'Student does not fulfil the requirements of the topic',
                    }
                  : {
                      value: 'TITLE_NOT_INTERESTING',
                      label: 'Suggested thesis topic is not interesting',
                    },
                {
                  value: 'BAD_GRADES',
                  label: 'Student has bad grades or did not attend required lectures',
                },
              ]}
              {...form.getInputProps('reason')}
            />
            <Checkbox
              label='Notify Student'
              required
              {...form.getInputProps('notifyUser', { type: 'checkbox' })}
            />
            <Button type='submit' loading={loading} disabled={!form.isValid()} fullWidth>
              Reject Application
            </Button>
          </Stack>
        </form>
      </Modal>
      Reject
    </Button>
  )
}

export default ApplicationRejectButton
