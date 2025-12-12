import { useEffect, useState } from 'react'
import { Button, Group, Stack, TextInput } from '@mantine/core'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { useForm } from '@mantine/form'
import { doRequest } from '../../../requests/request'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { showSimpleError } from '../../../utils/notification'
import { showNotification } from '@mantine/notifications'
import { useParams } from 'react-router'
import {
  IResearchGroupSettings,
  IResearchGroupSettingsEmail,
} from '../../../requests/responses/researchGroupSettings'

interface EmailSettingsCardProps {
  researchgroupEmailSettings?: IResearchGroupSettingsEmail
  setResearchgroupEmailSettings: (data: IResearchGroupSettingsEmail) => void
}

const EmailSettingsCard = ({
  researchgroupEmailSettings,
  setResearchgroupEmailSettings,
}: EmailSettingsCardProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()
  const [saving, setSaving] = useState(false)

  const form = useForm({
    initialValues: {
      applicationNotificationEmail: researchgroupEmailSettings?.applicationNotificationEmail ?? '',
    },
    validateInputOnChange: true,
    validate: {
      applicationNotificationEmail: (value) => {
        const trimmed = value.trim()
        if (!trimmed) return null
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed) ? null : 'Enter a valid email address'
      },
    },
  })

  useEffect(() => {
    form.setValues({
      applicationNotificationEmail: researchgroupEmailSettings?.applicationNotificationEmail ?? '',
    })
  }, [researchgroupEmailSettings?.applicationNotificationEmail])

  const hasChanges =
    (researchgroupEmailSettings?.applicationNotificationEmail ?? '') !==
    form.values.applicationNotificationEmail

  const updateEmailSettings = () => {
    setSaving(true)
    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          emailSettings: {
            applicationNotificationEmail: form.values.applicationNotificationEmail.trim() || null,
          },
        },
      },
      (res) => {
        setSaving(false)
        if (res.ok) {
          if (
            res.data.emailSettings.applicationNotificationEmail !==
            researchgroupEmailSettings?.applicationNotificationEmail
          ) {
            setResearchgroupEmailSettings(res.data.emailSettings)
          }
          showNotification({
            title: 'Success',
            message: 'Application notification email settings updated successfully.',
            color: 'green',
          })
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <ResearchGroupSettingsCard title='Email Settings' subtle='Manage additional email settings.'>
      <form onSubmit={form.onSubmit(updateEmailSettings)}>
        <Stack>
          <TextInput
            label='Additional application notification email (optional)'
            description='Send a copy of every new application notification (with attachments) to an additional address, even if advisors or supervisors muted their alerts. Leave empty to turn this off.'
            placeholder='notifications@example.edu'
            value={form.values.applicationNotificationEmail}
            onChange={(event) =>
              form.setFieldValue('applicationNotificationEmail', event.currentTarget.value)
            }
            error={form.errors.applicationNotificationEmail}
            disabled={saving}
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

export default EmailSettingsCard
