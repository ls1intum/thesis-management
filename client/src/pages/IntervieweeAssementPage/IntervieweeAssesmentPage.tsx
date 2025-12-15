import { Divider, Flex, ScrollArea, Stack, Title } from '@mantine/core'
import { IInterviewee } from '../../requests/responses/interview'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import ScoreCard from './components/ScoreCard'
import { useEffect, useState } from 'react'
import InterviewNoteCard from './components/InterviewNoteCard'
import { doRequest } from '../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'

const IntervieweeAssesmentPage = () => {
  const { processId } = useParams<{ processId: string }>()
  const { intervieweeId } = useParams<{ intervieweeId: string }>()

  const [interviewee, setInterviewee] = useState<IInterviewee | null>(null)

  const [intervieweeLoading, setIntervieweeLoading] = useState(false)

  const fetchInterviewee = async () => {
    setIntervieweeLoading(true)
    doRequest<IInterviewee>(
      `/v2/interview-process/${processId}/interviewee/${intervieweeId}`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setInterviewee(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setIntervieweeLoading(false)
      },
    )
  }

  useEffect(() => {
    fetchInterviewee()
  }, [])

  const isSmaller = useIsSmallerBreakpoint('sm')

  return (
    <Stack h={'100%'} gap={'1.5rem'}>
      <Stack gap={'0.5rem'}>
        <Title>{`Interview - ${interviewee?.user.firstName} ${interviewee?.user.lastName}`}</Title>
        {interviewee?.application && (
          <Title order={4} c={'dimmed'}>
            {interviewee.application?.thesisTitle}
          </Title>
        )}
      </Stack>

      <Flex
        h={'100%'}
        w={'100%'}
        direction={{ base: 'column', md: 'row' }}
        justify={'space-between'}
        gap={{ base: '1rem', md: '2rem' }}
        style={{ overflow: 'auto' }}
      >
        <Stack h={'100%'} flex={1} gap={'1.5rem'}>
          <Title order={3}>Interview Assesment</Title>

          <ScrollArea h={'100%'} w={'100%'} type={isSmaller ? 'never' : 'hover'} offsetScrollbars>
            <Stack h={'100%'} gap={'1rem'}>
              <ScoreCard
                score={interviewee?.score ?? 0}
                onScoreChange={(newScore) => {
                  setInterviewee((prev) => {
                    if (!prev) return prev
                    return { ...prev, score: newScore }
                  })
                  //TODO: send to server
                }}
              />
              <InterviewNoteCard
                interviewNote={interviewee?.interviewNote ?? ''}
                onInterviewNoteChange={(newNote) => {
                  setInterviewee((prev) => {
                    if (!prev) return prev
                    return { ...prev, interviewNote: newNote }
                  })
                  //TODO: send to server & delay for auto save
                }}
              />
            </Stack>
          </ScrollArea>
        </Stack>
        <Divider orientation='vertical' />
        <Stack w={{ base: '100%', md: '33%' }} h={'100%'} gap={'1.5rem'}>
          <Title order={3}>Application</Title>
          <ScrollArea h={'100%'} w={'100%'} type={isSmaller ? 'never' : 'hover'} offsetScrollbars>
            <div>TODO</div>
          </ScrollArea>
        </Stack>
      </Flex>
    </Stack>
  )
}
export default IntervieweeAssesmentPage
