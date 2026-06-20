import { ResearchGroupSettingsCard } from '@/core/group/pages/ResearchGroupSettingPage/components/ResearchGroupSettingsCard'
import { doRequest } from '@/core/requests/request'
import { showNotification } from '@mantine/notifications'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import type { IResearchGroup } from '@/core/group/requests/responses/researchGroup'
import type { ResearchGroupFormValues } from '@/core/group/pages/ResearchGroupAdminPage/components/CreateResearchGroupModal'
import ResearchGroupForm from '@/core/group/components/ResearchGroupForm/ResearchGroupForm'

interface IGeneralResearchGroupSettingsProps {
  researchGroupData: IResearchGroup | undefined
  setResearchGroupData: (data: IResearchGroup) => void
}

const GeneralResearchGroupSettings = ({
  researchGroupData,
  setResearchGroupData,
}: IGeneralResearchGroupSettingsProps) => {
  const handleSubmit = (values: ResearchGroupFormValues) => {
    if (!researchGroupData?.id) return

    doRequest<IResearchGroup>(
      `/v2/research-groups/${researchGroupData.id}`,
      {
        method: 'PUT',
        requiresAuth: true,
        data: {
          headUsername: values.headUsername,
          name: values.name,
          abbreviation: values.abbreviation,
          campus: values.campus,
          description: values.description,
          websiteUrl: values.websiteUrl,
        },
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: 'Research group updated successfully.',
            color: 'green',
          })
          setResearchGroupData(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <ResearchGroupSettingsCard
      title='Group Information'
      subtle='Edit the basic information about your research group.'
    >
      <ResearchGroupForm
        initialResearchGroup={{
          name: researchGroupData?.name,
          abbreviation: researchGroupData?.abbreviation,
          campus: researchGroupData?.campus,
          description: researchGroupData?.description,
          websiteUrl: researchGroupData?.websiteUrl,
          head: researchGroupData?.head,
        }}
        onSubmit={handleSubmit}
        submitLabel='Save Changes'
        layout='grid'
      />
    </ResearchGroupSettingsCard>
  )
}

export default GeneralResearchGroupSettings
