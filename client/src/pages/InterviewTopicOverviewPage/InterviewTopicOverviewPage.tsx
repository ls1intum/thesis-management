import { Center, Loader, Stack, Title } from '@mantine/core'
import CalendarCarousel from './components/CalendarCarousel'
import IntervieweesList from './components/IntervieweesList'
import InterviewProcessProvider from '../../providers/InterviewProcessProvider/InterviewProcessProvider'
import { useParams } from 'react-router'
import { doRequest } from '../../requests/request'
import { useEffect, useState } from 'react'

const InterviewTopicOverviewPage = () => {
  const { processId } = useParams<{ processId: string }>()

  const [processCompleted, setProcessCompleted] = useState<boolean | null>(null)

  const fetchInterviewState = async () => {
    doRequest<boolean>(
      `/v2/interview-process/${processId}/completed`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setProcessCompleted(res.data)
        } else {
          setProcessCompleted(false)
        }
      },
    )
  }

  useEffect(() => {
    fetchInterviewState()
  }, [])

  return processCompleted !== null ? (
    <Stack h={'100%'} gap={'2rem'}>
      <Stack gap={'0.5rem'}>
        <Title>Interview Management</Title>
        <Title order={5} c={'dimmed'}>
          Integrating Gender Sensitivity and Adaptive Learning in Education Games
        </Title>
      </Stack>

      <InterviewProcessProvider>
        <CalendarCarousel disabled={processCompleted} />

        <IntervieweesList disabled={processCompleted} />
      </InterviewProcessProvider>
    </Stack>
  ) : (
    <Center style={{ height: '100%' }}>
      <Loader />
    </Center>
  )
}
export default InterviewTopicOverviewPage
