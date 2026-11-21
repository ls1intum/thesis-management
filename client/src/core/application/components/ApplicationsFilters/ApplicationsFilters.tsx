import { Grid, MultiSelect, Select, Stack, Switch, TextInput } from '@mantine/core'
import { MagnifyingGlass } from '@phosphor-icons/react'
import { ApplicationState } from '@/core/application/requests/responses/application'
import { useApplicationsContext } from '@/core/application/providers/ApplicationsProvider/hooks'
import React from 'react'
import { formatApplicationState, formatThesisType } from '@/core/utils/format'
import { GLOBAL_CONFIG } from '@/core/config/global'

interface IApplicationsFiltersProps {
  size?: 'xl' | 'sm'
}

const ApplicationsFilters = (props: IApplicationsFiltersProps) => {
  const { size = 'xl' } = props

  const { topics, filters, setFilters, sort, setSort } = useApplicationsContext()

  return (
    <Grid gap='sm'>
      <Grid.Col span={12}>
        <TextInput
          placeholder='Search applications...'
          leftSection={<MagnifyingGlass size={16} />}
          value={filters.search ?? ''}
          onChange={(e) => {
            // Capture value synchronously — currentTarget is null once the
            // browser releases the event after the handler returns, and the
            // setState updater below runs in a later render tick.
            const search = e.currentTarget.value
            setFilters((prev) => ({ ...prev, search }))
          }}
        />
      </Grid.Col>
      <Grid.Col span={size === 'sm' ? 12 : 6}>
        <Stack gap='xs'>
          <MultiSelect
            hidePickedOptions
            label='Topic'
            placeholder='Open Topics'
            data={
              topics
                ? Object.values(topics).map((topic) => ({
                    value: topic.topicId,
                    label: topic.title,
                  }))
                : []
            }
            value={filters.topics ?? []}
            onChange={(value) => {
              setFilters((prev) => ({
                ...prev,
                topics: value,
              }))
            }}
            searchable
          />
          <Switch
            label='Include suggested topics'
            description='Show applications without a specific topic (the student proposed their own thesis title)'
            checked={filters.includeSuggestedTopics !== false}
            onChange={(e) => {
              const includeSuggestedTopics = e.currentTarget.checked
              setFilters((prev) => ({
                ...prev,
                includeSuggestedTopics,
              }))
            }}
          />
        </Stack>
      </Grid.Col>
      <Grid.Col span={size === 'sm' ? 12 : 6}>
        <MultiSelect
          hidePickedOptions
          label='Type'
          placeholder='Thesis Types'
          data={Object.keys(GLOBAL_CONFIG.thesis_types).map((key) => ({
            value: key,
            label: formatThesisType(key),
          }))}
          value={filters.types ?? []}
          onChange={(value) => {
            setFilters((prev) => ({
              ...prev,
              types: value,
            }))
          }}
          searchable
        />
      </Grid.Col>
      <Grid.Col span={size === 'sm' ? 12 : 6}>
        <MultiSelect
          hidePickedOptions
          label='States'
          placeholder='Application States'
          data={Object.values(ApplicationState).map((value) => ({
            value: value,
            label: formatApplicationState(value),
          }))}
          value={filters.states ?? []}
          onChange={(value) => {
            setFilters((prev) => ({
              ...prev,
              states: value,
            }))
          }}
          searchable
        />
      </Grid.Col>
      <Grid.Col span={size === 'sm' ? 12 : 6}>
        <Select
          label='Sort By'
          data={[
            { label: 'Created Ascending', value: 'createdAt:asc' },
            { label: 'Created Descending', value: 'createdAt:desc' },
          ]}
          value={sort.column + ':' + sort.direction}
          onChange={(x) =>
            setSort({
              column: (x?.split(':')[0] ?? 'createdAt') as 'createdAt' | 'updatedAt',
              direction: (x?.split(':')[1] ?? 'asc') as 'asc' | 'desc',
            })
          }
        />
      </Grid.Col>
    </Grid>
  )
}

export default ApplicationsFilters
