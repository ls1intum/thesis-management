import { Button, Group, Modal, Paper, ScrollArea, Stack, Text, Title } from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import AvatarUser from '../../../components/AvatarUser/AvatarUser'
import { ILightUser } from '../../../requests/responses/user'

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
