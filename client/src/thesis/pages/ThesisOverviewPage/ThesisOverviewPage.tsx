import React from 'react'
import ThesesFilters from '@/thesis/components/ThesesFilters/ThesesFilters'
import ThesesProvider from '@/thesis/providers/ThesesProvider/ThesesProvider'
import { usePageTitle } from '@/core/hooks/theme'
import ThesesGanttChart from '@/thesis/components/ThesesGanttChart/ThesesGanttChart'
import { Stack, Title } from '@mantine/core'
import { ThesisState } from '@/thesis/requests/responses/thesis'

const ThesisOverviewPage = () => {
  usePageTitle('Theses Overview')

  return (
    <ThesesProvider
      fetchAll={true}
      defaultStates={[
        ThesisState.PROPOSAL,
        ThesisState.WRITING,
        ThesisState.SUBMITTED,
        ThesisState.ASSESSED,
        ThesisState.GRADED,
      ]}
      limit={200}
    >
      <Stack>
        <Title>Theses Overview</Title>
        <ThesesFilters />
        <ThesesGanttChart />
      </Stack>
    </ThesesProvider>
  )
}

export default ThesisOverviewPage
