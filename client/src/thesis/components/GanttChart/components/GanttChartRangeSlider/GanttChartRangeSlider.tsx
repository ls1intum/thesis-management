import { Group, RangeSlider, Text } from '@mantine/core'
import { formatDate } from '@/core/utils/format'
import { useGanttChartContext } from '@/thesis/components/GanttChart/context'

const GanttChartRangeSlider = () => {
  const { totalRange, filteredRange, setRange } = useGanttChartContext()

  return (
    <Group mb='md' mt={30}>
      <Text fw='bold'>Timeline Range Filter:</Text>
      <RangeSlider
        style={{ flex: 1 }}
        min={totalRange[0]}
        max={totalRange[1]}
        step={3600 * 24 * 1000}
        value={filteredRange}
        onChange={setRange}
        label={(value) => formatDate(new Date(value), { withTime: false })}
      />
    </Group>
  )
}

export default GanttChartRangeSlider
