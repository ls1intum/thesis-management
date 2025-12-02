import { useEffect, useState } from 'react'
import { Button, Group, Stack, TextInput } from '@mantine/core'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { useForm } from '@mantine/form'
import { doRequest } from '../../../requests/request'
import { IResearchGroup } from '../../../requests/responses/researchGroup'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { showSimpleError } from '../../../utils/notification'
import { showNotification } from '@mantine/notifications'
import { useParams } from 'react-router'

interface ApplicationNotificationEmailCardProps {
  researchGroupData?: IResearchGroup
  setResearchGroupData: (data: IResearchGroup) => void
}

const ApplicationNotificationEmailCard = ({
  researchGroupData,
  setResearchGroupData,
}: ApplicationNotificationEmailCardProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()
  const [saving, setSaving] = useState(false)

  const form = useForm({
    initialValues: {
      applicationNotificationEmail: researchGroupData?.applicationNotificationEmail ?? '',
    },
    validateInputOnChange: true,
    validate: {
      applicationNotificationEmail: (value) => {
        const trimmed = value.trim()
        if (!trimmed) return null
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)
          ? null
          : 'Enter a valid email address'
      },
    },
  })

  useEffect(() => {
    form.setValues({
      applicationNotificationEmail: researchGroupData?.applicationNotificationEmail ?? '',
    })
  }, [researchGroupData?.applicationNotificationEmail])

  const hasChanges =
    (researchGroupData?.applicationNotificationEmail ?? '') !==
    form.values.applicationNotificationEmail

  const handleSubmit = (values: typeof form.values) => {
    if (!researchGroupId) return

    setSaving(true)
    const trimmedEmail = values.applicationNotificationEmail.trim()

    doRequest<IResearchGroup>(
      `/v2/research-groups/${researchGroupId}/application-notification-email`,
      {
        method: 'PUT',
        requiresAuth: true,
        data: {
          applicationNotificationEmail: trimmedEmail.length ? trimmedEmail : null,
        },
      },
      (res) => {
        setSaving(false)
        if (res.ok) {
          setResearchGroupData(res.data)
          showNotification({
            title: 'Notification email saved',
            message:
              'New application alerts will also be sent to this address, including attachments.',
            color: 'green',
          })
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <ResearchGroupSettingsCard
      title='Application Notifications'
      subtle='Send a copy of every new application notification (with attachments) to an additional address, even if advisors or supervisors muted their alerts. Leave empty to turn this off.'
    >
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Stack>
          <TextInput
            label='Additional notification email (optional)'
            placeholder='notifications@example.edu'
            value={form.values.applicationNotificationEmail}
            onChange={(event) =>
              form.setFieldValue('applicationNotificationEmail', event.currentTarget.value)
            }
            error={form.errors.applicationNotificationEmail}
            disabled={!researchGroupData?.id}
          />
          <Group justify='flex-end'>
            <Button
              type='submit'
              loading={saving}
              disabled={!hasChanges || Object.keys(form.errors).length > 0}
            >
              Save
            </Button>
          </Group>
        </Stack>
      </form>
    </ResearchGroupSettingsCard>
  )
}

export default ApplicationNotificationEmailCard
