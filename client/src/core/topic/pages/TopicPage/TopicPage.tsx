import React from 'react'
import { usePageTitle } from '@/core/hooks/theme'
import { Link, useNavigate, useParams } from 'react-router'
import { useTopic } from '@/core/hooks/fetcher'
import NotFound from '@/core/components/NotFound/NotFound'
import PageLoader from '@/core/components/PageLoader/PageLoader'
import { Button, Divider, Grid, Stack, Text, Title } from '@mantine/core'
import { useManagementAccess, useUser } from '@/core/hooks/authentication'
import ApplicationsProvider from '@/core/application/providers/ApplicationsProvider/ApplicationsProvider'
import ApplicationsTable from '@/core/application/components/ApplicationsTable/ApplicationsTable'
import { NotePencil } from '@phosphor-icons/react'
import TopicAdittionalInformationCard from '@/core/topic/pages/TopicPage/components/TopicAdittionalInformationCard'
import TopicInformationCard from '@/core/topic/pages/TopicPage/components/TopicInformationCard'
import { TopicState } from '@/core/topic/requests/responses/topic'

const TopicPage = () => {
  const { topicId } = useParams<{ topicId: string }>()

  const navigate = useNavigate()
  const managementAccess = useManagementAccess()

  const topic = useTopic(topicId)

  usePageTitle(topic ? topic.title : 'Topic')

  const user = useUser()

  if (topic === false) {
    return <NotFound />
  }

  if (!topic) {
    return <PageLoader />
  }

  const checkIfUserIsExaminerOrSupervisor = () => {
    if (!user) return false
    const userId = user.userId
    const isExaminer = (topic.examiners ?? []).some((examiner) => examiner.userId === userId)
    const isSupervisor = (topic.supervisors ?? []).some(
      (supervisor) => supervisor.userId === userId,
    )
    return isExaminer || isSupervisor
  }

  const canApply = topic.state === TopicState.OPEN

  return (
    <Stack gap={'2rem'}>
      <Stack gap={'1rem'}>
        <Title>{topic.title}</Title>
        {!managementAccess && !checkIfUserIsExaminerOrSupervisor() && (
          <Stack gap='xs' mr='auto'>
            {canApply ? (
              <Button
                component={Link}
                to={`/submit-application/${topic.topicId}`}
                leftSection={<NotePencil size={24} />}
                size='md'
              >
                Apply Now
              </Button>
            ) : (
              <Button leftSection={<NotePencil size={24} />} size='md' disabled>
                Apply Now
              </Button>
            )}
            {!canApply && (
              <Text size='sm' c='dimmed'>
                Applications are not open for this topic.
              </Text>
            )}
          </Stack>
        )}
      </Stack>

      <Grid>
        <Grid.Col span={{ base: 12, md: 9 }} order={{ base: 2, md: 1 }}>
          <Stack gap={'1.5rem'}>
            <TopicInformationCard
              title='Problem Statement'
              content={topic.problemStatement ?? ''}
            />
            {topic.requirements && (
              <TopicInformationCard title='Requirements' content={topic.requirements} />
            )}
            {topic.goals && <TopicInformationCard title='Goals' content={topic.goals} />}
            {topic.references && (
              <TopicInformationCard title='References' content={topic.references} />
            )}
          </Stack>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 3 }} order={{ base: 1, md: 2 }}>
          <TopicAdittionalInformationCard topic={topic} />
        </Grid.Col>
      </Grid>
      {managementAccess && (user ? topic.researchGroup.name === user.researchGroupName : false) && (
        <Stack>
          <Divider />
          <ApplicationsProvider
            fetchAll={true}
            limit={10}
            defaultTopics={[topic.topicId]}
            defaultIncludeSuggestedTopics={false}
          >
            <ApplicationsTable
              columns={['state', 'thesis_type', 'user', 'created_at']}
              onApplicationClick={(application) =>
                navigate(`/applications/${application.applicationId}`)
              }
            />
          </ApplicationsProvider>
        </Stack>
      )}
    </Stack>
  )
}

export default TopicPage
