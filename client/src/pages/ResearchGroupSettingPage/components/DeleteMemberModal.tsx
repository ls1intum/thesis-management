import { Modal, Text, Button, Group } from '@mantine/core'
import { ILightUser } from '../../../requests/responses/user'

type IDeleteMemberModalProps = {
  opened: boolean
  onClose: () => void
  onConfirm: () => void
  member?: ILightUser
}

const DeleteMemberModal = ({ opened, onClose, onConfirm, member }: IDeleteMemberModalProps) => {
  return (
    <Modal opened={opened} onClose={onClose} title='Confirm Deletion' centered>
      <Text mb='md'>
        Are you sure you want to remove{' '}
        {member ? `${member?.firstName} ${member?.lastName}` : 'this user'} from the research group?
      </Text>

      <Group justify='flex-end'>
        <Button variant='outline' color='red' onClick={onConfirm}>
          Remove
        </Button>
      </Group>
    </Modal>
  )
}

export default DeleteMemberModal
