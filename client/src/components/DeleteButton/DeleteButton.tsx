import { ActionIcon, Tooltip } from '@mantine/core'
import { TrashIcon } from '@phosphor-icons/react'

type IDeleteButtonProps = {
  onClick: () => void
  disabled?: boolean
  iconSize?: number
  buttonSize?: number
  tooltipText?: string
  tooltipTextPosition?: 'top' | 'bottom' | 'left' | 'right'
  tooltipOnlyWhenDisabled?: boolean
}

const DeleteButton = ({
  onClick,
  disabled = false,
  iconSize = 16,
  buttonSize,
  tooltipText,
  tooltipTextPosition = 'top',
  tooltipOnlyWhenDisabled = false,
}: IDeleteButtonProps) => {
  const renderButton = () => {
    return (
      <ActionIcon
        color='red'
        variant='light'
        onClick={onClick}
        disabled={disabled}
        size={buttonSize}
      >
        <TrashIcon size={iconSize} />
      </ActionIcon>
    )
  }

  return (
    <>
      {tooltipText && (tooltipOnlyWhenDisabled ? disabled : true) ? (
        <Tooltip
          label={tooltipText}
          position={tooltipTextPosition}
          withArrow
          multiline
          maw={'50vw'}
        >
          {renderButton()}
        </Tooltip>
      ) : (
        renderButton()
      )}
    </>
  )
}

export default DeleteButton
