import * as classes from './GanttChart.module.css'
import { arrayUnique } from '../../utils/array'
import { ReactNode, TouchEvent, Touch, WheelEvent, useMemo, useState, useEffect } from 'react'
import { Button, Collapse, Popover, RangeSlider } from '@mantine/core'
import { formatDate } from '../../utils/format'
import { CaretDown, CaretUp } from 'phosphor-react'

interface IGanttChartProps {
  columns: string[]
  data: Array<IGanttChartDataElement> | undefined
  itemPopover: (item: IGanttChartDataElement) => ReactNode
  onItemClick?: (item: IGanttChartDataElement) => unknown
  defaultRange?: number
  maxTicks?: number
}

export interface IGanttChartDataElement {
  id: string
  groupId: string
  groupName: string
  columns: string[]
  timeline: Array<{
    startDate: Date
    endDate: Date
    color: string
  }>
  events: Array<{
    icon: ReactNode
    time: Date
  }>
}

type DateRange = [number, number]

const isInDateRange = (range: DateRange, visibleRange: DateRange) => {
  const [rangeStart, rangeEnd] = range
  const [visibleStart, visibleEnd] = visibleRange

  return rangeEnd >= visibleStart && rangeStart <= visibleEnd
}

const GanttChart = (props: IGanttChartProps) => {
  const {
    columns,
    data,
    itemPopover,
    onItemClick,
    defaultRange = 3600 * 24 * 30 * 3 * 1000,
    maxTicks = 6,
  } = props

  const [range, setRange] = useState<DateRange>()
  const [collapsedGroups, setCollapsedGroups] = useState<string[]>([])
  const [popover, setPopover] = useState<string>()
  const [initialTouchDistance, setInitialTouchDistance] = useState<number>()

  const currentTime = useMemo(() => Date.now(), [])

  // Disable page zooming to prevent that page zooms on pinch gesture
  useEffect(() => {
    const handleWheelGlobal = (e: globalThis.WheelEvent) => {
      if (e.ctrlKey) {
        e.preventDefault()
      }
    }

    window.addEventListener('wheel', handleWheelGlobal, { passive: false })

    return () => {
      window.removeEventListener('wheel', handleWheelGlobal)
    }
  }, [])

  if (!data || data.length === 0) {
    return null
  }

  // Calculate total range based on the provided data
  const totalRange: DateRange = [
    Math.min(
      ...data.map((item) =>
        Math.min(
          ...item.timeline.map((timelineItem) => timelineItem.startDate.getTime()),
          ...item.events.map((timelineEvent) => timelineEvent.time.getTime()),
        ),
      ),
    ),
    Math.max(
      ...data.map((item) =>
        Math.max(
          ...item.timeline.map((timelineItem) => timelineItem.endDate.getTime()),
          ...item.events.map((timelineEvent) => timelineEvent.time.getTime()),
        ),
      ),
    ),
  ]

  // Default filtered range to currentTime if there are elements larger than current Time
  const initialEndTime = Math.min(currentTime, totalRange[1])
  const filteredRange: DateRange = [
    Math.max(totalRange[0], range?.[0] ?? initialEndTime - defaultRange),
    Math.min(totalRange[1], range?.[1] ?? initialEndTime),
  ]
  const filteredRangeDuration = filteredRange[1] - filteredRange[0]

  const groups: Array<{ groupId: string; groupName: string }> = arrayUnique(
    data.map((row) => ({
      groupId: row.groupId,
      groupName: row.groupName,
    })),
    (a, b) => a.groupId === b.groupId,
  )

  // Touch events for pinch to zoom
  const zoomRange = (zoomFactor: number) => {
    const center = (filteredRange[0] + filteredRange[1]) / 2

    setRange([
      Math.max(center - (center - filteredRange[0]) * zoomFactor, totalRange[0]),
      Math.min(center + (filteredRange[1] - center) * zoomFactor, totalRange[1]),
    ])
  }

  const getTouchDistance = (touch1: Touch, touch2: Touch): number => {
    return Math.sqrt(
      Math.pow(touch2.clientX - touch1.clientX, 2) + Math.pow(touch2.clientY - touch1.clientY, 2),
    )
  }

  const handleTouchStart = (e: TouchEvent<HTMLDivElement>) => {
    if (e.touches.length === 2) {
      setInitialTouchDistance(getTouchDistance(e.touches[0], e.touches[1]))
    }
  }

  const handleTouchMove = (e: TouchEvent<HTMLDivElement>) => {
    if (e.touches.length === 2 && initialTouchDistance) {
      const currentDistance = getTouchDistance(e.touches[0], e.touches[1])

      zoomRange(currentDistance / initialTouchDistance)
    }
  }

  const handleWheel = (e: WheelEvent<HTMLDivElement>) => {
    if (e.ctrlKey) {
      e.preventDefault()

      zoomRange(e.deltaY < 0 ? 1.05 : 0.95)
    }
  }

  const generateTicks = () => {
    const timeOffset = 0.05 * filteredRangeDuration
    const startDate = new Date(filteredRange[0] + timeOffset)
    const endDate = new Date(filteredRange[1] - timeOffset)

    let calculatedTicks: Array<{ label: string; type: string; value: number }> = []

    // Add Now tick if currentTime is in filtered range
    if (currentTime >= startDate.getTime() && currentTime <= endDate.getTime()) {
      calculatedTicks.push({
        type: 'now',
        value: currentTime,
        label: 'Now',
      })
    }

    let lastYear = startDate.getFullYear()
    let lastMonth = startDate.getMonth()

    for (let time = startDate.getTime(); time <= endDate.getTime(); time += 3600 * 24 * 1000) {
      const iterationDate = new Date(time)

      // Don't add tick if it's close to the "Now" tick
      if (Math.abs(time - currentTime) <= 0.1 * filteredRangeDuration) {
        continue
      }

      if (iterationDate.getFullYear() !== lastYear) {
        calculatedTicks.push({
          label: `${iterationDate.toLocaleString('default', { year: 'numeric' })}`,
          type: 'year',
          value: time,
        })
      } else if (iterationDate.getMonth() !== lastMonth) {
        calculatedTicks.push({
          label: `${iterationDate.toLocaleString('default', { month: 'long' })}`,
          type: 'month',
          value: time,
        })
      } else {
        calculatedTicks.push({
          label: `${iterationDate.toLocaleString('default', { day: '2-digit' })}.`,
          type: 'day',
          value: time,
        })
      }

      lastYear = iterationDate.getFullYear()
      lastMonth = iterationDate.getMonth()
    }

    const priorityTicks = ['now', 'year', 'month']

    // clear non priority items if there are too many ticks and ticks include priority items
    if (
      calculatedTicks.length > maxTicks &&
      calculatedTicks.some((row) => priorityTicks.includes(row.type))
    ) {
      calculatedTicks = calculatedTicks.filter((row) => priorityTicks.includes(row.type))
    }

    // reduce ticks until smaller than maxTicks
    let lastTickCount = calculatedTicks.length
    while (calculatedTicks.length > maxTicks) {
      calculatedTicks = calculatedTicks.filter(
        (row, index) => priorityTicks.includes(row.type) || index % 2 === 0,
      )

      if (calculatedTicks.length === lastTickCount) {
        calculatedTicks = calculatedTicks.filter((row) => priorityTicks.includes(row.type))

        priorityTicks.pop()
      }

      lastTickCount = calculatedTicks.length
    }

    return calculatedTicks
  }

  const ticks = generateTicks()

  return (
    <div className={classes.chartContainer}>
      <div className={classes.chartBox}>
        <RangeSlider
          min={totalRange[0]}
          max={totalRange[1]}
          step={3600 * 24 * 1000}
          value={filteredRange}
          onChange={setRange}
          label={(value) => formatDate(new Date(value), { withTime: false })}
          mb='md'
        />
        <div className={classes.headers}>
          {columns.map((column) => (
            <div key={column} className={classes.dataHeader}>
              {column}
            </div>
          ))}
          <div className={classes.timelineHeader}>
            {ticks.map((tick) => (
              <div
                key={tick.value}
                className={classes.timelineTick}
                style={{
                  left: `${(100 * (tick.value - filteredRange[0])) / filteredRangeDuration}%`,
                }}
              >
                {tick.label}
              </div>
            ))}
          </div>
        </div>
        <div
          className={classes.content}
          onTouchStart={handleTouchStart}
          onTouchMove={handleTouchMove}
          onWheel={handleWheel}
          style={{ touchAction: 'none' }}
        >
          {groups.map((group) => (
            <div key={group.groupId} className={classes.groupRow}>
              <div>
                <Button
                  variant='transparent'
                  px={0}
                  onClick={() =>
                    setCollapsedGroups((prev) =>
                      prev.includes(group.groupId)
                        ? [...prev.filter((x) => x !== group.groupId)]
                        : [...prev, group.groupId],
                    )
                  }
                  rightSection={
                    collapsedGroups.includes(group.groupId) ? <CaretDown /> : <CaretUp />
                  }
                >
                  {group.groupName}
                </Button>
              </div>
              <div className={classes.groupContent}>
                <Collapse in={!collapsedGroups.includes(group.groupId)}>
                  {data
                    .filter((row) => row.groupId === group.groupId)
                    .map((item) => (
                      <Popover
                        key={item.id}
                        opened={popover === `${item.groupId}-${item.id}`}
                        withArrow
                        shadow='md'
                      >
                        <Popover.Target>
                          <div
                            className={classes.groupContentRow}
                            style={{
                              cursor: onItemClick ? 'pointer' : undefined,
                            }}
                            onMouseEnter={() => setPopover(`${item.groupId}-${item.id}`)}
                            onMouseLeave={() => setPopover(undefined)}
                            onClick={() => onItemClick?.(item)}
                          >
                            {columns.map((column, index) => (
                              <div
                                key={column}
                                className={classes.dataColumn}
                                title={item.columns[index]}
                              >
                                {item.columns[index]}
                              </div>
                            ))}
                            <div className={classes.timelineColumn}>
                              {item.timeline
                                .filter((timelineItem) =>
                                  isInDateRange(
                                    [
                                      timelineItem.startDate.getTime(),
                                      timelineItem.endDate.getTime(),
                                    ],
                                    filteredRange,
                                  ),
                                )
                                .map((timelineItem) => (
                                  <div
                                    key={timelineItem.startDate.getTime()}
                                    className={classes.timelinePart}
                                    style={{
                                      left: `${Math.max(
                                        (100 *
                                          (timelineItem.startDate.getTime() - filteredRange[0])) /
                                          filteredRangeDuration,
                                        0,
                                      )}%`,
                                      width: `${
                                        (100 *
                                          (Math.min(
                                            timelineItem.endDate.getTime(),
                                            filteredRange[1],
                                          ) -
                                            Math.max(
                                              timelineItem.startDate.getTime(),
                                              filteredRange[0],
                                            ))) /
                                        filteredRangeDuration
                                      }%`,
                                      backgroundColor: timelineItem.color,
                                    }}
                                  />
                                ))}
                              {item.events
                                .filter((timelineEvent) =>
                                  isInDateRange(
                                    [timelineEvent.time.getTime(), timelineEvent.time.getTime()],
                                    filteredRange,
                                  ),
                                )
                                .map((timelineEvent) => (
                                  <div
                                    key={timelineEvent.time.getTime()}
                                    className={classes.timelineEvent}
                                    style={{
                                      left: `${Math.max(
                                        (100 * (timelineEvent.time.getTime() - filteredRange[0])) /
                                          filteredRangeDuration,
                                        0,
                                      )}%`,
                                    }}
                                  >
                                    {timelineEvent.icon}
                                  </div>
                                ))}
                            </div>
                          </div>
                        </Popover.Target>
                        <Popover.Dropdown style={{ pointerEvents: 'none' }}>
                          {itemPopover(item)}
                        </Popover.Dropdown>
                      </Popover>
                    ))}
                </Collapse>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default GanttChart
