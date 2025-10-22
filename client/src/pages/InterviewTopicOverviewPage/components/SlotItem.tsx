import { Card, Stack, Title } from '@mantine/core'
import { IIntervieweeSlot } from '../../../requests/responses/interview'

const SlotItem = ({ slot }: { slot: IIntervieweeSlot }) => {
  return (
    <Card withBorder radius='md' p={'0.5rem'}>
      <Stack>
        <Title order={6}>
          {`${slot.startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${slot.endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
        </Title>
      </Stack>
    </Card>
  )
}

export default SlotItem
