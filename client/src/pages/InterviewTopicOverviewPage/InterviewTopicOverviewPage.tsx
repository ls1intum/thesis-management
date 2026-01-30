import { Center, Loader, Stack, Title } from '@mantine/core'
import CalendarCarousel from './components/CalendarCarousel'
import IntervieweesList from './components/IntervieweesList'
import InterviewProcessProvider from '../../providers/InterviewProcessProvider/InterviewProcessProvider'
import { useParams } from 'react-router'
import { doRequest } from '../../requests/request'
import { useEffect, useState } from 'react'
import { IInterviewProcess } from '../../requests/responses/interview'

const InterviewTopicOverviewPage = () => {
  const { processId } = useParams<{ processId: string }>()

  const [processCompleted, setProcessCompleted] = useState<boolean | null>(null)
  const [title, setTitle] = useState<string | null>(null)

  const fetchInterviewProcess = async () => {
    doRequest<IInterviewProcess>(
      `/v2/interview-process/${processId}`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setProcessCompleted(res.data.completed)
          setTitle(res.data.topicTitle)
        } else {
          setProcessCompleted(false)
        }
      },
    )
  }

  useEffect(() => {
    fetchInterviewProcess()
  }, [])

  return processCompleted !== null ? (
    <Stack h={'100%'} gap={'2rem'}>
      <Stack gap={'0.5rem'}>
        <Title>Interview Management</Title>
        {title && (
          <Title order={5} c={'dimmed'}>
            {title}
          </Title>
        )}
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
