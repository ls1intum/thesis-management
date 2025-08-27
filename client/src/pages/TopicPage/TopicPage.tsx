import React from 'react'
import { usePageTitle } from '../../hooks/theme'
import { Link, useNavigate, useParams } from 'react-router'
import { useTopic } from '../../hooks/fetcher'
import NotFound from '../../components/NotFound/NotFound'
import PageLoader from '../../components/PageLoader/PageLoader'
import { Button, Card, Divider, Grid, Group, Stack, Title } from '@mantine/core'
import TopicData from '../../components/TopicData/TopicData'
import { useManagementAccess, useUser } from '../../hooks/authentication'
import ApplicationsProvider from '../../providers/ApplicationsProvider/ApplicationsProvider'
import ApplicationsTable from '../../components/ApplicationsTable/ApplicationsTable'
import { NotePencil } from 'phosphor-react'
import TopicAdittionalInformationCard from './components/TopicAdittionalInformationCard'
import DocumentEditor from '../../components/DocumentEditor/DocumentEditor'
import TopicInformationCard from './components/TopicInformationCard'

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

  const checkIfUserIsSupervisorOrAdvisor = () => {
    if (!user) return false
    const userId = user.userId
    const isSupervisor = topic.supervisors.some((supervisor) => supervisor.userId === userId)
    const isAdvisor = topic.advisors.some((advisor) => advisor.userId === userId)
    return isSupervisor || isAdvisor
  }

  return (
    <Stack gap={'2rem'}>
      <Stack gap={'1rem'}>
        <Title>{topic.title}</Title>
        {!checkIfUserIsSupervisorOrAdvisor() && (
          <Button
            component={Link}
            to={`/submit-application/${topic.topicId}`}
            mr={'auto'}
            leftSection={<NotePencil size={24} />}
            size='md'
          >
            Apply Now
          </Button>
        )}
      </Stack>

      <Grid>
        <Grid.Col span={{ base: 12, md: 9 }} order={{ base: 2, md: 1 }}>
          <Stack gap={'1.5rem'}>
            <TopicInformationCard title='Problem Statement' content={topic.problemStatement} />
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
          <ApplicationsProvider fetchAll={true} limit={10} defaultTopics={[topic.topicId]}>
            <ApplicationsTable
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
