import {
  Button,
  Center,
  Group,
  Input,
  Loader,
  Modal,
  Paper,
  ScrollArea,
  Stack,
  Title,
  Text,
} from '@mantine/core'
import {
  IIntervieweeLightWithNextSlot,
  IInterviewSlot,
} from '../../../requests/responses/interview'
import SlotItem from './SlotItem'
import { useEffect, useState } from 'react'
import { doRequest } from '../../../requests/request'
import { PaginationResponse } from '../../../requests/responses/pagination'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { useParams } from 'react-router'
import AvatarUser from '../../../components/AvatarUser/AvatarUser'

interface IAssignIntervieweeToSlotModalProps {
  slot: IInterviewSlot
  assignModalOpen: boolean
  setAssignModalOpen: (open: boolean) => void
}

const AssignIntervieweeToSlotModal = ({
  slot,
  assignModalOpen,
  setAssignModalOpen,
}: IAssignIntervieweeToSlotModalProps) => {
  const { processId } = useParams<{ processId: string }>()
  const [interviewees, setInterviewees] = useState<IIntervieweeLightWithNextSlot[]>([])
  const [intervieweesLoading, setIntervieweesLoading] = useState(false)

  const fetchPossibleInterviewees = async () => {
    setIntervieweesLoading(true)
    doRequest<PaginationResponse<IIntervieweeLightWithNextSlot>>(
      `/v2/interview-process/${processId}/interviewees`,
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          limit: 100,
        },
      },
      (res) => {
        if (res.ok) {
          setInterviewees(res.data.content)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setIntervieweesLoading(false)
      },
    )

    {
      /*TODO: Reuse the Interviewees already fetched, instead of fetching them again*/
    }
  }

  const [selectedInterviewee, setSelectedInterviewee] =
    useState<IIntervieweeLightWithNextSlot | null>(null)

  //TODO: Diplicate Code from InterviewBookingPage - refactor to custom hook
  const [bookingLoading, setBookingLoading] = useState(false)
  const bookSlot = async (slotId: string) => {
    setBookingLoading(true)
    doRequest<IInterviewSlot>(
      `/v2/interview-process/${processId}/slot/${slotId}/book`,
      {
        method: 'PUT',
        requiresAuth: true,
        data: {
          intervieweeUserId: selectedInterviewee?.user.userId,
        },
      },
      (res) => {
        setBookingLoading(false)
        if (res.ok) {
          setAssignModalOpen(false)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  useEffect(() => {
    if (assignModalOpen) {
      fetchPossibleInterviewees()
    }
  }, [assignModalOpen])

  return (
    <Modal
      opened={assignModalOpen}
      onClose={() => setAssignModalOpen(false)}
      title={<Title order={3}>Confirm Invitation</Title>}
      size={'xl'}
      centered
    >
      <Stack>
        <Input.Wrapper label='Selected Slot'>
          <SlotItem slot={slot} hoverEffect={false} withDate={true} />
        </Input.Wrapper>
        <Input.Wrapper label='Select Interviewee' withAsterisk>
          {intervieweesLoading ? (
            <Center>
              <Loader />
            </Center>
          ) : (
            <ScrollArea.Autosize mih={'50px'} mah={'30vh'} w={'100%'} type='hover' bdrs={'md'}>
              <Stack gap='0.5rem'>
                {interviewees.map((interviewee) => {
                  const intervieweeDisabled = interviewee.nextSlot !== null
                  return (
                    <Paper
                      withBorder
                      key={interviewee.intervieweeId}
                      onClick={() => {
                        if (!intervieweeDisabled) {
                          setSelectedInterviewee(interviewee)
                        }
                      }}
                      bg={
                        intervieweeDisabled
                          ? 'gray.0'
                          : interviewee === selectedInterviewee
                            ? 'primary.1'
                            : undefined
                      }
                      p={'xs'}
                      style={{ cursor: intervieweeDisabled ? 'not-allowed' : 'pointer' }}
                    >
                      <Group justify='space-between' align='center'>
                        <AvatarUser
                          user={interviewee.user}
                          textColor={intervieweeDisabled ? 'dimmed' : undefined}
                        ></AvatarUser>
                        {intervieweeDisabled && (
                          <Text c='dimmed' size='xs'>
                            Already booked a slot
                          </Text>
                        )}
                      </Group>
                    </Paper>
                  )
                })}
              </Stack>
            </ScrollArea.Autosize>
          )}
        </Input.Wrapper>
        <Group justify='end' align='center'>
          <Button
            variant='default'
            onClick={() => {
              setAssignModalOpen(false)
            }}
          >
            Cancel
          </Button>
          <Button
            onClick={() => bookSlot(slot.slotId)}
            loading={bookingLoading}
            disabled={selectedInterviewee === null}
          >
            Book Slot
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export default AssignIntervieweeToSlotModal
