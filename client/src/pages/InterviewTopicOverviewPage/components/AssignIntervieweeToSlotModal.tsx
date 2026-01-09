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
import AvatarUser from '../../../components/AvatarUser/AvatarUser'
import { useInterviewProcessContext } from '../../../providers/InterviewProcessProvider/hooks'

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
  const { bookSlot, bookingLoading, interviewees, intervieweesLoading, fetchPossibleInterviewees } =
    useInterviewProcessContext()

  const [selectedInterviewee, setSelectedInterviewee] =
    useState<IIntervieweeLightWithNextSlot | null>(null)

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
                          if (interviewee === selectedInterviewee) {
                            setSelectedInterviewee(null)
                          } else {
                            setSelectedInterviewee(interviewee)
                          }
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
            onClick={() => {
              if (selectedInterviewee) {
                bookSlot(slot.slotId, selectedInterviewee.user.userId)
              }
            }}
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
