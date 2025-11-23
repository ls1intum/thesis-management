import { Group, Stack, Switch, Text } from '@mantine/core'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { useEffect } from 'react'
import { doRequest } from '../../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { IResearchGroupSettings } from '../../../requests/responses/researchGroupSettings'

interface PhaseSettingsProps {
  proposalPhaseActive: boolean
  setProposalPhaseActive: (value: boolean) => void
}

const PhaseSettingsCard = ({ proposalPhaseActive, setProposalPhaseActive }: PhaseSettingsProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const handleProposalPhaseChange = (value: boolean) => {
    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          phaseSettings: {
            proposalPhaseActive: value,
          },
        },
      },
      (res) => {
        if (res.ok) {
          if (res.data.phaseSettings.proposalPhaseActive !== proposalPhaseActive) {
            setProposalPhaseActive(res.data.phaseSettings.proposalPhaseActive)
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <ResearchGroupSettingsCard
      title='Thesis Phase Settings'
      subtle='Turn off thesis phases for this research group that are not used in this group.'
    >
      <Stack>
        <Group justify='space-between' align='center' wrap='nowrap'>
          <Stack gap={2}>
            <Text size='sm' fw={500}>
              Enable Proposal Phase
            </Text>
            <Text size='xs' c='dimmed'>
              Turn off the proposal phase for this research group if proposals are not used. This
              will only affect new theses that are created or accepted after changing this setting.
            </Text>
          </Stack>
          <Switch
            checked={proposalPhaseActive}
            onChange={(event) => {
              setProposalPhaseActive(event.currentTarget.checked)
              handleProposalPhaseChange(event.currentTarget.checked)
            }}
          />
        </Group>
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default PhaseSettingsCard
