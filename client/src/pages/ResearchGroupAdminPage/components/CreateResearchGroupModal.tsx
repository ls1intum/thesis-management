import { Modal } from '@mantine/core'
import ResearchGroupForm from '../../../components/ResearchGroupForm/ResearchGroupForm'

interface ICreateResearchGroupModalProps {
  opened: boolean
  onClose: () => void
  onSubmit: (values: ResearchGroupFormValues) => void
}

export interface ResearchGroupFormValues {
  name: string
  abbreviation: string
  campus: string
  description: string
  websiteUrl: string
  headUsername: string
}

const CreateResearchGroupModal = ({
  opened,
  onClose,
  onSubmit,
}: ICreateResearchGroupModalProps) => {
  return (
    <Modal opened={opened} onClose={onClose} title='Create Research Group' centered>
      <ResearchGroupForm
        onSubmit={(values) => onSubmit(values)}
        submitLabel='Create Research Group'
      />
    </Modal>
  )
}

export default CreateResearchGroupModal
