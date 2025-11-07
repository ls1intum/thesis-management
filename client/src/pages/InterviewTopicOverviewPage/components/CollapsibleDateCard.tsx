import { Accordion, Button, Card, Divider, Group, Stack, Text, Title } from '@mantine/core'
import dayjs from 'dayjs'
import { IIntervieweeSlot } from '../../../requests/responses/interview'
import { useState } from 'react'
import { CardsIcon } from '@phosphor-icons/react'
import { TimeInput } from '@mantine/dates'
import SlotItem from './SlotItem'
import DeleteButton from '../../../components/DeleteButton/DeleteButton'

interface ICollapsibleDateCardProps {
  date: Date
  duration?: number
}

interface ISlotRange {
  startTime: Date | null
  endTime: Date | null
  type: 'single' | 'range'
  slots?: IIntervieweeSlot[]
}

const CollapsibleDateCard = ({ date, duration = 30 }: ICollapsibleDateCardProps) => {
  const [slotRanges, setSlotRanges] = useState<ISlotRange[]>([])

  const createSlotsForRange = (
    startTime: Date,
    endTime: Date,
    duration: number,
  ): IIntervieweeSlot[] => {
    const slots: IIntervieweeSlot[] = []

    let currentTime = new Date(startTime)
    let slotEnd = new Date(currentTime)
    slotEnd.setMinutes(slotEnd.getMinutes() + duration)

    while (slotEnd <= endTime) {
      slots.push({
        slotId: `slot-${startTime.toDateString()}-${currentTime.toTimeString()}`,
        startDate: new Date(currentTime),
        endDate: new Date(slotEnd),
        bookedBy: null,
      })

      currentTime.setMinutes(currentTime.getMinutes() + duration)
      slotEnd.setMinutes(slotEnd.getMinutes() + duration)
    }

    return slots
  }

  const handleDeleteSlotRange = (index: number) => {
    setSlotRanges((prev) => prev.filter((_, i) => i !== index))
  }

  return (
    <Card withBorder radius='md' my='sm' p={'0.25rem'} style={{ cursor: 'pointer' }}>
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
              <Stack justify='flex-start' align={'flex-start'}>
                {slotRanges.map((slotRange, index) => (
                  <Stack key={`slot-range-${date}-${index}`} w={'100%'} gap={'0.75rem'}>
                    <Group justify='space-between' align='center'>
                      <Text fw={500} size={'xs'} c={'dimmed'}>
                        {slotRange.type === 'range' ? 'Range' : 'Single'} Slot
                      </Text>

                      <DeleteButton
                        onClick={() => handleDeleteSlotRange(index)}
                        iconSize={16}
                        buttonSize={20}
                      ></DeleteButton>
                    </Group>
                    <Group wrap='nowrap' key={`slot-range-${date}-${index}`} h={'100%'} w={'100%'}>
                      <Divider
                        orientation='vertical'
                        size={'lg'}
                        color={slotRange.type === 'range' ? 'blue' : 'green'}
                      />

                      <Stack w={'100%'}>
                        <Group>
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
                                if (slotRange.type === 'single') {
                                  newRanges[index].endTime = (() => {
                                    let newEndTime = new Date(date)
                                    const [hours, minutes] = newTime.target.value
                                      .split(':')
                                      .map(Number)
                                    newEndTime.setHours(hours, minutes, 0, 0)
                                    newEndTime.setMinutes(newEndTime.getMinutes() + duration)

                                    return newEndTime
                                  })()
                                }

                                if (newRanges[index].endTime && newRanges[index].startTime) {
                                  if (slotRange.type === 'range') {
                                    //Generate slots between start and end time every
                                    newRanges[index].slots = createSlotsForRange(
                                      newRanges[index].startTime,
                                      newRanges[index].endTime,
                                      duration,
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

                                newRanges.sort((a, b) => {
                                  if (a.startTime && b.startTime) {
                                    return a.startTime.getTime() - b.startTime.getTime()
                                  }
                                  return 0
                                })
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
                                  const [hours, minutes] = newTime.target.value
                                    .split(':')
                                    .map(Number)
                                  newEndTime.setHours(hours, minutes, 0, 0)

                                  newRanges[index].endTime = newEndTime

                                  if (newRanges[index].endTime && newRanges[index].startTime) {
                                    //Generate slots between start and end time
                                    newRanges[index].slots = createSlotsForRange(
                                      newRanges[index].startTime,
                                      newRanges[index].endTime,
                                      duration,
                                    )
                                  }
                                  return newRanges
                                })
                              }}
                            />
                          )}
                        </Group>
                        {slotRange.slots && slotRange.slots.length > 0 ? (
                          <Stack w={'100%'} pt={6} gap={'0.5rem'}>
                            {slotRange.slots.map((slot) => (
                              <SlotItem key={slot.slotId} slot={slot} hoverEffect={false} />
                            ))}
                          </Stack>
                        ) : slotRange.startTime && slotRange.endTime ? (
                          <Text c='red' size={'sm'}>
                            StartTime must be before EndTime
                          </Text>
                        ) : (
                          <Text c='dimmed' size={'sm'}>
                            {slotRange.type === 'range'
                              ? 'Add a start and end time to see slots in this range'
                              : 'Add a start time to create this slot'}
                          </Text>
                        )}
                      </Stack>
                    </Group>
                  </Stack>
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
