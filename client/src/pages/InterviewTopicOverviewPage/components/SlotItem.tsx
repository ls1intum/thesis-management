import {
  Card,
  Group,
  Stack,
  Title,
  Text,
  useMantineColorScheme,
  Button,
  Badge,
} from '@mantine/core'
import { IInterviewSlot } from '../../../requests/responses/interview'
import { ClockIcon } from '@phosphor-icons/react'
import { useHover } from '@mantine/hooks'
import AvatarUser from '../../../components/AvatarUser/AvatarUser'
import AssignIntervieweeToSlotModal from './AssignIntervieweeToSlotModal'
import { useState } from 'react'

interface ISlotItemProps {
  slot: IInterviewSlot
  withTimeSpan?: boolean
  withInterviewee?: boolean
  withDate?: boolean
  selected?: boolean
  onClick?: () => void
  disabled?: boolean
  hoverEffect?: boolean
  assignable?: boolean
  isPast?: boolean
}

const SlotItem = ({
  slot,
  withTimeSpan = false,
  withInterviewee = false,
  withDate = false,
  selected = false,
  onClick,
  disabled = false,
  hoverEffect = true,
  assignable = false,
  isPast = false,
}: ISlotItemProps) => {
  const { ref, hovered } = useHover()
  const { colorScheme } = useMantineColorScheme()

  const showHover = hoverEffect ? hovered : false

  const [assignModalOpen, setAssignModalOpen] = useState(false)

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
            : isPast
              ? colorScheme === 'dark'
                ? 'dark.6'
                : 'gray.1'
              : undefined
      }
      style={{
        cursor: onClick && !disabled ? 'pointer' : 'default',
        userSelect: 'none',
        WebkitUserSelect: 'none',
      }}
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
          {`${new Date(slot.startDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${new Date(slot.endDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
        </Title>
        {withInterviewee && (
          <Group align='center' justify='space-between' mih={30} w={'100%'}>
            {slot.bookedBy ? (
              <AvatarUser
                user={slot.bookedBy.user}
                textColor='dimmed'
                textSize='sm'
                fontWeight={500}
              />
            ) : (
              <Group align='center' justify='space-between' w={'100%'} gap={'0.25rem'}>
                <Text c={'dimmed'} size='sm' fw={500}>
                  {isPast ? 'No interview' : 'Open slot'}
                </Text>
                {assignable && (
                  <Button onClick={() => setAssignModalOpen(true)} size='xs' variant={'subtle'}>
                    Assign
                  </Button>
                )}
              </Group>
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
        {withDate && (
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
            {`${new Date(slot.startDate).toLocaleDateString('en-US', {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            })}`}
          </Text>
        )}
      </Stack>
      <AssignIntervieweeToSlotModal
        slot={slot}
        assignModalOpen={assignModalOpen}
        setAssignModalOpen={setAssignModalOpen}
      />
    </Card>
  )
}

export default SlotItem
