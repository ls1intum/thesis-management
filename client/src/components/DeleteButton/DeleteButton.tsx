import { ActionIcon } from '@mantine/core'
import { Trash } from 'phosphor-react'

type IDeleteButtonProps = {
  onClick: () => void
  disabled?: boolean
}

const DeleteButton = ({ onClick, disabled = false }: IDeleteButtonProps) => {
  return (
    <ActionIcon color='red' variant='light' onClick={onClick} disabled={disabled}>
      <Trash size={16} />
    </ActionIcon>
  )
}

export default DeleteButton
