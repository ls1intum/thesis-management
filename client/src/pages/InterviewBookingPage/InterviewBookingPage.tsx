import { Button, Divider, Flex, Group, ScrollArea, Stack, Title, Text } from '@mantine/core'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import { IInterviewSlot } from '../../requests/responses/interview'
import { DateHeaderItem } from '../InterviewTopicOverviewPage/components/DateHeaderItem'
import SlotItem from '../InterviewTopicOverviewPage/components/SlotItem'
import { Carousel } from '@mantine/carousel'
import { useEffect, useState } from 'react'
import SummaryCard from './components/SummaryCard'
import { CalendarDotsIcon, ClockIcon, MapPinIcon } from '@phosphor-icons/react'
import { doRequest } from '../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'

const InterviewBookingPage = () => {
  const { processId } = useParams<{ processId: string }>()

  const [interviewSlots, setInterviewSlots] = useState<Record<string, IInterviewSlot[]>>({})
  const [interviewSlotsLoading, setInterviewSlotsLoading] = useState(false)

  //TODO: move to provider -> currently duplicate code with InterviewTopicOverviewPage
  const fetchInterviewSlots = async () => {
    setInterviewSlotsLoading(true)

    doRequest<IInterviewSlot[]>(
      `/v2/interview-process/${processId}/interview-slots`,
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          excludeBooked: true,
        },
      },
      (res) => {
        if (res.ok) {
          setInterviewSlots(groupSlotsByDate(res.data))
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setInterviewSlotsLoading(false)
      },
    )
  }

  function groupSlotsByDate(slots: IInterviewSlot[]): Record<string, IInterviewSlot[]> {
    return slots.reduce(
      (acc, slot) => {
        const startDate = new Date(slot.startDate)
        const endDate = new Date(slot.endDate)
        const dateKey = startDate.toISOString().slice(0, 10)
        const slotWithDates = { ...slot, startDate, endDate }
        if (!acc[dateKey]) acc[dateKey] = []
        acc[dateKey].push(slotWithDates)
        return acc
      },
      {} as Record<string, IInterviewSlot[]>,
    )
  }

  useEffect(() => {
    fetchInterviewSlots()
  }, [])

  const isSmaller = useIsSmallerBreakpoint('sm')

  const [selectedSlot, setSelectedSlot] = useState<IInterviewSlot | null>(null)

  const [carouselSlide, setCarouselSlide] = useState(0)

  const dateRowDisabled = (itemsPerPage: number, rowKey: string) => {
    const keys = Object.keys(interviewSlots)
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

  const isMobile = useIsSmallerBreakpoint('sm')
  const isSmallScreen = useIsSmallerBreakpoint('lg')
  const isMediumScreen = useIsSmallerBreakpoint('xl')

  const getSlideDisplayAmount = () => {
    const slideAmount = isMobile ? 1 : isSmallScreen ? 2 : isMediumScreen ? 3 : 4
    return Math.min(slideAmount, Object.keys(interviewSlots).length)
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
              slideSize={`${(100 - 14) / getSlideDisplayAmount()}%`}
              emblaOptions={{ align: 'center', slidesToScroll: getSlideDisplayAmount() }}
              onSlideChange={(index) => setCarouselSlide(index)}
              px={20}
            >
              {Object.entries(interviewSlots).map(([date, slots]) => (
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
