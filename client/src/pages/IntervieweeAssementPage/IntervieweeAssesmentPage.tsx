import { Divider, Flex, ScrollArea, Stack, Title } from '@mantine/core'
import { IInterviewee } from '../../requests/responses/interview'
import { useIsSmallerBreakpoint } from '../../hooks/theme'
import ScoreCard from './components/ScoreCard'
import { useState } from 'react'
import InterviewNoteCard from './components/InterviewNoteCard'

const IntervieweeAssesmentPage = () => {
  const [interviewee, setInterviewee] = useState<IInterviewee>({
    //TODO: replace with real data
    intervieweeId: 'u1',
    user: {
      userId: 'u1',
      firstName: 'Alice',
      lastName: 'Smith',
      avatar: null,
      universityId: '',
      matriculationNumber: null,
      email: null,
      studyDegree: null,
      studyProgram: null,
      customData: null,
      joinedAt: '',
      groups: [],
    },
    score: 4,
    lastInvited: new Date('2023-01-01'),
    interviewNote: '',
    application: {
      applicationId: 'a1',
      studyDegree: 'Master',
      studyProgram: 'Computer Science',
      thesisTitle: 'Integrating Gender Sensitivity and Adaptive Learning in Education Games',
      motivation: 'I want to explore the use of AI to enhance learning experiences.',
    },
  })

  const isSmaller = useIsSmallerBreakpoint('sm')

  return (
    <Stack h={'100%'} gap={'1.5rem'}>
      <Stack gap={'0.5rem'}>
        <Title>{`Interview - ${interviewee.user.firstName} ${interviewee.user.lastName}`}</Title>
        {interviewee.application && (
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
                score={interviewee.score}
                onScoreChange={(newScore) => {
                  setInterviewee((prev) => ({ ...prev, score: newScore }))
                  //TODO: send to server
                }}
              />
              <InterviewNoteCard
                interviewNote={interviewee.interviewNote}
                onInterviewNoteChange={(newNote) => {
                  setInterviewee((prev) => ({ ...prev, interviewNote: newNote }))
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
