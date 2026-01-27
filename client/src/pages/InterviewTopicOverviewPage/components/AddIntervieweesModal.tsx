import {
  Center,
  Input,
  Loader,
  Modal,
  Paper,
  Title,
  Text,
  useMantineColorScheme,
  Button,
  Stack,
} from '@mantine/core'
import { useEffect, useState } from 'react'
import { IApplicationInterviewProcess } from '../../../requests/responses/interview'
import SelectApplicantsList from '../../InterviewOverviewPage/components/SelectApplicantsList'
import { doRequest } from '../../../requests/request'
import { PaginationResponse } from '../../../requests/responses/pagination'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { useParams } from 'react-router'
import { useInterviewProcessContext } from '../../../providers/InterviewProcessProvider/hooks'

interface IAddIntervieweesModalProps {
  opened: boolean
  closeModal: () => void
}

const AddIntervieweesModal = ({ opened, closeModal }: IAddIntervieweesModalProps) => {
  const { processId } = useParams<{ processId: string }>()

  const [applicantsLoading, setApplicantsLoading] = useState(false)
  const [possibleInterviewApplicants, setPossibleInterviewApplicants] = useState<
    IApplicationInterviewProcess[]
  >([])
  const [selectedApplicants, setSelectedApplicants] = useState<string[]>([])
  const { colorScheme } = useMantineColorScheme()

  const { addIntervieweesToProcess } = useInterviewProcessContext()

  const fetchPossibleInterviewApplicantsByTopic = async () => {
    setApplicantsLoading(true)
    doRequest<PaginationResponse<IApplicationInterviewProcess>>(
      `/v2/interview-process/${processId}/interview-applications`,
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          limit: -1,
        },
      },
      (res) => {
        if (res.ok) {
          setPossibleInterviewApplicants(res.data.content)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setApplicantsLoading(false)
      },
    )
  }

  useEffect(() => {
    if (opened) {
      fetchPossibleInterviewApplicantsByTopic()
    }
  }, [opened])

  return (
    <Modal
      opened={opened}
      onClose={closeModal}
      centered
      size='xl'
      title={<Title order={3}>Add Interviewees to Interview Process</Title>}
    >
      <Stack>
        <Input.Wrapper label='Select Applicants'>
          {applicantsLoading ? (
            <Center h={'10vh'} w={'100%'}>
              <Loader />
            </Center>
          ) : possibleInterviewApplicants.length === 0 ? (
            <Paper bg={colorScheme === 'dark' ? 'dark.8' : 'gray.0'} h={'50px'}>
              <Center>
                <Text c='dimmed' m={'xs'}>
                  No more applicants found for this interview topic.
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
        <Button
          onClick={() => {
            addIntervieweesToProcess(selectedApplicants)
            closeModal()
          }}
          disabled={selectedApplicants.length === 0}
        >
          Add Interviewees
        </Button>
      </Stack>
    </Modal>
  )
}

export default AddIntervieweesModal
