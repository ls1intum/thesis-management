import {
  Group,
  SegmentedControl,
  Stack,
  Title,
  Divider,
  Badge,
  Button,
  TextInput,
} from '@mantine/core'
import { IIntervieweeSlot, InterviewState } from '../../requests/responses/interview'
import { DateHeaderItem } from './components/DateHeaderItem'
import SlotItem from './components/SlotItem'
import { Carousel } from '@mantine/carousel'
import { useEffect, useState } from 'react'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import { MagnifyingGlassIcon, PaperPlaneTiltIcon, PlusIcon } from '@phosphor-icons/react'
import { CalendarDotsIcon } from '@phosphor-icons/react/dist/ssr'
import AddSlotsModal from './components/AddSlotsModal'

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
          user: {
            userId: 'u1',
            firstName: 'Alice',
            lastName: 'Smith',
            avatar: null,
            universityId: '',
            matriculationNumber: null,
            email: null,
            studyDegree: null,
            studyProgram: null,
            customData: null,
            joinedAt: '',
            groups: [],
          },
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
          user: {
            userId: 'u2',
            firstName: 'Bob',
            lastName: 'Johnson',
            avatar: null,
            universityId: '',
            matriculationNumber: null,
            email: null,
            studyDegree: null,
            studyProgram: null,
            customData: null,
            joinedAt: '',
            groups: [],
          },
          score: 5,
          lastInvited: null,
        },
      },
    ],
    '2025-12-12': [
      {
        slotId: '4',
        startDate: new Date('2025-12-12T10:00:00Z'),
        endDate: new Date('2025-12-12T10:30:00Z'),
        bookedBy: {
          intervieweeId: 'u3',
          user: {
            userId: 'u3',
            firstName: 'Charlie',
            lastName: 'Brown',
            avatar: null,
            universityId: '',
            matriculationNumber: null,
            email: null,
            studyDegree: null,
            studyProgram: null,
            customData: null,
            joinedAt: '',
            groups: [],
          },
          score: 1,
          lastInvited: null,
        },
      },
      {
        slotId: '5',
        startDate: new Date('2025-12-12T11:00:00Z'),
        endDate: new Date('2025-12-12T11:30:00Z'),
        bookedBy: {
          intervieweeId: 'u4',
          user: {
            userId: 'u4',
            firstName: 'Diana',
            lastName: 'Prince',
            avatar: null,
            universityId: '',
            matriculationNumber: null,
            email: null,
            studyDegree: null,
            studyProgram: null,
            customData: null,
            joinedAt: '',
            groups: [],
          },
          score: null,
          lastInvited: null,
        },
      },
      {
        slotId: '6',
        startDate: new Date('2025-12-12T12:00:00Z'),
        endDate: new Date('2025-12-12T12:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '7',
        startDate: new Date('2025-12-12T13:00:00Z'),
        endDate: new Date('2025-12-12T13:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '8',
        startDate: new Date('2025-12-12T14:00:00Z'),
        endDate: new Date('2025-12-12T14:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '9',
        startDate: new Date('2025-12-12T15:00:00Z'),
        endDate: new Date('2025-12-12T15:30:00Z'),
        bookedBy: null,
      },
      {
        slotId: '10',
        startDate: new Date('2025-12-12T16:00:00Z'),
        endDate: new Date('2025-12-12T16:30:00Z'),
        bookedBy: null,
      },
    ],
  }

  const [searchIntervieweeKey, setSearchIntervieweeKey] = useState('')

  const [carouselSlide, setCarouselSlide] = useState(0)

  const rowAmount = 3

  const [firstSlideIndexForDate, setFirstSlideIndexForDate] = useState<Record<string, number>>({})
  const [totalSlides, setTotalSlides] = useState(0)
  useEffect(() => {
    let slideIndex = 0
    const firstSlideIndexForDateTemp: Record<string, number> = {}
    Object.entries(interviewSlotItems).forEach(([date, slots]) => {
      const chunks = Math.ceil(slots.length / rowAmount)
      firstSlideIndexForDateTemp[date] = slideIndex
      slideIndex += chunks
    })
    setFirstSlideIndexForDate(firstSlideIndexForDateTemp)
    setTotalSlides(slideIndex)
  }, [])

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

  const [state, setState] = useState<string>('ALL')

  const isSmaller = useIsSmallerBreakpoint('md')

  const [slotModalOpen, setSlotModalOpen] = useState(false)

  const getSlideDisplayAmount = () => {
    const isMobile = useIsSmallerBreakpoint('sm')
    const isSmallScreen = useIsSmallerBreakpoint('lg')
    const isMediumScreen = useIsSmallerBreakpoint('xl')

    return isMobile ? 1 : isSmallScreen ? 2 : isMediumScreen ? 3 : 4
  }

  const calendarHeader = () => {
    //TODO: LOGIC DOES NOT WORK YET DUE TO MULTIPLE ROWS PER DATE
    const keys = Object.keys(interviewSlotItems)
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
    const indexOfDate = Object.keys(interviewSlotItems).indexOf(date)
    if (indexOfDate <= 0) return false

    const previousDate = Object.keys(interviewSlotItems)[indexOfDate - 1]
    const currentDateObj = new Date(date)
    const previousDateObj = new Date(previousDate)
    return (
      currentDateObj.getMonth() !== previousDateObj.getMonth() ||
      currentDateObj.getFullYear() !== previousDateObj.getFullYear()
    )
  }

  return (
    <Stack h={'100%'} gap={'2rem'}>
      <Stack gap={'0.5rem'}>
        <Title>Interview Management</Title>
        <Title order={5} c={'dimmed'}>
          Integrating Gender Sensitivity and Adaptive Learning in Education Games
        </Title>
      </Stack>

      <Stack gap={'0.25rem'}>
        <Group justify='space-between' align='center' gap={'0.5rem'}>
          <Title order={3}>{calendarHeader()}</Title>
          <Group gap={'0.5rem'}>
            <Button variant='outline' size='xs' leftSection={<CalendarDotsIcon size={16} />}>
              {isSmaller ? 'Subscribe' : 'Subscribe to Calendar'}
            </Button>
            <Button
              size='xs'
              leftSection={<PlusIcon size={16} />}
              onClick={() => setSlotModalOpen(true)}
            >
              {isSmaller ? 'Add' : 'Add Slot'}
            </Button>
          </Group>
        </Group>
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
          {Object.entries(interviewSlotItems).map(([date, slots]) => {
            const chunks: IIntervieweeSlot[][] = []
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
                      {chunk.map((slot) => (
                        <SlotItem
                          slot={slot}
                          key={slot.slotId}
                          withInterviewee
                          disabled={dateRowDisabled(date, chunkIndex)}
                          hoverEffect={false}
                        />
                      ))}
                    </Stack>
                  </Carousel.Slide>
                ))}
              </>
            )
          })}
        </Carousel>
      </Stack>

      <Stack gap={'1.5rem'}>
        <Group justify='space-between' align='center' gap={'0.5rem'}>
          <Title order={2}>Interviewees</Title>
          <Group gap={'0.5rem'}>
            <Button variant='outline' size='xs' leftSection={<PaperPlaneTiltIcon size={16} />}>
              {isSmaller ? 'Invites' : 'Send Invites'}
            </Button>
            <Button size='xs' leftSection={<PlusIcon size={16} />}>
              {isSmaller ? 'Add' : 'Add Interviewee'}
            </Button>
          </Group>
        </Group>

        <Group justify='space-between' align='center'>
          <SegmentedControl
            value={state}
            onChange={(value) => setState(value)}
            data={[
              { value: 'ALL', label: 'All' },
              ...Object.entries(InterviewState).map(([value, label]) => ({ value, label })),
            ]}
            radius={'md'}
          />
          <Group>
            <TextInput
              placeholder='Search name...'
              leftSection={<MagnifyingGlassIcon size={16} />}
              value={searchIntervieweeKey}
              onChange={(x) => setSearchIntervieweeKey(x.target.value || '')}
              w={300}
            />
          </Group>
        </Group>
        <Stack></Stack>
      </Stack>

      <AddSlotsModal slotModalOpen={slotModalOpen} setSlotModalOpen={setSlotModalOpen} />
    </Stack>
  )
}
export default InterviewTopicOverviewPage
