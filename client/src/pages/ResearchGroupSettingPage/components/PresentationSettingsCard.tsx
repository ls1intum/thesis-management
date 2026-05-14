import { useEffect, useRef, useState } from 'react'
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

// Mantine's NumberInput emits `''` when the field is cleared. We hold the
// raw value (number or '') in local state so the input visually stays empty
// during editing instead of snapping to "0 minutes" via Number('') === 0.
type DurationInputValue = number | ''

const PresentationSettingsCard = ({
  presentationDurationSettings,
  setPresentationDurationSettings,
}: PresentaionSettingsProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  // Local state for the input so intermediate keystrokes don't each trigger a
  // POST. Without this, Mantine's NumberInput fires onChange for every parse
  // event (e.g. '', 4, 45 when filling "45"), causing overlapping saves that
  // race on the server and can land in any order.
  const [localDuration, setLocalDuration] = useState<DurationInputValue>(
    presentationDurationSettings,
  )
  const [debouncedDuration] = useDebouncedValue(localDuration, 500)

  // Track whether a save is currently in flight so the prop-sync effect below
  // doesn't clobber a user keystroke that lands while the server is still
  // processing the previous save.
  const savePendingRef = useRef(false)

  // Re-sync local state when the prop changes (initial load, navigation,
  // successful save response). Skip while a save is in flight — otherwise
  // the prop update from a slightly-stale save response would overwrite
  // characters the user has typed since.
  useEffect(() => {
    if (savePendingRef.current) return
    setLocalDuration(presentationDurationSettings)
  }, [presentationDurationSettings])

  useEffect(() => {
    if (typeof debouncedDuration !== 'number') return
    if (debouncedDuration === presentationDurationSettings) return
    if (!Number.isFinite(debouncedDuration) || debouncedDuration < MIN_DURATION_MINUTES) return

    savePendingRef.current = true
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
        savePendingRef.current = false
        if (res.ok) {
          setPresentationDurationSettings(res.data.presentationSettings.presentationSlotDuration)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
    // `researchGroupId` is stable for the lifetime of this card (route param;
    // a different group would unmount/remount this tree) and
    // `setPresentationDurationSettings` is a stable setter from the parent —
    // both are intentionally omitted from the dependency list.
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- see comment above
  }, [debouncedDuration, presentationDurationSettings])

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
                if (value === '' || value === null || value === undefined) {
                  setLocalDuration('')
                  return
                }
                const parsed = typeof value === 'number' ? value : Number(value)
                setLocalDuration(Number.isFinite(parsed) ? parsed : '')
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
