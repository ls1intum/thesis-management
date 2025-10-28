import { Grid, Stack, Title } from '@mantine/core'
import { IIntervieweeSlot } from '../../requests/responses/interview'
import { DateHeaderItem } from './components/DateHeaderItem'
import SlotItem from './components/SlotItem'
import { Carousel } from '@mantine/carousel'
import { useState } from 'react'

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
    '2025-11-12': [
      {
        slotId: '4',
        startDate: new Date('2025-11-12T10:00:00Z'),
        endDate: new Date('2025-11-12T10:30:00Z'),
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
        startDate: new Date('2025-11-12T11:00:00Z'),
        endDate: new Date('2025-11-12T11:30:00Z'),
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
      {
        slotId: '10',
        startDate: new Date('2025-11-12T16:00:00Z'),
        endDate: new Date('2025-11-12T16:30:00Z'),
        bookedBy: null,
      },
    ],
  }

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
    <Stack h={'100%'} gap={'1.5rem'}>
      <Stack gap={'0.5rem'}>
        <Title>Interview Management</Title>
        <Title order={4} c={'dimmed'}>
          Integrating Gender Sensitivity and Adaptive Learning in Education Games
        </Title>
      </Stack>

      <Carousel
        slideGap='sm'
        controlsOffset={'-100px'}
        controlSize={32}
        withControls
        withIndicators={false}
        slideSize={'23%'}
        emblaOptions={{ align: 'start', slidesToScroll: 4 }}
        onSlideChange={(index) => setCarouselSlide(index)}
      >
        {Object.entries(interviewSlotItems).map(([date, slots]) => {
          const rowAmount = 3
          const chunks: IIntervieweeSlot[][] = []
          for (let i = 0; i < slots.length; i += rowAmount) {
            chunks.push(slots.slice(i, i + rowAmount))
          }

          return (
            <>
              {chunks.map((chunk, chunkIndex) => (
                <Carousel.Slide key={`${date}-${chunkIndex}`}>
                  <Stack gap={'0.25rem'}>
                    {chunkIndex === 0 ? (
                      <DateHeaderItem date={date} h={50} disabled={dateRowDisabled(4, date)} />
                    ) : (
                      <Stack h={50} />
                    )}
                    {chunk.map((slot) => (
                      <SlotItem slot={slot} key={slot.slotId} withInterviewee />
                    ))}
                  </Stack>
                </Carousel.Slide>
              ))}
            </>
          )
        })}
      </Carousel>
    </Stack>
  )
}
export default InterviewTopicOverviewPage
