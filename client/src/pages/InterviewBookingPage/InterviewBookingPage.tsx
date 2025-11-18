import { Button, Divider, Flex, Group, ScrollArea, Stack, Title, Text } from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import { IInterviewSlot } from '../../requests/responses/interview'
import { DateHeaderItem } from '../InterviewTopicOverviewPage/components/DateHeaderItem'
import SlotItem from '../InterviewTopicOverviewPage/components/SlotItem'
import { Carousel } from '@mantine/carousel'
import { useState } from 'react'
import SummaryCard from './components/SummaryCard'
import { CalendarDotsIcon, ClockIcon, MapPinIcon } from '@phosphor-icons/react'

const InterviewBookingPage = () => {
  const interviewSlotItems: Record<string, IInterviewSlot[]> = {
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
    '2025-11-28': [
      {
        slotId: '14',
        startDate: new Date('2025-11-28T10:00:00Z'),
        endDate: new Date('2025-11-28T10:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '15',
        startDate: new Date('2025-11-28T11:00:00Z'),
        endDate: new Date('2025-11-28T11:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '16',
        startDate: new Date('2025-11-28T11:00:00Z'),
        endDate: new Date('2025-11-28T11:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '17',
        startDate: new Date('2025-11-28T12:00:00Z'),
        endDate: new Date('2025-11-28T12:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '18',
        startDate: new Date('2025-11-28T12:30:00Z'),
        endDate: new Date('2025-11-28T13:00:00Z'),
        bookedBy: null,
      },
    ],
  }

  const isSmaller = useIsSmallerBreakpoint('sm')

  const [selectedSlot, setSelectedSlot] = useState<IInterviewSlot | null>(null)

  const [carouselSlide, setCarouselSlide] = useState(0)

  const dateRowDisabled = (itemsPerPage: number, rowKey: string) => {
    const keys = Object.keys(interviewSlotItems)
    const totalSlides = keys.length
    const index = keys.indexOf(rowKey)
    if (index === -1) return true

    const lastSlideIndex = Math.max(0, Math.ceil(totalSlides / itemsPerPage) - 1)

    let visibleSlidesStart = carouselSlide * itemsPerPage
    let visibleSlidesEnd = visibleSlidesStart + itemsPerPage

    // If we're on the last carousel page, ensure the last `itemsPerPage` items are visible
    if (carouselSlide >= lastSlideIndex) {
      visibleSlidesStart = Math.max(0, totalSlides - itemsPerPage)
      visibleSlidesEnd = totalSlides
    }

    return index < visibleSlidesStart || index >= visibleSlidesEnd
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
          <ScrollArea
            h={'100%'}
            w={'100%'}
            type={isSmaller ? 'never' : 'hover'}
            offsetScrollbars
            flex={1}
          >
            <Carousel
              slideGap='sm'
              controlsOffset={'-100px'}
              controlSize={32}
              withControls
              withIndicators={false}
              slideSize={'23%'}
              emblaOptions={{ align: 'start', slidesToScroll: 4 }}
              onSlideChange={(index) => setCarouselSlide(index)}
              px={20}
            >
              {Object.entries(interviewSlotItems).map(([date, slots]) => (
                <Carousel.Slide key={date}>
                  <Stack gap={'1.5rem'}>
                    <DateHeaderItem date={date} size={'lg'} disabled={dateRowDisabled(4, date)} />
                    {/*TODO: Add this to general components*/}
                    <Stack>
                      {slots
                        .sort((a, b) => a.startDate.getTime() - b.startDate.getTime())
                        .map((slot) => (
                          <SlotItem
                            key={slot.slotId}
                            slot={slot}
                            withTimeSpan
                            selected={selectedSlot?.slotId === slot.slotId}
                            onClick={() => setSelectedSlot(slot)}
                            disabled={dateRowDisabled(4, date)}
                          />
                        ))}
                      {/*TODO: Add this to general components*/}
                    </Stack>
                  </Stack>
                </Carousel.Slide>
              ))}
            </Carousel>
          </ScrollArea>
          <Group justify='end' align='center' py={2}>
            <Button variant='outline'>Not available on any slot</Button>
          </Group>
        </Stack>
        <Divider orientation='vertical' />
        <Stack w={{ base: '100%', md: '25%' }} h={'100%'} gap={'1.5rem'}>
          <Title order={3}>Summary</Title>
          <ScrollArea
            h={'100%'}
            w={'100%'}
            type={isSmaller ? 'never' : 'hover'}
            offsetScrollbars
            flex={1}
          >
            <Stack p={0} h={'100%'}>
              {selectedSlot && (
                <SummaryCard
                  title={'Selected Interview'}
                  sections={[
                    {
                      title: 'Date',
                      content: (
                        <Text size='xs' pl={'xs'}>
                          {selectedSlot.startDate.toLocaleDateString()}
                        </Text>
                      ),
                      icon: <CalendarDotsIcon />,
                    },
                    {
                      title: 'Time',
                      content: (
                        <Text size='xs' pl={'xs'}>
                          {`${selectedSlot.startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${selectedSlot.endDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}, ${`${Math.round(
                            (selectedSlot.endDate.getTime() - selectedSlot.startDate.getTime()) /
                              60000,
                          )} min`}`}
                        </Text>
                      ),
                      icon: <ClockIcon />,
                    },
                    {
                      title: 'Location',
                      content: (
                        <Text size='xs' pl={'xs'}>
                          {selectedSlot.location || selectedSlot.streamUrl || 'Not specified'}
                        </Text>
                      ),
                      icon: <MapPinIcon />,
                    },
                  ]}
                ></SummaryCard>
              )}
            </Stack>
          </ScrollArea>
          <Button fullWidth>Reserve Interview Slot</Button>
        </Stack>
      </Flex>
    </Stack>
  )
}

export default InterviewBookingPage
