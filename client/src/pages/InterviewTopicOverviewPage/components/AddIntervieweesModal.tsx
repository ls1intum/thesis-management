import {
  Center,
  Input,
  Loader,
  Modal,
  Paper,
  Title,
  Text,
  useMantineColorScheme,
} from '@mantine/core'
import { useState } from 'react'
import { IApplicationInterviewProcess } from '../../../requests/responses/interview'
import SelectApplicantsList from '../../InterviewOverviewPage/components/SelectApplicantsList'

interface IAddIntervieweesModalProps {
  opened: boolean
  closeModal: () => void
}

const AddIntervieweesModal = ({ opened, closeModal }: IAddIntervieweesModalProps) => {
  const [applicantsLoading, setApplicantsLoading] = useState(false)
  const [possibleInterviewApplicants, setPossibleInterviewApplicants] = useState<
    IApplicationInterviewProcess[]
  >([])
  const [selectedApplicants, setSelectedApplicants] = useState<string[]>([])
  const { colorScheme } = useMantineColorScheme()

  //TODO: Fetch applicants for the interview topic when opening the modal -> new Endpoint (fetch it with the interview process id)

  return (
    <Modal
      opened={opened}
      onClose={closeModal}
      centered
      size='xl'
      title={<Title order={3}>Add Interviewees to Interview Process</Title>}
    >
      <Input.Wrapper
        label='Select Applicants (optional)'
        description='Select applicants for this interview process. You can also add them later or while reviewing applications.'
      >
        {applicantsLoading ? (
          <Center h={'10vh'} w={'100%'}>
            <Loader />
          </Center>
        ) : possibleInterviewApplicants.length === 0 ? (
          <Paper bg={colorScheme === 'dark' ? 'dark.8' : 'gray.0'} h={'50px'}>
            <Center>
              <Text c='dimmed' m={'xs'}>
                No applicants found for the selected topic.
              </Text>
            </Center>
          </Paper>
        ) : (
          <SelectApplicantsList
            possibleInterviewApplicants={possibleInterviewApplicants}
            selectedApplicants={selectedApplicants}
            setSelectedApplicants={setSelectedApplicants}
          />
        )}
      </Input.Wrapper>
    </Modal>
  )
}

export default AddIntervieweesModal
