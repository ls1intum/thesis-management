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

const PresentationOverviewPage = () => {
  usePageTitle('Presentations')

  const [researchGroups, setResearchGroups] = useState<ILightResearchGroup[]>([])
  const [selectedGroup, setSelectedGroup] = useState<string | null>(null)

  useEffect(() => {
    return doRequest<ILightResearchGroup[]>(
      `/v2/research-groups/light/active`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setResearchGroups(res.data)
          setSelectedGroup(res.data[0].id || null)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [])

  const calendarUrl =
    GLOBAL_CONFIG.calendar_url ||
    `${GLOBAL_CONFIG.server_host}/api/v2/calendar/presentations${selectedGroup ? `/${selectedGroup}` : ''}`

  return (
    <Stack>
      <Title>Presentations</Title>
      {researchGroups.length > 1 && (
        <Tabs value={selectedGroup} onChange={setSelectedGroup}>
          <Tabs.List>
            {researchGroups.map((group) => (
              <Tabs.Tab key={group.id} value={group.id}>
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
      <PublicPresentationsTable includeDrafts={true} researchGroupId={selectedGroup} />
    </Stack>
  )
}

export default PresentationOverviewPage
