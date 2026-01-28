import { Alert, Button, Group, Modal, Paper, ScrollArea, Stack, Text, Title } from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import AvatarUser from '../../../components/AvatarUser/AvatarUser'
import { ILightUser } from '../../../requests/responses/user'
import { useInterviewProcessContext } from '../../../providers/InterviewProcessProvider/hooks'
import { WarningCircleIcon } from '@phosphor-icons/react'

interface IinviteConfirmationModalProps {
  inviteModalOpen: boolean
  setInviteModalOpen: (open: boolean) => void
  interviewees: ILightUser[]
  sendInvite: (() => void) | undefined
  onCancel?: () => void
}

const InviteConfirmationModal = ({
  inviteModalOpen,
  setInviteModalOpen,
  interviewees,
  sendInvite,
  onCancel,
}: IinviteConfirmationModalProps) => {
  const isSmaller = useIsSmallerBreakpoint('sm')

  const { interviewSlots } = useInterviewProcessContext()

  const slotInFutureAmount = Object.values(interviewSlots).reduce((acc, slots) => {
    return (
      acc +
      slots
        .filter((slot) => slot.bookedBy === null)
        .filter((slot) => new Date(slot.startDate) > new Date()).length
    )
  }, 0)

  return (
    <Modal
      opened={inviteModalOpen}
      onClose={() => setInviteModalOpen(false)}
      title={<Title order={3}>Confirm Invitation</Title>}
      size={'xl'}
      centered
    >
      <Stack>
        <Text>
          {`Are you sure you want to send${interviewees.length <= 1 ? ' a' : ''} interview invitation${interviewees.length > 1 ? 's' : ''} to the following interviewee${interviewees.length > 1 ? 's' : ''}?`}
        </Text>
        <ScrollArea.Autosize mih={'50px'} mah={'30vh'} w={'100%'} type='hover' bdrs={'md'}>
          <Stack>
            {interviewees.map((user) => (
              <Paper withBorder key={`inviteUser - ${user.userId}`} p='xs'>
                <AvatarUser user={user} />
              </Paper>
            ))}
          </Stack>
        </ScrollArea.Autosize>
        {slotInFutureAmount < interviewees.length && (
          <Alert
            variant='light'
            color='red'
            title='Not enough slots available'
            icon={<WarningCircleIcon size={16} />}
          >
            {`There ${slotInFutureAmount !== 1 ? 'are' : 'is'} ${slotInFutureAmount > 0 ? slotInFutureAmount : 'no'} interview slot${slotInFutureAmount !== 1 ? 's' : ''} available in the future. This is not enough for ${interviewees.length} interviewee${interviewees.length !== 1 ? 's' : ''}. Please create more interview slots before sending out all invitations.`}
          </Alert>
        )}
        <Group justify='end' align='center'>
          <Button
            variant='default'
            onClick={() => {
              setInviteModalOpen(false)
              onCancel?.()
            }}
          >
            Cancel
          </Button>
          <Button onClick={sendInvite}>Send Invitations</Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export default InviteConfirmationModal
