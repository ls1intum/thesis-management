import { Center, Divider, Flex, Loader, ScrollArea, Stack, Title } from '@mantine/core'
import { IInterviewee } from '../../requests/responses/interview'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import ScoreCard from './components/ScoreCard'
import { useEffect, useState } from 'react'
import InterviewNoteCard from './components/InterviewNoteCard'
import { doRequest } from '../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import UserCard from '../../components/UserCard/UserCard'
import TopicInformationCard from '../TopicPage/components/TopicInformationCard'

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

  const [saving, setSaving] = useState(false)

  const saveIntervieweeAssesment = async (newScore?: number, newNote?: string) => {
    if (!interviewee) {
      return
    }

    setSaving(true)

    doRequest<IInterviewee>(
      `/v2/interview-process/${processId}/interviewee/${intervieweeId}`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          intervieweeNote: newNote,
          score: newScore !== undefined ? newScore : interviewee.score,
        },
      },
      (res) => {
        if (res.ok) {
          setInterviewee(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setSaving(false)
      },
    )
  }

  const isSmaller = useIsSmallerBreakpoint('sm')

  return intervieweeLoading ? (
    <Center style={{ height: '100%' }}>
      <Loader />
    </Center>
  ) : (
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
                  saveIntervieweeAssesment(newScore, undefined)
                }}
                disabled={saving}
              />
              <InterviewNoteCard
                interviewNote={interviewee?.interviewNote ?? ''}
                onInterviewNoteChange={(newNote) => {
                  setInterviewee((prev) => {
                    if (!prev) return prev
                    return { ...prev, interviewNote: newNote }
                  })
                  //Debounce happens in card
                  saveIntervieweeAssesment(undefined, newNote)
                }}
              />
            </Stack>
          </ScrollArea>
        </Stack>
        <Divider orientation='vertical' />
        <Stack w={{ base: '100%', md: '33%' }} h={'100%'} gap={'1.5rem'}>
          <Title order={3}>Application</Title>
          <ScrollArea h={'100%'} w={'100%'} type={isSmaller ? 'never' : 'hover'} offsetScrollbars>
            <Stack h={'100%'} gap={'1rem'}>
              {interviewee && <UserCard user={interviewee.user} />}
              {interviewee?.application && (
                <TopicInformationCard
                  title='Motivation'
                  content={interviewee.application.motivation}
                />
              )}
            </Stack>
          </ScrollArea>
        </Stack>
      </Flex>
    </Stack>
  )
}
export default IntervieweeAssesmentPage
