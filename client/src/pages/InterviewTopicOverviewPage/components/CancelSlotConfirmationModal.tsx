import { Modal, Stack, Title, Text, Paper, Group, Button, Input } from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import { IInterviewSlot } from '../../../requests/responses/interview'
import { useInterviewProcessContext } from '../../../providers/InterviewProcessProvider/hooks'
import SlotItem from './SlotItem'
import AvatarUser from '../../../components/AvatarUser/AvatarUser'

interface ICancelSlotConfirmationModalProps {
  cancelModalOpen: boolean
  setCancelModalOpen: (open: boolean) => void

  slot?: IInterviewSlot
}

const CancelSlotConfirmationModal = ({
  cancelModalOpen,
  setCancelModalOpen,
  slot,
}: ICancelSlotConfirmationModalProps) => {
  const isSmaller = useIsSmallerBreakpoint('sm')

  const slotLabel =
    slot?.startDate && slot?.endDate
      ? `${new Date(slot.startDate).toLocaleString()} – ${new Date(slot.endDate).toLocaleTimeString()}`
      : undefined

  const { bookingLoading, cancelSlot } = useInterviewProcessContext()

  return (
    <Modal
      opened={cancelModalOpen}
      onClose={() => setCancelModalOpen(false)}
      title={<Title order={3}>Confirm Cancellation</Title>}
      size={isSmaller ? 'md' : 'xl'}
      centered
    >
      <Stack>
        <Text>Are you sure you want to cancel this interview slot booking?</Text>

        {slot?.bookedBy && (
          <Input.Wrapper label='Interviewee'>
            <Paper withBorder p='xs'>
              <AvatarUser user={slot?.bookedBy?.user} />
            </Paper>
          </Input.Wrapper>
        )}

        {slot && (
          <Input.Wrapper label='Booked Slot'>
            <SlotItem slot={slot} hoverEffect={false} withDate={true} />
          </Input.Wrapper>
        )}

        <Group justify='end' align='center'>
          <Button
            variant='default'
            onClick={() => {
              setCancelModalOpen(false)
            }}
          >
            Close
          </Button>

          <Button
            color='red'
            loading={bookingLoading}
            disabled={!slot || !cancelSlot}
            onClick={() => {
              if (!slot || !cancelSlot) return
              cancelSlot(slot.slotId, () => {
                setCancelModalOpen(false)
              })
            }}
          >
            Cancel Interview
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export default CancelSlotConfirmationModal
