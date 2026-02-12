import { Carousel } from '@mantine/carousel'
import { Center, Loader, ScrollArea, Stack } from '@mantine/core'
import { DateHeaderItem } from '../../InterviewTopicOverviewPage/components/DateHeaderItem'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'
import { useInterviewProcessContext } from '../../../providers/InterviewProcessProvider/hooks'
import SlotItem from '../../InterviewTopicOverviewPage/components/SlotItem'
import { useState } from 'react'
import { IInterviewSlot } from '../../../requests/responses/interview'

interface ISelectSlotCarouselProps {
  selectedSlot: IInterviewSlot | null
  setSelectedSlot: (slot: IInterviewSlot) => void
}

const SelectSlotCarousel = ({ selectedSlot, setSelectedSlot }: ISelectSlotCarouselProps) => {
  const isSmaller = useIsSmallerBreakpoint('sm')

  const isMobile = useIsSmallerBreakpoint('sm')
  const isSmallScreen = useIsSmallerBreakpoint('lg')
  const isMediumScreen = useIsSmallerBreakpoint('xl')

  const { interviewSlots, interviewSlotsLoading } = useInterviewProcessContext()

  const [carouselSlide, setCarouselSlide] = useState(0)

  const getSlideDisplayAmount = () => {
    const slideAmount = isMobile ? 1 : isSmallScreen ? 2 : isMediumScreen ? 3 : 4
    return Math.min(slideAmount, Object.keys(interviewSlots).length)
  }

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

  if (interviewSlotsLoading) {
    return (
      <Center h={'100%'}>
        <Loader />
      </Center>
    )
  }

  return (
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
        {Object.entries(interviewSlots)
          .sort(([dateA], [dateB]) => new Date(dateA).getTime() - new Date(dateB).getTime())
          .map(([date, slots]) => (
            <Carousel.Slide key={date}>
              <Stack gap={'1.5rem'}>
                <DateHeaderItem date={date} size={'lg'} disabled={dateRowDisabled(4, date)} />
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
                </Stack>
              </Stack>
            </Carousel.Slide>
          ))}
      </Carousel>
    </ScrollArea>
  )
}

export default SelectSlotCarousel
