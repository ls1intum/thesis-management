import { useEffect, useState } from 'react'
import { Group, NumberInput, Stack, Text } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { doRequest } from '../../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import type { IResearchGroupSettings } from '../../../requests/responses/researchGroupSettings'

interface PresentaionSettingsProps {
  presentationDurationSettings: number
  setPresentationDurationSettings: (value: number) => void
}

const MIN_DURATION_MINUTES = 2

const PresentationSettingsCard = ({
  presentationDurationSettings,
  setPresentationDurationSettings,
}: PresentaionSettingsProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  // Local state for the input so intermediate keystrokes don't each trigger a
  // POST. Without this, Mantine's NumberInput fires onChange for every parse
  // event (e.g. '', 4, 45 when filling "45"), causing overlapping saves that
  // race on the server and can land in any order.
  const [localDuration, setLocalDuration] = useState(presentationDurationSettings)
  const [debouncedDuration] = useDebouncedValue(localDuration, 500)

  // Re-sync local state when the prop changes (initial load, save response,
  // navigation). Skip while the user has unsaved edits in flight.
  useEffect(() => {
    setLocalDuration(presentationDurationSettings)
  }, [presentationDurationSettings])

  useEffect(() => {
    if (debouncedDuration === presentationDurationSettings) return
    if (!Number.isFinite(debouncedDuration) || debouncedDuration < MIN_DURATION_MINUTES) return

    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          presentationSettings: {
            presentationSlotDuration: debouncedDuration,
          },
        },
      },
      (res) => {
        if (res.ok) {
          setPresentationDurationSettings(res.data.presentationSettings.presentationSlotDuration)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- debounced auto-save; researchGroupId and setter are stable for the lifetime of this card
  }, [debouncedDuration])

  return (
    <ResearchGroupSettingsCard
      title='Presentation Settings'
      subtle='Configure settings related to presentations of this research group.'
    >
      <Stack>
        <Group wrap='nowrap' w={'100%'}>
          <Stack gap={2} w={'100%'}>
            <Text size='sm' fw={500}>
              Presentation Slot Duration
            </Text>
            <Text size='xs' c='dimmed'>
              Default duration for a presentation slot of this research group.
            </Text>
            <NumberInput
              placeholder="Don't enter less than 2 minutes"
              min={MIN_DURATION_MINUTES}
              suffix=' minutes'
              value={localDuration}
              pt={6}
              onChange={(value) => {
                setLocalDuration(typeof value === 'number' ? value : Number(value))
              }}
              w={'100%'}
            />
          </Stack>
        </Group>
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default PresentationSettingsCard
