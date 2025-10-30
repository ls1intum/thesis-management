import { Accordion, Button, Card, Divider, Group, Stack, Text, Title } from '@mantine/core'
import { useHover } from '@mantine/hooks'
import dayjs from 'dayjs'
import { IIntervieweeSlot } from '../../../requests/responses/interview'

interface ICollapsibleDateCardProps {
  date: Date
}

interface ISlotRange {
  startTime: string | null
  endTime: string | null
  slots?: IIntervieweeSlot[]
}

const CollapsibleDateCard = ({ date }: ICollapsibleDateCardProps) => {
  const { ref, hovered } = useHover()

  return (
    <Card
      withBorder
      shadow={hovered ? 'md' : 'xs'}
      radius='md'
      my='sm'
      p={'0.25rem'}
      style={{ cursor: 'pointer' }}
      ref={ref}
    >
      <Accordion.Item key={date.toDateString()} value={date.toDateString()}>
        <Accordion.Control>
          <Group justify='space-between' align='center'>
            <Stack gap={0}>
              <Text fw={600} c={'dimmed'} size={'xs'}>
                {date.toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase()}
              </Text>
              <Title order={6}>{dayjs(date).format('MMM D, YYYY')}</Title>
            </Stack>
            <Group gap={'0.25rem'}>
              <Button
                size='xs'
                variant='outline'
                title='Add Slot in Range'
                onMouseDown={(e) => e.stopPropagation()}
                onClick={(e) => {
                  e.stopPropagation()
                  e.preventDefault()
                  // your add-range logic here
                }}
              >
                Add range
              </Button>
              <Button
                size='xs'
                variant='outline'
                title='Add a single Slot'
                onMouseDown={(e) => e.stopPropagation()}
                onClick={(e) => {
                  e.stopPropagation()
                  e.preventDefault()
                  // your add-slot logic here
                }}
              >
                Add slot
              </Button>
            </Group>
          </Group>
        </Accordion.Control>
        <Accordion.Panel>
          <Stack>
            <Divider />
            <Text>Interview Length:</Text>
            <Text>Interview Break:</Text>
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Card>
  )
}

export default CollapsibleDateCard
