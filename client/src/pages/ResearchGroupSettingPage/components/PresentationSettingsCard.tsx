import { Divider, Group, NumberInput, Stack, Switch, Text } from '@mantine/core'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { useEffect } from 'react'
import { useDebouncedValue } from '@mantine/hooks'
import { doRequest } from '../../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { IResearchGroupSettings } from '../../../requests/responses/researchGroupSettings'

interface PresentaionSettingsProps {
  presentationDurationSettings: number
  setPresentationDurationSettings: (value: number) => void
}

const PresentationSettingsCard = ({
  presentationDurationSettings,
  setPresentationDurationSettings,
}: PresentaionSettingsProps) => {
  const [debouncedPresentationDuration] = useDebouncedValue(presentationDurationSettings, 300)

  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  useEffect(() => {
    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}/automatic-reject`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          presentationSettings: {
            presentationSlotDuration: debouncedPresentationDuration,
          },
        },
      },
      (res) => {
        if (res.ok) {
          if (
            res.data.presentationSettings.presentationSlotDuration !== presentationDurationSettings
          ) {
            setPresentationDurationSettings(res.data.presentationSettings.presentationSlotDuration)
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [debouncedPresentationDuration])

  return (
    <ResearchGroupSettingsCard
      title='Presentation Settings'
      subtle='Configure default presentation settings.'
    >
      <Stack>
        <Group wrap='nowrap' w={'100%'}>
          <Stack gap={2} w={'100%'}>
            <Text size='sm' fw={500}>
              Presentation Slot Duration
            </Text>
            <Text size='xs' c='dimmed'>
              Default duration for presentation when creating a new presentation.
            </Text>
            <NumberInput
              placeholder="Don't enter less than 2 minutes"
              min={2}
              suffix=' minutes'
              value={presentationDurationSettings}
              pt={6}
              onChange={(value) =>
                setPresentationDurationSettings(typeof value === 'number' ? value : Number(value))
              }
              w={'100%'}
            />
          </Stack>
        </Group>
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default PresentationSettingsCard
