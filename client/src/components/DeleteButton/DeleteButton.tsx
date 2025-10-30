import { ActionIcon } from '@mantine/core'
import { TrashIcon } from '@phosphor-icons/react'

type IDeleteButtonProps = {
  onClick: () => void
  disabled?: boolean
  iconSize?: number
  buttonSize?: number
}

const DeleteButton = ({
  onClick,
  disabled = false,
  iconSize = 16,
  buttonSize,
}: IDeleteButtonProps) => {
  return (
    <ActionIcon color='red' variant='light' onClick={onClick} disabled={disabled} size={buttonSize}>
      <TrashIcon size={iconSize} />
    </ActionIcon>
  )
}

export default DeleteButton
