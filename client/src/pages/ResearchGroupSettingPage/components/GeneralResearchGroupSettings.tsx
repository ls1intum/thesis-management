import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { doRequest } from '../../../requests/request'
import { showNotification } from '@mantine/notifications'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { IResearchGroup } from '../../../requests/responses/researchGroup'
import { ResearchGroupFormValues } from '../../ResearchGroupAdminPage/components/CreateResearchGroupModal'
import ResearchGroupForm from '../../../components/ResearchGroupForm/ResearchGroupForm'

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
