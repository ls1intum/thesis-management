import { Button, Group, Modal, Paper, Stack, Text, Title } from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'

interface IinviteConfirmationModalProps {
  inviteModalOpen: boolean
  setInviteModalOpen: (open: boolean) => void
  intervieweeNames: string[]
  sendInvite: (() => void) | undefined
  onCancel?: () => void
}

const InviteConfirmationModal = ({
  inviteModalOpen,
  setInviteModalOpen,
  intervieweeNames,
  sendInvite,
  onCancel,
}: IinviteConfirmationModalProps) => {
  const isSmaller = useIsSmallerBreakpoint('sm')

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
          {`Are you sure you want to send${intervieweeNames.length <= 1 ? ' a' : ''} interview invitation${intervieweeNames.length > 1 ? 's' : ''} to the following interviewee${intervieweeNames.length > 1 ? 's' : ''}?`}
        </Text>
        <Stack>
          {intervieweeNames.map((name) => (
            <Paper withBorder key={name} p='xs'>
              <Text>{name}</Text>
            </Paper>
          ))}
        </Stack>
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
