import { Card, Group, Stack, Title, Text, useMantineColorScheme, Badge } from '@mantine/core'
import { IInterviewSlot } from '../../../requests/responses/interview'
import { ClockIcon } from '@phosphor-icons/react'
import { useHover } from '@mantine/hooks'
import AvatarUser from '../../../components/AvatarUser/AvatarUser'
import { createScoreLabel, scoreColorTranslate } from '../../../utils/format'

interface ISlotItemProps {
  slot: IInterviewSlot
  withTimeSpan?: boolean
  withInterviewee?: boolean
  selected?: boolean
  onClick?: () => void
  disabled?: boolean
  hoverEffect?: boolean
}

const SlotItem = ({
  slot,
  withTimeSpan = false,
  withInterviewee = false,
  selected = false,
  onClick,
  disabled = false,
  hoverEffect = true,
}: ISlotItemProps) => {
  const { ref, hovered } = useHover()
  const { colorScheme } = useMantineColorScheme()

  const showHover = hoverEffect ? hovered : false

  return (
    <Card
      withBorder
      radius='md'
      py={'0.5rem'}
      px={'0.75rem'}
      onClick={disabled ? undefined : onClick}
      bg={
        selected
          ? 'primary'
          : showHover && !disabled
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
              : showHover && colorScheme === 'dark' && !disabled
                ? 'dark.9'
                : undefined
          }
        >
          {`${slot.startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${slot.endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
        </Title>
        {withInterviewee && (
          <Group align='center' justify='space-between' mih={30}>
            {slot.bookedBy ? (
              <AvatarUser
                user={slot.bookedBy.user}
                textColor='dimmed'
                textSize='sm'
                fontWeight={500}
              />
            ) : (
              <Text c={'dimmed'} size='sm' fw={500}>
                No interview yet
              </Text>
            )}
          </Group>
        )}
        {withTimeSpan && (
          <Group gap={'0.25rem'} align='center'>
            <ClockIcon
              size={14}
              color={
                !selected
                  ? showHover && colorScheme === 'dark' && !disabled
                    ? 'dark.9'
                    : 'gray'
                  : 'white'
              }
            />
            <Text
              size='xs'
              c={
                !selected
                  ? showHover && colorScheme === 'dark' && !disabled
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
