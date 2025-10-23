import { Card, Group, Stack, Title, Text } from '@mantine/core'
import { IIntervieweeSlot } from '../../../requests/responses/interview'
import { ClockIcon } from '@phosphor-icons/react'

interface ISlotItemProps {
  slot: IIntervieweeSlot
  withTimeSpan?: boolean
}

const SlotItem = ({ slot, withTimeSpan = false }: ISlotItemProps) => {
  return (
    <Card withBorder radius='md' p={'0.5rem'}>
      <Stack gap={'0.25rem'}>
        <Title order={6}>
          {`${slot.startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${slot.endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
        </Title>
        {withTimeSpan && (
          <Group gap={'0.25rem'} align='center'>
            <ClockIcon size={14} color='gray' />
            <Text size='xs' c='dimmed'>
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
