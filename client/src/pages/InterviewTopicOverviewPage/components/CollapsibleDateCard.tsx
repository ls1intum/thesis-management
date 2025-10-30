import { Accordion, Button, Card, Divider, Group, Stack, Text, Title } from '@mantine/core'
import { useHover } from '@mantine/hooks'
import dayjs from 'dayjs'
import { IIntervieweeSlot } from '../../../requests/responses/interview'
import { useState } from 'react'
import { CardsIcon } from '@phosphor-icons/react'
import { TimeInput } from '@mantine/dates'
import { n } from 'react-router/dist/development/index-react-server-client-BKpa2trA'
import SlotItem from './SlotItem'

interface ICollapsibleDateCardProps {
  date: Date
}

interface ISlotRange {
  startTime: Date | null
  endTime: Date | null
  type: 'single' | 'range'
  slots?: IIntervieweeSlot[]
}

const CollapsibleDateCard = ({ date }: ICollapsibleDateCardProps) => {
  const { ref, hovered } = useHover()

  const [slotRanges, setSlotRanges] = useState<ISlotRange[]>([])

  const createSlotsForRange = (
    startTime: Date,
    endTime: Date,
    duration: number,
  ): IIntervieweeSlot[] => {
    const slots: IIntervieweeSlot[] = []

    let currentTime = new Date(startTime)

    while (currentTime <= endTime) {
      const slotStart = new Date(currentTime)
      const slotEnd = new Date(currentTime)
      slotEnd.setMinutes(slotEnd.getMinutes() + duration)

      slots.push({
        slotId: `slot-${startTime.toDateString()}-${slotStart.toTimeString()}`,
        startDate: slotStart,
        endDate: slotEnd,
        bookedBy: null,
      })

      currentTime.setMinutes(currentTime.getMinutes() + duration)
    }
    return slots
  }

  return (
    <Card withBorder radius='md' my='sm' p={'0.25rem'} style={{ cursor: 'pointer' }} ref={ref}>
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
                  setSlotRanges((prev) => [
                    ...prev,
                    { startTime: null, endTime: null, type: 'range', slots: [] },
                  ])
                }}
                color='blue'
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
                  setSlotRanges((prev) => [
                    ...prev,
                    { startTime: null, endTime: null, type: 'single', slots: [] },
                  ])
                }}
                color='green'
              >
                Add slot
              </Button>
            </Group>
          </Group>
        </Accordion.Control>
        <Accordion.Panel>
          <Stack>
            <Divider />
            {slotRanges.length === 0 ? (
              <Stack justify='center' align='center' h={'100%'} gap={'0.25rem'}>
                <CardsIcon size={30} />
                <Stack gap={0} justify='center' align='center'>
                  <Title order={6}>No Slots yet</Title>
                  <Text c='dimmed' size={'sm'}>
                    Add a range or a single slot to get started
                  </Text>
                </Stack>
              </Stack>
            ) : (
              <Stack>
                {slotRanges.map((slotRange, index) => (
                  <Group wrap='nowrap' key={`slot-range-${date}-${index}`} h={'100%'}>
                    <Group gap={'0.5rem'} wrap='nowrap' h={'100%'}>
                      <Divider
                        orientation='vertical'
                        size={'lg'}
                        color={slotRange.type === 'range' ? 'blue' : 'green'}
                      />
                      <Stack justify='space-between'>
                        <TimeInput
                          value={slotRange.startTime?.toTimeString().slice(0, 5) ?? ''}
                          size='xs'
                          description='Start time'
                          onChange={(newTime) => {
                            setSlotRanges((prev) => {
                              const newRanges = [...prev]

                              let newStartTime = new Date(date)
                              const [hours, minutes] = newTime.target.value.split(':').map(Number)
                              newStartTime.setHours(hours, minutes, 0, 0)
                              newRanges[index].startTime = newStartTime

                              //When it is single also set end time
                              //TODO: USE ACTUALL DURATION
                              if (slotRange.type === 'single') {
                                newRanges[index].endTime = (() => {
                                  let newEndTime = new Date(date)
                                  const [hours, minutes] = newTime.target.value
                                    .split(':')
                                    .map(Number)
                                  newEndTime.setHours(hours, minutes, 0, 0)
                                  newEndTime.setMinutes(newEndTime.getMinutes() + 30)

                                  return newEndTime
                                })()
                              }

                              if (newRanges[index].endTime && newRanges[index].startTime) {
                                if (slotRange.type === 'range') {
                                  //Generate slots between start and end time every 30 minutes
                                  //TODO: USE ACTUAL DURATION
                                  newRanges[index].slots = createSlotsForRange(
                                    newRanges[index].startTime,
                                    newRanges[index].endTime,
                                    30,
                                  )
                                } else {
                                  newRanges[index].slots = [
                                    {
                                      slotId: `slot-${date.toDateString()}-${newRanges[index].startTime}`,
                                      startDate: new Date(newRanges[index].startTime),
                                      endDate: new Date(newRanges[index].endTime),
                                      bookedBy: null,
                                    },
                                  ]
                                }
                              }

                              return newRanges
                            })
                          }}
                        />
                        {slotRange.type === 'range' && (
                          <TimeInput
                            value={slotRange.endTime?.toTimeString().slice(0, 5) ?? ''}
                            size='xs'
                            description='End time'
                            onChange={(newTime) => {
                              setSlotRanges((prev) => {
                                const newRanges = [...prev]
                                let newEndTime = new Date(date)
                                const [hours, minutes] = newTime.target.value.split(':').map(Number)
                                newEndTime.setHours(hours, minutes, 0, 0)

                                newRanges[index].endTime = newEndTime

                                if (newRanges[index].endTime && newRanges[index].startTime) {
                                  //Generate slots between start and end time every 30 minutes
                                  //TODO: USE ACTUAL DURATION
                                  newRanges[index].slots = createSlotsForRange(
                                    newRanges[index].startTime,
                                    newRanges[index].endTime,
                                    30,
                                  )
                                }
                                return newRanges
                              })
                            }}
                          />
                        )}
                      </Stack>
                    </Group>

                    {slotRange.slots && slotRange.slots.length > 0 ? (
                      <Stack w={'100%'} pt={6}>
                        {slotRange.slots.map((slot) => (
                          <SlotItem key={slot.slotId} slot={slot} hoverEffect={false} />
                        ))}
                      </Stack>
                    ) : (
                      <Text c='dimmed' size={'sm'}>
                        {slotRange.type === 'range'
                          ? 'Add a start and end time to see slots in this range'
                          : 'Add a start time to create this slot'}
                      </Text>
                    )}
                  </Group>
                ))}
              </Stack>
            )}
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Card>
  )
}

export default CollapsibleDateCard
