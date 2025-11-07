import { Group, SegmentedControl, Stack, Title, Button, TextInput } from '@mantine/core'
import { InterviewState } from '../../requests/responses/interview'
import { MagnifyingGlassIcon, PaperPlaneTiltIcon, PlusIcon } from '@phosphor-icons/react'
import CalendarCarousel from './components/CalendarCarousel'
import { useState } from 'react'
import IntervieweesList from './components/IntervieweesList'

const InterviewTopicOverviewPage = () => {
  const [searchIntervieweeKey, setSearchIntervieweeKey] = useState('')

  return (
    <Stack h={'100%'} gap={'2rem'}>
      <Stack gap={'0.5rem'}>
        <Title>Interview Management</Title>
        <Title order={5} c={'dimmed'}>
          Integrating Gender Sensitivity and Adaptive Learning in Education Games
        </Title>
      </Stack>

      <CalendarCarousel />

      <IntervieweesList />
    </Stack>
  )
}
export default InterviewTopicOverviewPage
