import { Button, createPolymorphicComponent, Group, Modal, Stack, Text } from '@mantine/core'
import { forwardRef, ReactNode, useState } from 'react'
import { ButtonProps } from '@mantine/core/lib/components/Button/Button'

interface IConfirmationButtonProps extends ButtonProps {
  confirmationTitle: string
  confirmationText: string
  confirmationAdditionalInformation?: ReactNode
  onClick?: () => unknown
}

const ConfirmationButton = createPolymorphicComponent<'button', IConfirmationButtonProps>(
  forwardRef<HTMLButtonElement, IConfirmationButtonProps>(
    (
      {
        confirmationTitle,
        confirmationText,
        confirmationAdditionalInformation: confirmationAdditionalInformation,
        children,
        onClick,
        ...others
      },
      ref,
    ) => {
      const [opened, setOpened] = useState(false)

      return (
        <Button {...others} onClick={() => setOpened(true)} ref={ref}>
          <Modal
            opened={opened}
            onClose={() => setOpened(false)}
            title={confirmationTitle}
            onClick={(e) => e.stopPropagation()}
          >
            <Stack>
              <Text>{confirmationText}</Text>
              {confirmationAdditionalInformation && confirmationAdditionalInformation}
              <Group grow>
                <Button variant='outline' color='red' onClick={() => setOpened(false)}>
                  Cancel
                </Button>
                <Button
                  variant='outline'
                  color='green'
                  onClick={() => {
                    setOpened(false)
                    onClick?.()
                  }}
                >
                  Confirm
                </Button>
              </Group>
            </Stack>
          </Modal>
          {children}
        </Button>
      )
    },
  ),
)

ConfirmationButton.displayName = 'ConfirmationButton'

export default ConfirmationButton
