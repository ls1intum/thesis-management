import { Modal, Text, Button, Group, Alert, Stack } from '@mantine/core'
import { ILightUser } from '../../../requests/responses/user'
import { WarningCircle } from 'phosphor-react'
import DeleteButton from '../../../components/DeleteButton/DeleteButton'
import { useState } from 'react'

type IDeleteMemberModalProps = {
  onConfirm: () => void
  member?: ILightUser
  disabled?: boolean
}

const DeleteMemberModal = ({ onConfirm, member, disabled }: IDeleteMemberModalProps) => {
  const [deleteMemberModalOpened, setDeleteMemberModalOpened] = useState(false)

  return (
    <div>
      <DeleteButton
        onClick={() => {
          setDeleteMemberModalOpened(true)
        }}
        disabled={disabled}
      />
      <Modal
        opened={deleteMemberModalOpened}
        onClose={() => setDeleteMemberModalOpened(false)}
        title='Confirm Deletion'
        centered
      >
        <Stack>
          <Text>
            Are you sure you want to remove{' '}
            {member ? `${member?.firstName} ${member?.lastName}` : 'this user'} from the research
            group?
          </Text>

          <Alert
            variant='light'
            color='orange'
            title='Important'
            icon={<WarningCircle size={16} />}
          >
            The removed member might have open theses and topics. Please ensure that you have
            considered this before proceeding.
          </Alert>

          <Group justify='flex-end'>
            <Button
              variant='outline'
              color='red'
              onClick={() => {
                onConfirm()
                setDeleteMemberModalOpened(false)
              }}
            >
              Remove
            </Button>
          </Group>
        </Stack>
      </Modal>
    </div>
  )
}

export default DeleteMemberModal
