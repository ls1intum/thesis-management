import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'
import type { IResearchGroup } from '@/core/group/requests/responses/researchGroup'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import { Center, Loader, Stack, Tabs, Title } from '@mantine/core'
import GeneralResearchGroupSettings from '@/core/group/pages/ResearchGroupSettingPage/components/GeneralResearchGroupSettings'
import ResearchGroupMembers from '@/core/group/pages/ResearchGroupSettingPage/components/MemberSettings/ResearchGroupMembers'
import EmailTemplatesOverview from '@/core/group/pages/ResearchGroupSettingPage/components/EmailTemplateSettings/EmailTemplatesOverview'
import { useUser } from '@/core/hooks/authentication'
import ApplicationPhaseSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/ApplicationPhaseSettingsCard'
import type { IResearchGroupSettings } from '@/core/group/requests/responses/researchGroupSettings'
import PresentationSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/PresentationSettingsCard'
import ProposalSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/ProposalSettingsCard'
import EmailSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/EmailSettingsCard'
import ScientificWritingGuideSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/ScientificWritingGuideSettingsCard'
import AIReviewGuidelinesSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/AIReviewGuidelinesSettingsCard'
import ApplicationEmailContentSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/ApplicationEmailContentSettingsCard'
import GradingSchemeSettingsCard from '@/core/group/pages/ResearchGroupSettingPage/components/GradingSchemeSettingsCard'

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

    if (selectedTab !== 'general') {
      params.set('setting', selectedTab)
    } else {
      params.delete('setting')
    }

    setSearchParams(params, { replace: true })
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- searchParams/setSearchParams change on every navigation; only sync URL when the selected tab changes
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
          setResearchGroupData(res.data)
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
          setResearchGroupSettings(res.data)
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
      (user.groups?.includes('admin') ? false : user.researchGroupId !== researchGroupId) ? (
        <Center h={'100%'}>
          <h1>403 - Unauthorized</h1>
        </Center>
      ) : (
        <Stack>
          <Title>Research Group Settings</Title>

          <Tabs
            value={selectedTab}
            onChange={(value) => {
              setSelectedTab(value ?? 'general')
            }}
          >
            <Tabs.List>
              <Tabs.Tab value='general'>General</Tabs.Tab>
              <Tabs.Tab value='members'>Members</Tabs.Tab>
              <Tabs.Tab value='email-templates'>Email Settings</Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value='general' pt='md'>
              <Stack>
                <GeneralResearchGroupSettings
                  researchGroupData={researchGroupData}
                  setResearchGroupData={setResearchGroupData}
                />
                <ScientificWritingGuideSettingsCard
                  writingGuideSettings={researchGroupSettings?.writingGuideSettings}
                  setWritingGuideSettings={(writingGuideSettings) =>
                    setResearchGroupSettings(
                      (prev) =>
                        ({
                          ...prev,
                          writingGuideSettings: writingGuideSettings,
                        }) as IResearchGroupSettings,
                    )
                  }
                />
                <AIReviewGuidelinesSettingsCard />
                {!researchGroupSettingsLoading && (
                  <>
                    <ApplicationPhaseSettingsCard
                      automaticRejectionEnabledSettings={
                        researchGroupSettings?.rejectSettings.automaticRejectEnabled ?? false
                      }
                      rejectDurationSettings={
                        researchGroupSettings?.rejectSettings.rejectDuration ?? 8
                      }
                      setAutomaticRejectionEnabledSettings={(value: boolean) =>
                        setResearchGroupSettings(
                          (prev) =>
                            ({
                              ...prev,
                              rejectSettings: {
                                ...prev?.rejectSettings,
                                automaticRejectEnabled: value,
                              },
                            }) as IResearchGroupSettings,
                        )
                      }
                      setRejectDurationSettings={(value: number) =>
                        setResearchGroupSettings(
                          (prev) =>
                            ({
                              ...prev,
                              rejectSettings: {
                                ...prev?.rejectSettings,
                                rejectDuration: value,
                              },
                            }) as IResearchGroupSettings,
                        )
                      }
                    />
                    <ProposalSettingsCard
                      proposalPhaseActive={
                        researchGroupSettings?.phaseSettings.proposalPhaseActive ?? false
                      }
                      setProposalPhaseActive={(value: boolean) =>
                        setResearchGroupSettings(
                          (prev) =>
                            ({
                              ...prev,
                              phaseSettings: {
                                ...prev?.phaseSettings,
                                proposalPhaseActive: value,
                              },
                            }) as IResearchGroupSettings,
                        )
                      }
                    />
                    <PresentationSettingsCard
                      presentationDurationSettings={
                        researchGroupSettings?.presentationSettings.presentationSlotDuration ?? 30
                      }
                      setPresentationDurationSettings={(value: number) =>
                        setResearchGroupSettings(
                          (prev) =>
                            ({
                              ...prev,
                              presentationSettings: {
                                ...prev?.presentationSettings,
                                presentationSlotDuration: value,
                              },
                            }) as IResearchGroupSettings,
                        )
                      }
                    />
                    <GradingSchemeSettingsCard
                      gradingSchemeSettings={researchGroupSettings?.gradingSchemeSettings}
                      setGradingSchemeSettings={(gradingSchemeSettings) =>
                        setResearchGroupSettings(
                          (prev) =>
                            ({
                              ...prev,
                              gradingSchemeSettings,
                            }) as IResearchGroupSettings,
                        )
                      }
                    />
                  </>
                )}
              </Stack>
            </Tabs.Panel>
            <Tabs.Panel value='members' pt='md'>
              <ResearchGroupMembers researchGroupData={researchGroupData} />
            </Tabs.Panel>
            <Tabs.Panel value='email-templates' pt='md'>
              {researchGroupSettingsLoading ? (
                <Loader />
              ) : (
                <Stack>
                  <EmailSettingsCard
                    researchgroupEmailSettings={researchGroupSettings?.emailSettings}
                    setResearchgroupEmailSettings={(emailSettings) =>
                      setResearchGroupSettings(
                        (prev) =>
                          ({
                            ...prev,
                            emailSettings: emailSettings,
                          }) as IResearchGroupSettings,
                      )
                    }
                  />
                  <ApplicationEmailContentSettingsCard
                    includeApplicationDataInEmail={
                      researchGroupSettings?.applicationEmailSettings
                        ?.includeApplicationDataInEmail ?? false
                    }
                    setIncludeApplicationDataInEmail={(value: boolean) =>
                      setResearchGroupSettings(
                        (prev) =>
                          ({
                            ...prev,
                            applicationEmailSettings: {
                              ...prev?.applicationEmailSettings,
                              includeApplicationDataInEmail: value,
                            },
                          }) as IResearchGroupSettings,
                      )
                    }
                  />
                  <EmailTemplatesOverview
                    includeApplicationDataInEmail={
                      researchGroupSettings?.applicationEmailSettings
                        ?.includeApplicationDataInEmail ?? false
                    }
                  />
                </Stack>
              )}
            </Tabs.Panel>
          </Tabs>
        </Stack>
      )}
    </>
  )
}

export default ResearchGroupSettingPage
