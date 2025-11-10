import { Accordion, Button, Card, Divider, Group, Stack, Text, Title } from '@mantine/core'
import dayjs from 'dayjs'
import { IIntervieweeSlot } from '../../../requests/responses/interview'
import { useEffect, useState } from 'react'
import { CardsIcon } from '@phosphor-icons/react'
import { TimeInput } from '@mantine/dates'
import SlotItem from './SlotItem'
import DeleteButton from '../../../components/DeleteButton/DeleteButton'

interface ICollapsibleDateCardProps {
  date: Date
  duration?: number
  slots?: IIntervieweeSlot[]
}

interface ISlotRange {
  startTime: Date | null
  endTime: Date | null
  type: 'single' | 'range' | 'scheduled'
  duration: number
  slots?: IIntervieweeSlot[]
}

const CollapsibleDateCard = ({ date, duration = 30, slots }: ICollapsibleDateCardProps) => {
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

  //These next functions build slot ranges from existing slots, we do this in the client, because saving it to the server does not make sense since it is extra overhead to manage and changes frequently when slots are scheduled
  // For the user it is only important to be able to change them as quickly as possible and the range is just a representation of that

  function buildRangesForUnscheduled(slots: IIntervieweeSlot[]): ISlotRange[] {
    if (!slots.length) return []

    // 1) Sort by start time
    const sorted = [...slots].sort((a, b) => a.startDate.getTime() - b.startDate.getTime())

    const n = sorted.length
    const durations = sorted.map((s) => s.endDate.getTime() - s.startDate.getTime())
    const breaks: number[] = []
    for (let i = 0; i < n - 1; i++) {
      breaks[i] = sorted[i + 1].startDate.getTime() - sorted[i].endDate.getTime()
    }

    type CandidateRange = { start: number; end: number; length: number }
    const candidates: CandidateRange[] = []

    // 2) find all possible contiguous ranges [start, end]
    for (let start = 0; start < n; start++) {
      const baseDuration = durations[start]
      let currentBreak: number | null = null

      for (let end = start + 1; end < n; end++) {
        if (durations[end] !== baseDuration) break

        const b = breaks[end - 1]

        if (currentBreak === null) {
          currentBreak = b
        } else if (b !== currentBreak) {
          break
        }

        const length = end - start + 1
        if (length >= 2) {
          candidates.push({ start, end, length })
        }
      }
    }

    // 3) pick longest non-overlapping ranges
    candidates.sort((a, b) => b.length - a.length)

    const used = new Array(n).fill(false)
    const chosenRanges: CandidateRange[] = []

    for (const cand of candidates) {
      let canUse = true
      for (let i = cand.start; i <= cand.end; i++) {
        if (used[i]) {
          canUse = false
          break
        }
      }
      if (!canUse) continue

      chosenRanges.push(cand)
      for (let i = cand.start; i <= cand.end; i++) {
        used[i] = true
      }
    }

    const ranges: ISlotRange[] = []

    // add multi-slot ranges
    for (const r of chosenRanges) {
      const groupSlots = sorted.slice(r.start, r.end + 1)
      const duration = groupSlots[0].endDate.getTime() - groupSlots[0].startDate.getTime()
      ranges.push({
        startTime: groupSlots[0].startDate,
        endTime: groupSlots[groupSlots.length - 1].endDate,
        type: 'range',
        duration,
        slots: groupSlots,
      })
    }

    // add remaining singles (unscheduled but not part of any chosen range)
    for (let i = 0; i < n; i++) {
      if (!used[i]) {
        const s = sorted[i]
        const duration = s.endDate.getTime() - s.startDate.getTime()
        ranges.push({
          startTime: s.startDate,
          endTime: s.endDate,
          type: 'single',
          duration,
          slots: [s],
        })
      }
    }

    // keep overall chronological order in the result
    ranges.sort((a, b) => (a.startTime?.getTime() ?? 0) - (b.startTime?.getTime() ?? 0))

    return ranges
  }

  function buildSlotRanges(slots: IIntervieweeSlot[]): ISlotRange[] {
    if (!slots.length) return []

    // 0) split into scheduled vs unscheduled
    const scheduledSlots = slots.filter((s) => s.bookedBy != null)
    const unscheduledSlots = slots.filter((s) => s.bookedBy == null)

    // 1) scheduled slots are always singles
    const scheduledRanges: ISlotRange[] = scheduledSlots.map((s) => ({
      startTime: s.startDate,
      endTime: s.endDate,
      type: 'scheduled',
      duration: s.endDate.getTime() - s.startDate.getTime(),
      slots: [s],
    }))

    // 2) compute ranges only on unscheduled
    const unscheduledRanges = buildRangesForUnscheduled(unscheduledSlots)

    // 3) merge & sort by time
    const allRanges = [...scheduledRanges, ...unscheduledRanges].sort(
      (a, b) => (a.startTime?.getTime() ?? 0) - (b.startTime?.getTime() ?? 0),
    )

    return allRanges
  }

  useEffect(() => {
    if (slots && slots.length > 0) {
      setSlotRanges(buildSlotRanges(slots))
    }
  }, [])

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
                    {
                      startTime: null,
                      endTime: null,
                      type: 'range',
                      duration: duration,
                      slots: [],
                    },
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
                    {
                      startTime: null,
                      endTime: null,
                      type: 'single',
                      duration: duration,
                      slots: [],
                    },
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
                        {slotRange.type === 'single'
                          ? 'Single'
                          : slotRange.type === 'scheduled'
                            ? 'Booked'
                            : 'Range'}
                        Slot
                      </Text>

                      <DeleteButton
                        onClick={() => handleDeleteSlotRange(index)}
                        iconSize={16}
                        buttonSize={20}
                        disabled={
                          slotRange.slots &&
                          slotRange.slots.length > 0 &&
                          slotRange.slots[0].bookedBy
                            ? true
                            : false
                        }
                        tooltipText={'Booked slots need to be rescheduled first'}
                        tooltipTextPosition='left'
                        tooltipOnlyWhenDisabled={true}
                      ></DeleteButton>
                    </Group>
                    <Group wrap='nowrap' key={`slot-range-${date}-${index}`} h={'100%'} w={'100%'}>
                      <Divider
                        orientation='vertical'
                        size={'lg'}
                        color={
                          slotRange.type === 'single'
                            ? 'green'
                            : slotRange.type === 'scheduled'
                              ? 'primary'
                              : 'blue'
                        }
                      />

                      <Stack w={'100%'}>
                        {slotRange.type !== 'scheduled' && (
                          <Group>
                            <TimeInput
                              value={slotRange.startTime?.toTimeString().slice(0, 5) ?? ''}
                              size='xs'
                              description='Start time'
                              onChange={(newTime) => {
                                setSlotRanges((prev) => {
                                  const newRanges = [...prev]

                                  let newStartTime = new Date(date)
                                  const [hours, minutes] = newTime.target.value
                                    .split(':')
                                    .map(Number)
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
                        )}
                        {slotRange.slots && slotRange.slots.length > 0 ? (
                          <Stack w={'100%'} pt={6} gap={'0.5rem'}>
                            {slotRange.slots.map((slot) => (
                              <SlotItem
                                key={slot.slotId}
                                slot={slot}
                                hoverEffect={false}
                                withInterviewee={
                                  slotRange.slots &&
                                  slotRange.slots.length > 0 &&
                                  slotRange.slots[0].bookedBy
                                    ? true
                                    : false
                                }
                              />
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
