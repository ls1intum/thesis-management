import { Alert, Divider, Group, NumberInput, Stack, Switch, Text } from '@mantine/core'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { useState, useEffect } from 'react'
import { useDebouncedValue } from '@mantine/hooks'
import { doRequest } from '../../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { IResearchGroupSettings } from '../../../requests/responses/researchGroupSettings'
import { WarningCircleIcon } from '@phosphor-icons/react'

interface AutomaticRejectionCardProps {
  automaticRejectionEnabledSettings: boolean
  setAutomaticRejectionEnabledSettings: (value: boolean) => void
  rejectDurationSettings: number
  setRejectDurationSettings: (value: number) => void
}

const AutomaticRejectionCard = ({
  automaticRejectionEnabledSettings,
  rejectDurationSettings,
  setAutomaticRejectionEnabledSettings,
  setRejectDurationSettings,
}: AutomaticRejectionCardProps) => {
  const [debouncedRejectDuration] = useDebouncedValue(rejectDurationSettings, 300)

  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  useEffect(() => {
    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}/automatic-reject`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          automaticRejectEnabled: automaticRejectionEnabledSettings,
          rejectDuration: debouncedRejectDuration,
        },
      },
      (res) => {
        if (res.ok) {
          if (res.data.automaticRejectEnabled !== automaticRejectionEnabledSettings) {
            setAutomaticRejectionEnabledSettings(res.data.automaticRejectEnabled)
          }
          if (res.data.rejectDuration !== rejectDurationSettings) {
            setRejectDurationSettings(res.data.rejectDuration)
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [debouncedRejectDuration, automaticRejectionEnabledSettings])

  return (
    <ResearchGroupSettingsCard
      title='Automatic Rejects'
      subtle='Configure automatic rejection settings for applications.'
    >
      <Stack>
        <Group justify='space-between' align='center' wrap='nowrap'>
          <Stack gap={2}>
            <Text size='sm' fw={500}>
              Enable automatic rejection
            </Text>
            <Text size='xs' c='dimmed'>
              Automatically reject applications after a specified time period.
            </Text>
          </Stack>
          <Switch
            checked={automaticRejectionEnabledSettings}
            onChange={(event) => setAutomaticRejectionEnabledSettings(event.currentTarget.checked)}
          />
        </Group>
        {!automaticRejectionEnabledSettings && (
          <Alert
            variant='light'
            color='orange'
            title='Automatic Rejection Warning'
            icon={<WarningCircleIcon size={16} />}
            mt='md'
          >
            Enabling automatic rejection will cause all open applications older than your set
            rejection time period to be rejected daily at 9:00 AM. Please review your application
            list before activating this feature to ensure no suitable candidates are unintentionally
            rejected.
          </Alert>
        )}
        {automaticRejectionEnabledSettings && (
          <Group ml={'1rem'} wrap='nowrap'>
            <Divider orientation='vertical' />
            <Stack gap={2}>
              <Text size='sm' fw={500}>
                Rejection Time Period
              </Text>
              <Text size='xs' c='dimmed'>
                Automatically reject applications after the selected number of weeks. The period
                starts after the application deadline, or if none is set, after the intended start
                date, otherwise from the application creation date. Applicants are never rejected
                earlier than two weeks after applying.
              </Text>
              <NumberInput
                placeholder="Don't enter less than 2 weeks"
                min={2}
                suffix=' weeks'
                value={rejectDurationSettings}
                pt={6}
                onChange={(value) =>
                  setRejectDurationSettings(typeof value === 'number' ? value : Number(value))
                }
              />
            </Stack>
          </Group>
        )}
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default AutomaticRejectionCard
