import { Divider, Flex, Grid, ScrollArea, Stack, Title } from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import { IIntervieweeSlot } from '../../requests/responses/interview'
import { useState } from 'react'
import { DateHeaderItem } from '../InterviewTopicOverviewPage/components/DateHeaderItem'
import SlotItem from '../InterviewTopicOverviewPage/components/SlotItem'

const InterviewBookingPage = () => {
  const interviewSlotItems: Record<string, IIntervieweeSlot[]> = {
    //TODO: replace with real data & make sure only available, in the future and not booked slots are shown
    '2025-11-10': [
      {
        slotId: '1',
        startDate: new Date('2025-11-10T10:00:00Z'),
        endDate: new Date('2025-11-10T10:30:00Z'),
        bookedBy: null,
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
        bookedBy: null,
      },
    ],
    '2025-11-12': [
      {
        slotId: '4',
        startDate: new Date('2025-11-12T10:00:00Z'),
        endDate: new Date('2025-11-12T10:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '5',
        startDate: new Date('2025-11-12T11:00:00Z'),
        endDate: new Date('2025-11-12T11:30:00Z'),
        bookedBy: null,
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
    '2025-11-20': [
      {
        slotId: '10',
        startDate: new Date('2025-11-20T10:00:00Z'),
        endDate: new Date('2025-11-20T10:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '11',
        startDate: new Date('2025-11-20T11:00:00Z'),
        endDate: new Date('2025-11-20T11:30:00Z'),
        bookedBy: null,
      },
    ],
    '2025-11-27': [
      {
        slotId: '12',
        startDate: new Date('2025-11-27T10:00:00Z'),
        endDate: new Date('2025-11-27T10:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '13',
        startDate: new Date('2025-11-27T11:00:00Z'),
        endDate: new Date('2025-11-27T11:30:00Z'),
        bookedBy: null,
      },
    ],
  }

  const [selectedSlot, setSelectedSlot] = useState<IIntervieweeSlot | null>(null)

  const [slotPage, setSlotPage] = useState(1)

  const isSmaller = useIsSmallerBreakpoint('sm')

  const getEntriesPage = (page = 0, pageSize = 4) => {
    const entries = Object.entries(interviewSlotItems)
    const total = entries.length
    const normalizedPage = Math.max(0, page)
    const requestedStart = normalizedPage * pageSize

    const start = Math.min(requestedStart, Math.max(0, total - pageSize))
    return entries.slice(start, start + pageSize)
  }

  return (
    <Stack gap={'2rem'} h={'100%'}>
      <Title>Select your Interview Slot</Title>
      <Flex
        h={'100%'}
        w={'100%'}
        direction={{ base: 'column', md: 'row' }}
        justify={'space-between'}
        gap={{ base: '1rem', md: '2rem' }}
        style={{ overflow: 'auto' }}
      >
        <Stack h={'100%'} flex={1} gap={'1.5rem'}>
          <ScrollArea h={'100%'} w={'100%'} type={isSmaller ? 'never' : 'hover'} offsetScrollbars>
            <Grid>
              {(() => {
                return getEntriesPage(slotPage).map(([date, slots]) => (
                  <Grid.Col span={3} key={date}>
                    <Stack gap={'1.5rem'}>
                      <DateHeaderItem date={date} size={'lg'} />{' '}
                      {/*TODO: Add this to general components*/}
                      {/*TODO: Add this to general components*/}
                      <Stack>
                        {slots
                          .sort((a, b) => a.startDate.getTime() - b.startDate.getTime())
                          .map((slot) => (
                            <SlotItem key={slot.slotId} slot={slot} withTimeSpan />
                          ))}
                      </Stack>
                    </Stack>
                  </Grid.Col>
                ))
              })()}
            </Grid>
          </ScrollArea>
        </Stack>
        <Divider orientation='vertical' />
        <Stack w={{ base: '100%', md: '25%' }} h={'100%'} gap={'1.5rem'}>
          <Title order={3}>Summary</Title>
          <ScrollArea h={'100%'} w={'100%'} type={isSmaller ? 'never' : 'hover'} offsetScrollbars>
            <Stack p={0} gap={'1rem'}>
              <div>TODO</div>
            </Stack>
          </ScrollArea>
        </Stack>
      </Flex>
    </Stack>
  )
}

export default InterviewBookingPage
