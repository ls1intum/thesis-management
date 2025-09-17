import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { IResearchGroup } from '../../requests/responses/researchGroup'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { Loader, Stack, Tabs, Title } from '@mantine/core'
import GeneralResearchGroupSettings from './components/GeneralResearchGroupSettings'
import ResearchGroupMembers from './components/ResearchGroupMembers'
import { useUser } from '../../hooks/authentication'
import AutomaticRejectionCard from './components/AutomaticRejectionCard'

const ResearchGroupSettingPage = () => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const [loading, setLoading] = useState(true)
  const [researchGroupData, setResearchGroupData] = useState<IResearchGroup | undefined>(undefined)

  const user = useUser()

  const [searchParams, setSearchParams] = useSearchParams()
  const [selectedTab, setSelectedTab] = useState(searchParams.get('setting') ?? 'general')

  useEffect(() => {
    const params = new URLSearchParams(searchParams)

    if (selectedTab != 'general') {
      params.set('setting', selectedTab)
    } else {
      params.delete('setting')
    }

    setSearchParams(params, { replace: true })
  }, [selectedTab])

  useEffect(() => {
    if (!researchGroupId) return

    setLoading(true)

    doRequest<IResearchGroup>(
      `/v2/research-groups/${researchGroupId}`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          if (res.ok) {
            setResearchGroupData(res.data)
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setLoading(false)
      },
    )
  }, [researchGroupId])

  if (loading) return <Loader />

  return (
    <Stack>
      <Title>Research Group Settings</Title>

      <Tabs
        value={selectedTab}
        onChange={(value) => {
          setSelectedTab(value || 'general')
        }}
      >
        <Tabs.List>
          <Tabs.Tab value='general'>General</Tabs.Tab>
          <Tabs.Tab value='members'>Members</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value='general' pt='md'>
          <Stack>
            <GeneralResearchGroupSettings
              researchGroupData={researchGroupData}
              setResearchGroupData={setResearchGroupData}
            />
            <AutomaticRejectionCard />
          </Stack>
        </Tabs.Panel>
        <Tabs.Panel value='members' pt='md'>
          <ResearchGroupMembers researchGroupData={researchGroupData} />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  )
}

export default ResearchGroupSettingPage
