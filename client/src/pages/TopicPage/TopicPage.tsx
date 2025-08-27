import React from 'react'
import { usePageTitle } from '../../hooks/theme'
import { Link, useNavigate, useParams } from 'react-router'
import { useTopic } from '../../hooks/fetcher'
import NotFound from '../../components/NotFound/NotFound'
import PageLoader from '../../components/PageLoader/PageLoader'
import { Button, Divider, Grid, Group, Stack, Title } from '@mantine/core'
import TopicData from '../../components/TopicData/TopicData'
import { useManagementAccess } from '../../hooks/authentication'
import ApplicationsProvider from '../../providers/ApplicationsProvider/ApplicationsProvider'
import ApplicationsTable from '../../components/ApplicationsTable/ApplicationsTable'
import { NotePencil } from 'phosphor-react'
import TopicAdittionalInformationCard from './components/TopicAdittionalInformationCard'

const TopicPage = () => {
  const { topicId } = useParams<{ topicId: string }>()

  const navigate = useNavigate()
  const managementAccess = useManagementAccess()

  const topic = useTopic(topicId)

  usePageTitle(topic ? topic.title : 'Topic')

  if (topic === false) {
    return <NotFound />
  }

  if (!topic) {
    return <PageLoader />
  }

  return (
    <Stack gap={'2rem'}>
      <Stack gap={'1rem'}>
        <Title>{topic.title}</Title>
        <Button
          component={Link}
          to={`/submit-application/${topic.topicId}`}
          mr={'auto'}
          leftSection={<NotePencil size={24} />}
          size='md'
        >
          Apply Now
        </Button>
      </Stack>

      <Grid>
        <Grid.Col span={{ base: 12, md: 9 }} order={{ base: 2, md: 1 }}>
          <div>Left</div>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 3 }} order={{ base: 1, md: 2 }}>
          <TopicAdittionalInformationCard topic={topic} />
        </Grid.Col>
      </Grid>

      <TopicData topic={topic} />
      <Group>
        {managementAccess && (
          <Button variant='outline' component={Link} to='/topics'>
            Manage Topics
          </Button>
        )}
        <Button ml='auto' component={Link} to={`/submit-application/${topic.topicId}`}>
          Apply Now
        </Button>
      </Group>
      {managementAccess && (
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
