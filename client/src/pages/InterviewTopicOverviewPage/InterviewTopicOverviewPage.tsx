import { Stack, Title } from '@mantine/core'
import CalendarCarousel from './components/CalendarCarousel'
import IntervieweesList from './components/IntervieweesList'
import InterviewProcessProvider from '../../providers/InterviewProcessProvider/InterviewProcessProvider'
import { useParams } from 'react-router'

const InterviewTopicOverviewPage = () => {
  return (
    <Stack h={'100%'} gap={'2rem'}>
      <Stack gap={'0.5rem'}>
        <Title>Interview Management</Title>
        <Title order={5} c={'dimmed'}>
          Integrating Gender Sensitivity and Adaptive Learning in Education Games
        </Title>
      </Stack>

      <InterviewProcessProvider>
        <CalendarCarousel />

        <IntervieweesList />
      </InterviewProcessProvider>
    </Stack>
  )
}
export default InterviewTopicOverviewPage
