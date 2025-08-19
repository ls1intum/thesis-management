import {
  ActionIcon,
  CopyButton,
  Group,
  Stack,
  TextInput,
  Title,
  Tooltip,
  Text,
  Tabs,
  Select,
} from '@mantine/core'
import { usePageTitle } from '../../hooks/theme'
import { GLOBAL_CONFIG } from '../../config/global'
import { Check, Copy } from 'phosphor-react'
import PublicPresentationsTable from '../../components/PublicPresentationsTable/PublicPresentationsTable'
import { useEffect, useState } from 'react'
import { doRequest } from '../../requests/request'
import { ILightResearchGroup } from '../../requests/responses/researchGroup'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { useAuthenticationContext } from '../../hooks/authentication'

const PresentationOverviewPage = () => {
  usePageTitle('Presentations')

  const [researchGroups, setResearchGroups] = useState<ILightResearchGroup[]>([])
  const [selectedGroup, setSelectedGroup] = useState<ILightResearchGroup | undefined>(undefined)

  const context = useAuthenticationContext()

  useEffect(() => {
    if (context.researchGroups.length > 0) {
      setResearchGroups(context.researchGroups)
      setSelectedGroup(context.researchGroups[0])
    }
  }, [context.researchGroups])

  const calendarUrl =
    GLOBAL_CONFIG.calendar_url ||
    `${GLOBAL_CONFIG.server_host}/api/v2/calendar/presentations${selectedGroup ? `/${selectedGroup.abbreviation}` : ''}`

  return (
    <Stack>
      <Title>Presentations</Title>
      {researchGroups.length > 1 && (
        <Tabs
          value={selectedGroup?.abbreviation}
          onChange={(value) => {
            const group = researchGroups.find((g) => g.abbreviation === value)
            setSelectedGroup(group)
          }}
        >
          <Tabs.List>
            {researchGroups.map((group) => (
              <Tabs.Tab key={group.id} value={group.abbreviation}>
                {group.name}
              </Tabs.Tab>
            ))}
          </Tabs.List>
        </Tabs>
      )}
      <Group>
        <Text c='dimmed'>Subscribe to Calendar</Text>
        <div style={{ flexGrow: 1 }}>
          <CopyButton value={calendarUrl}>
            {({ copied, copy }) => (
              <TextInput
                value={calendarUrl}
                onChange={() => undefined}
                onClick={(e) => e.currentTarget.select()}
                rightSection={
                  <Tooltip label={copied ? 'Copied' : 'Copy'} withArrow position='right'>
                    <ActionIcon color={copied ? 'teal' : 'gray'} variant='subtle' onClick={copy}>
                      {copied ? <Check size={16} /> : <Copy size={16} />}
                    </ActionIcon>
                  </Tooltip>
                }
              />
            )}
          </CopyButton>
        </div>
      </Group>
      <PublicPresentationsTable includeDrafts={true} researchGroupId={selectedGroup?.id} />
    </Stack>
  )
}

export default PresentationOverviewPage
