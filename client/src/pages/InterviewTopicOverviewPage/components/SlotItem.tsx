import { Card, Group, Stack, Title, Text, useMantineColorScheme } from '@mantine/core'
import { IIntervieweeSlot } from '../../../requests/responses/interview'
import { ClockIcon } from '@phosphor-icons/react'
import { useHover } from '@mantine/hooks'

interface ISlotItemProps {
  slot: IIntervieweeSlot
  withTimeSpan?: boolean
  selected?: boolean
  onClick?: () => void
  disabled?: boolean
}

const SlotItem = ({
  slot,
  withTimeSpan = false,
  selected = false,
  onClick,
  disabled = false,
}: ISlotItemProps) => {
  const { ref, hovered } = useHover()
  const { colorScheme } = useMantineColorScheme()

  return (
    <Card
      withBorder
      radius='md'
      p={'0.5rem'}
      onClick={disabled ? undefined : onClick}
      bg={
        selected
          ? 'primary'
          : hovered && !disabled
            ? colorScheme === 'dark'
              ? 'primary.2'
              : 'primary.0'
            : undefined
      }
      style={{ cursor: onClick && !disabled ? 'pointer' : 'default' }}
      ref={ref}
      opacity={disabled ? 0.35 : 1}
    >
      <Stack gap={'0.25rem'}>
        <Title
          order={6}
          c={
            selected
              ? 'white'
              : hovered && colorScheme === 'dark' && !disabled
                ? 'dark.9'
                : undefined
          }
        >
          {`${slot.startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${slot.endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
        </Title>
        {withTimeSpan && (
          <Group gap={'0.25rem'} align='center'>
            <ClockIcon
              size={14}
              color={
                !selected
                  ? hovered && colorScheme === 'dark' && !disabled
                    ? 'dark.9'
                    : 'gray'
                  : 'white'
              }
            />
            <Text
              size='xs'
              c={
                !selected
                  ? hovered && colorScheme === 'dark' && !disabled
                    ? 'dark.9'
                    : 'dimmed'
                  : 'white'
              }
            >
              {(() => {
                const minutes = Math.round(
                  (slot.endDate.getTime() - slot.startDate.getTime()) / 60000,
                )
                return `${minutes} min`
              })()}
            </Text>
          </Group>
        )}
      </Stack>
    </Card>
  )
}

export default SlotItem
