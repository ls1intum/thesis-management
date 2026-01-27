import { Carousel } from '@mantine/carousel'
import {
  Badge,
  Button,
  Center,
  Divider,
  Group,
  Loader,
  Stack,
  Title,
  Text,
  Popover,
  CopyButton,
  TextInput,
  Tooltip,
  ActionIcon,
  CheckIcon,
} from '@mantine/core'
import { CalendarDotsIcon, ClockUserIcon, CopyIcon, PlusIcon } from '@phosphor-icons/react'
import { IInterviewSlot } from '../../../requests/responses/interview'
import { DateHeaderItem } from './DateHeaderItem'
import SlotItem from './SlotItem'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import { useEffect, useState } from 'react'
import AddSlotsModal from './AddSlotsModal'
import { useInterviewProcessContext } from '../../../providers/InterviewProcessProvider/hooks'
import { GLOBAL_CONFIG } from '../../../config/global'
import { useUser } from '../../../hooks/authentication'

const CalendarCarousel = () => {
  const [carouselSlide, setCarouselSlide] = useState(0)

  const [firstSlideIndexForDate, setFirstSlideIndexForDate] = useState<Record<string, number>>({})
  const [totalSlides, setTotalSlides] = useState(0)

  const { interviewSlots, interviewSlotsLoading } = useInterviewProcessContext()

  useEffect(() => {
    let slideIndex = 0
    const firstSlideIndexForDateTemp: Record<string, number> = {}
    Object.entries(interviewSlots).forEach(([date, slots]) => {
      const chunks = Math.ceil(slots.length / rowAmount)
      firstSlideIndexForDateTemp[date] = slideIndex
      slideIndex += chunks
    })
    setFirstSlideIndexForDate(firstSlideIndexForDateTemp)
    setTotalSlides(slideIndex)
  }, [interviewSlots])

  const dateRowDisabled = (rowKey: string, chunkIndex: number) => {
    const index = firstSlideIndexForDate[rowKey] + chunkIndex
    if (index === -1) return true

    const itemsPerPage = getSlideDisplayAmount()

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

  const isSmaller = useIsSmallerBreakpoint('md')
  const isMobile = useIsSmallerBreakpoint('sm')
  const isSmallScreen = useIsSmallerBreakpoint('lg')
  const isMediumScreen = useIsSmallerBreakpoint('xl')

  const [slotModalOpen, setSlotModalOpen] = useState(false)

  const rowAmount = isSmaller ? 2 : 3

  const getSlideDisplayAmount = () => {
    const slideAmount = isMobile ? 1 : isSmallScreen ? 2 : isMediumScreen ? 3 : 4
    return Math.min(slideAmount, totalSlides)
  }

  const calendarHeader = () => {
    //TODO: LOGIC DOES NOT WORK YET DUE TO MULTIPLE ROWS PER DATE
    const keys = Object.keys(interviewSlots)
    const itemsPerPage = getSlideDisplayAmount()
    const totalSlides = keys.length
    const lastSlideIndex = Math.max(0, Math.ceil(totalSlides / itemsPerPage) - 1)

    let visibleStart = carouselSlide * itemsPerPage
    if (carouselSlide >= lastSlideIndex) {
      visibleStart = Math.max(0, totalSlides - itemsPerPage)
    }

    const firstKey = keys[visibleStart]
    return firstKey
      ? new Date(firstKey).toLocaleString(undefined, { month: 'long', year: 'numeric' })
      : ''
  }

  const dateBeforeIsDiffrentMonth = (date: string): boolean => {
    const indexOfDate = Object.keys(interviewSlots).indexOf(date)
    if (indexOfDate <= 0) return false

    const previousDate = Object.keys(interviewSlots)[indexOfDate - 1]
    const currentDateObj = new Date(date)
    const previousDateObj = new Date(previousDate)
    return (
      currentDateObj.getMonth() !== previousDateObj.getMonth() ||
      currentDateObj.getFullYear() !== previousDateObj.getFullYear()
    )
  }

  const user = useUser()

  const calendarUrl =
    GLOBAL_CONFIG.calendar_url ||
    `${GLOBAL_CONFIG.server_host}/api/v2/calendar/interviews/user/${user ? user.userId : ''}`

  return (
    <Stack gap={'0.25rem'}>
      <Group justify='space-between' align='center' gap={'0.5rem'}>
        <Title order={3}>{calendarHeader()}</Title>
        <Group gap={'0.5rem'}>
          <Popover position='bottom' withArrow shadow='md'>
            <Popover.Target>
              <Button variant='outline' size='xs' leftSection={<CalendarDotsIcon size={16} />}>
                {isSmaller ? 'Subscribe' : 'Subscribe to Calendar'}
              </Button>
            </Popover.Target>
            <Popover.Dropdown>
              <Group>
                <Text c='dimmed'>Subscribe to Calendar</Text>
                <div style={{ flexGrow: 1 }}>
                  <CopyButton value={calendarUrl}>
                    {({ copied, copy }) => (
                      <TextInput
                        value={calendarUrl}
                        onChange={() => undefined}
                        onClick={(e) => e.currentTarget.select()}
                        rightSection={
                          <Tooltip label={copied ? 'Copied' : 'Copy'} withArrow position='right'>
                            <ActionIcon
                              color={copied ? 'teal' : 'gray'}
                              variant='subtle'
                              onClick={copy}
                            >
                              {copied ? <CheckIcon size={16} /> : <CopyIcon size={16} />}
                            </ActionIcon>
                          </Tooltip>
                        }
                      />
                    )}
                  </CopyButton>
                </div>
              </Group>
            </Popover.Dropdown>
          </Popover>
          <Button
            size='xs'
            leftSection={<PlusIcon size={16} />}
            onClick={() => setSlotModalOpen(true)}
          >
            {isSmaller ? 'Add' : 'Add Slot'}
          </Button>
        </Group>
      </Group>
      {interviewSlotsLoading ? (
        <Center h={'100%'} mih={'30vh'}>
          <Loader />
        </Center>
      ) : Object.keys(interviewSlots).length === 0 ? (
        <Center h={'100%'} mih={'20vh'}>
          <Stack justify='center' align='center' h={'100%'} gap={'0.5rem'}>
            <ClockUserIcon size={50} />
            <Stack gap={'0.25rem'} justify='center' align='center'>
              <Title order={5}>No Slots Found</Title>
              <Text c='dimmed' ta={'center'}>
                Add a new slot to get started
              </Text>
            </Stack>
          </Stack>
        </Center>
      ) : (
        <Carousel
          slideGap={'0.5rem'}
          controlsOffset={'-100px'}
          controlSize={32}
          withControls
          withIndicators={false}
          slideSize={`${(100 - 14) / getSlideDisplayAmount()}%`}
          emblaOptions={{ align: 'center', slidesToScroll: getSlideDisplayAmount() }}
          onSlideChange={(index) => setCarouselSlide(index)}
          px={20}
        >
          {Object.entries(interviewSlots)
            .sort(([dateA], [dateB]) => new Date(dateA).getTime() - new Date(dateB).getTime())
            .map(([date, slotsUnsorted]) => {
              const slots = slotsUnsorted.sort(
                (a, b) => a.startDate.getTime() - b.startDate.getTime(),
              )
              const chunks: IInterviewSlot[][] = []
              for (let i = 0; i < slots.length; i += rowAmount) {
                chunks.push(slots.slice(i, i + rowAmount))
              }

              return (
                <>
                  {chunks.map((chunk, chunkIndex) => (
                    <Carousel.Slide key={`${date}-${chunkIndex}`}>
                      <Stack gap={'0.5rem'}>
                        {chunkIndex === 0 ? (
                          <Group align={'end'} justify={'flex-start'} gap={'0.25rem'} h={70}>
                            {dateBeforeIsDiffrentMonth(date) && (
                              <Stack gap={0} w={50} ml={-29}>
                                <Group justify='center' w={50}>
                                  <Badge h={20} mb={5} size='sm'>
                                    {new Date(date).toLocaleString(undefined, { month: 'short' })}
                                  </Badge>
                                </Group>
                                <Group justify='center' w={50}>
                                  <Divider
                                    orientation='vertical'
                                    size='md'
                                    h={40}
                                    mb={5}
                                    color='primary'
                                  />
                                </Group>
                              </Stack>
                            )}
                            <DateHeaderItem
                              date={date}
                              h={50}
                              disabled={dateRowDisabled(date, chunkIndex)}
                            />
                          </Group>
                        ) : (
                          <Stack h={70} />
                        )}
                        {chunk.map((slot) => {
                          const isFuture = new Date(slot.endDate) > new Date()

                          return (
                            <SlotItem
                              slot={slot}
                              key={slot.slotId}
                              withInterviewee
                              disabled={dateRowDisabled(date, chunkIndex)}
                              hoverEffect={false}
                              assignable={isFuture ? true : false}
                              isPast={!isFuture}
                              withLocation
                            />
                          )
                        })}
                      </Stack>
                    </Carousel.Slide>
                  ))}
                </>
              )
            })}
        </Carousel>
      )}
      <AddSlotsModal
        slotModalOpen={slotModalOpen}
        setSlotModalOpen={setSlotModalOpen}
        interviewSlotItems={interviewSlots}
      />
    </Stack>
  )
}
export default CalendarCarousel
