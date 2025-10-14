import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { IResearchGroup } from '../../requests/responses/researchGroup'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { Center, Loader, Stack, Tabs, Title } from '@mantine/core'
import GeneralResearchGroupSettings from './components/GeneralResearchGroupSettings'
import ResearchGroupMembers from './components/MemberSettings/ResearchGroupMembers'
import EmailTemplatesOverview from './components/EmailTemplateSettings/EmailTemplatesOverview'
import { useUser } from '../../hooks/authentication'
import AutomaticRejectionCard from './components/AutomaticRejectionCard'
import { IResearchGroupSettings } from '../../requests/responses/researchGroupSettings'

const ResearchGroupSettingPage = () => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const user = useUser()

  const [loading, setLoading] = useState(true)
  const [researchGroupData, setResearchGroupData] = useState<IResearchGroup | undefined>(undefined)
  const [researchGroupSettings, setResearchGroupSettings] = useState<
    IResearchGroupSettings | undefined
  >(undefined)

  const [searchParams, setSearchParams] = useSearchParams()
  const [selectedTab, setSelectedTab] = useState(searchParams.get('setting') ?? 'general')

  const [researchGroupSettingsLoading, setResearchGroupSettingsLoading] = useState(true)

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

    setResearchGroupSettingsLoading(true)

    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}`,
      { method: 'GET', requiresAuth: true },
      (res) => {
        if (res.ok) {
          if (res.ok) {
            setResearchGroupSettings(res.data)
          }
        } else if (res.status === 404) {
          setResearchGroupData(undefined)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setResearchGroupSettingsLoading(false)
      },
    )
  }, [researchGroupId])

  if (loading) return <Loader />

  return (
    <>
      {user &&
      (user.groups.includes('admin') ? false : user.researchGroupId !== researchGroupId) ? (
        <Center h={'100%'}>
          <h1>403 - Unauthorized</h1>
        </Center>
      ) : (
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
              <Tabs.Tab value='email-templates'>Email Templates</Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value='general' pt='md'>
              <Stack>
                <GeneralResearchGroupSettings
                  researchGroupData={researchGroupData}
                  setResearchGroupData={setResearchGroupData}
                />
                {!researchGroupSettingsLoading && (
                  <AutomaticRejectionCard
                    automaticRejectionEnabledSettings={
                      researchGroupSettings?.automaticRejectEnabled || false
                    }
                    rejectDurationSettings={researchGroupSettings?.rejectDuration || 8}
                    setAutomaticRejectionEnabledSettings={(value: boolean) =>
                      setResearchGroupSettings(
                        (prev) =>
                          ({
                            ...prev,
                            automaticRejectEnabled: value,
                          }) as IResearchGroupSettings,
                      )
                    }
                    setRejectDurationSettings={(value: number) =>
                      setResearchGroupSettings(
                        (prev) =>
                          ({
                            ...prev,
                            rejectDuration: value,
                          }) as IResearchGroupSettings,
                      )
                    }
                  />
                )}
              </Stack>
            </Tabs.Panel>
            <Tabs.Panel value='members' pt='md'>
              <ResearchGroupMembers researchGroupData={researchGroupData} />
            </Tabs.Panel>
            <Tabs.Panel value='email-templates' pt='md'>
              <EmailTemplatesOverview />
            </Tabs.Panel>
          </Tabs>
        </Stack>
      )}
    </>
  )
}

export default ResearchGroupSettingPage
