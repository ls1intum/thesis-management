import { Modal, Text, Button, Group, Alert, Stack } from '@mantine/core'
import { ILightUser } from '../../../requests/responses/user'
import { WarningCircle } from 'phosphor-react'

type IDeleteMemberModalProps = {
  opened: boolean
  onClose: () => void
  onConfirm: () => void
  member?: ILightUser
}

const DeleteMemberModal = ({ opened, onClose, onConfirm, member }: IDeleteMemberModalProps) => {
  return (
    <Modal opened={opened} onClose={onClose} title='Confirm Deletion' centered>
      <Stack>
        <Text>
          Are you sure you want to remove{' '}
          {member ? `${member?.firstName} ${member?.lastName}` : 'this user'} from the research
          group?
        </Text>

        <Alert variant='light' color='orange' title='Important' icon={<WarningCircle size={16} />}>
          The removed member might have open theses and topics. Please ensure that you have
          considered this before proceeding.
        </Alert>

        <Group justify='flex-end'>
          <Button variant='outline' color='red' onClick={onConfirm}>
            Remove
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export default DeleteMemberModal
