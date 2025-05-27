import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { IResearchGroup } from '../../requests/responses/researchGroup'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { Loader, Stack, Title } from '@mantine/core'
import GeneralResearchGroupSettings from './components/GeneralResearchGroupSettings'
import ResearchGroupMembers from './components/ResearchGroupMembers'

const ResearchGroupSettingPage = () => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const [loading, setLoading] = useState(true)
  const [researchGroupData, setResearchGroupData] = useState<IResearchGroup | undefined>(undefined)

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

      <GeneralResearchGroupSettings
        researchGroupData={researchGroupData}
        setResearchGroupData={setResearchGroupData}
      />
      <ResearchGroupMembers researchGroupData={researchGroupData} />
    </Stack>
  )
}

export default ResearchGroupSettingPage
