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
  IResearchGroupSettingsWritingGuide,
} from '../../../requests/responses/researchGroupSettings'

interface ScientificWritingGuideSettingsCardProps {
  writingGuideSettings?: IResearchGroupSettingsWritingGuide
  setWritingGuideSettings: (data: IResearchGroupSettingsWritingGuide) => void
}

const ScientificWritingGuideSettingsCard = ({
  writingGuideSettings,
  setWritingGuideSettings,
}: ScientificWritingGuideSettingsCardProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()
  const [saving, setSaving] = useState(false)

  const form = useForm({
    initialValues: {
      scientificWritingGuideLink: writingGuideSettings?.scientificWritingGuideLink ?? '',
    },
    validateInputOnChange: true,
    validate: {
      scientificWritingGuideLink: (value) => {
        const trimmed = value.trim()
        if (!trimmed) return null
        try {
          new URL(trimmed)
          return null
        } catch {
          return 'Enter a valid URL'
        }
      },
    },
  })

  useEffect(() => {
    form.setValues({
      scientificWritingGuideLink: writingGuideSettings?.scientificWritingGuideLink ?? '',
    })
  }, [writingGuideSettings?.scientificWritingGuideLink])

  const hasChanges =
    (writingGuideSettings?.scientificWritingGuideLink ?? '') !==
    form.values.scientificWritingGuideLink

  const updateWritingGuideSettings = () => {
    setSaving(true)
    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          writingGuideSettings: {
            scientificWritingGuideLink: form.values.scientificWritingGuideLink.trim() || null,
          },
        },
      },
      (res) => {
        setSaving(false)
        if (res.ok) {
          if (
            res.data.writingGuideSettings.scientificWritingGuideLink !==
            writingGuideSettings?.scientificWritingGuideLink
          ) {
            setWritingGuideSettings(res.data.writingGuideSettings)
          }
          showNotification({
            title: 'Success',
            message: 'Scientific writing guide settings updated successfully.',
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
      title='Scientific Writing Guide'
      subtle='Link to a guide that explains scientific writing standards for this research group.'
    >
      <form onSubmit={form.onSubmit(updateWritingGuideSettings)}>
        <Stack>
          <TextInput
            label='Scientific writing guide URL (optional)'
            description='Students will see a dashboard task linking to this guide. Leave empty to disable.'
            placeholder='https://example.com/writing-guide'
            value={form.values.scientificWritingGuideLink}
            onChange={(event) =>
              form.setFieldValue('scientificWritingGuideLink', event.currentTarget.value)
            }
            error={form.errors.scientificWritingGuideLink}
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

export default ScientificWritingGuideSettingsCard
