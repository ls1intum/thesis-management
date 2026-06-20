import { Center, Checkbox, Grid, Group, SegmentedControl, Stack } from '@mantine/core'
import { GLOBAL_CONFIG } from '@/core/config/global'
import { useTopicsContext } from '@/core/topic/providers/TopicsProvider/hooks'
import { TopicState } from '@/core/topic/requests/responses/topic'
import { formatThesisType, formatTopicState } from '@/core/utils/format'

interface ITopicsFiltersProps {
  visible: Array<'type' | 'states'>
}

const TopicsFilters = (props: ITopicsFiltersProps) => {
  const { visible } = props

  const { filters, setFilters } = useTopicsContext()

  return (
    <Stack>
      {visible.includes('states') && (
        <Group>
          <SegmentedControl
            data={Object.values(TopicState).map((state) => ({
              label: formatTopicState(state),
              value: state,
            }))}
            onChange={(e) => {
              setFilters((prev) => ({
                ...prev,
                states: [e],
              }))
            }}
            size='sm'
            transitionDuration={300}
            transitionTimingFunction='linear'
          />
        </Group>
      )}
      {visible.includes('type') && (
        <Grid grow>
          {Object.keys(GLOBAL_CONFIG.thesis_types).map((key) => (
            <Grid.Col key={key} span={{ md: 3 }}>
              <Center>
                <Checkbox
                  label={formatThesisType(key)}
                  checked={Boolean(filters.types?.includes(key))}
                  onChange={(e) => {
                    setFilters((prev) => ({
                      types: [...(prev.types ?? []), key].filter(
                        (row) => e.target.checked || row !== key,
                      ),
                    }))
                  }}
                />
              </Center>
            </Grid.Col>
          ))}
        </Grid>
      )}
    </Stack>
  )
}

export default TopicsFilters
