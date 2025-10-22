import { Grid, Stack, Title } from '@mantine/core'
import { IIntervieweeSlot } from '../../requests/responses/interview'
import { DateHeaderItem } from './components/DateHeaderItem'
import SlotItem from './components/SlotItem'

const InterviewTopicOverviewPage = () => {
  const interviewSlotItems: Record<string, IIntervieweeSlot[]> = {
    //TODO: replace with real data
    '2025-11-10': [
      {
        slotId: '1',
        startDate: new Date('2025-11-10T10:00:00Z'),
        endDate: new Date('2025-11-10T10:30:00Z'),
        bookedBy: {
          intervieweeId: 'u1',
          firstName: 'Alice',
          lastName: 'Smith',
          score: 4,
          lastInvited: null,
        },
      },
    ],
    '2025-11-11': [
      {
        slotId: '2',
        startDate: new Date('2025-11-11T11:00:00Z'),
        endDate: new Date('2025-11-11T11:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '3',
        startDate: new Date('2025-11-11T12:00:00Z'),
        endDate: new Date('2025-11-11T12:30:00Z'),
        bookedBy: {
          intervieweeId: 'u2',
          firstName: 'Bob',
          lastName: 'Johnson',
          score: 5,
          lastInvited: null,
        },
      },
    ],
    '2025-11-12': [
      {
        slotId: '4',
        startDate: new Date('2025-11-12T10:00:00Z'),
        endDate: new Date('2025-11-12T10:30:00Z'),
        bookedBy: {
          intervieweeId: 'u3',
          firstName: 'Charlie',
          lastName: 'Brown',
          score: 1,
          lastInvited: null,
        },
      },
      {
        slotId: '5',
        startDate: new Date('2025-11-12T11:00:00Z'),
        endDate: new Date('2025-11-12T11:30:00Z'),
        bookedBy: {
          intervieweeId: 'u4',
          firstName: 'David',
          lastName: 'Wilson',
          score: null,
          lastInvited: null,
        },
      },
      {
        slotId: '6',
        startDate: new Date('2025-11-12T12:00:00Z'),
        endDate: new Date('2025-11-12T12:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '7',
        startDate: new Date('2025-11-12T13:00:00Z'),
        endDate: new Date('2025-11-12T13:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '8',
        startDate: new Date('2025-11-12T14:00:00Z'),
        endDate: new Date('2025-11-12T14:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '9',
        startDate: new Date('2025-11-12T15:00:00Z'),
        endDate: new Date('2025-11-12T15:30:00Z'),
        bookedBy: null,
      },
    ],
  }

  return (
    <Stack h={'100%'} gap={'1.5rem'}>
      <Stack gap={'0.5rem'}>
        <Title>Interview Management</Title>
        <Title order={4} c={'dimmed'}>
          Integrating Gender Sensitivity and Adaptive Learning in Education Games
        </Title>
      </Stack>

      <Grid gutter={'xs'}>
        {Object.entries(interviewSlotItems).map(([date, slots]) => (
          <Grid.Col span={3} key={`dateitem-${date}`}>
            <Stack gap={'0.25rem'}>
              <DateHeaderItem date={date} />
              {slots.map((slot) => (
                <SlotItem slot={slot} key={slot.slotId} />
              ))}
            </Stack>
          </Grid.Col>
        ))}
      </Grid>
    </Stack>
  )
}
export default InterviewTopicOverviewPage
