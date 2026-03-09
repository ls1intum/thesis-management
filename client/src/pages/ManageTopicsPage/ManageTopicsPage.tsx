import React, { useState } from 'react'
import { usePageTitle } from '../../hooks/theme'
import { Button, Group, Stack, Title } from '@mantine/core'
import TopicsProvider from '../../providers/TopicsProvider/TopicsProvider'
import TopicsTable from '../../components/TopicsTable/TopicsTable'
import { TopicState } from '../../requests/responses/topic'
import { PencilIcon } from '@phosphor-icons/react'
import CloseTopicButton from './components/CloseTopicButton/CloseTopicButton'
import ReplaceTopicModal from './components/ReplaceTopicModal/ReplaceTopicModal'
import TopicsFilters from '../../components/TopicsFilters/TopicsFilters'

const ManageTopicsPage = () => {
  usePageTitle('Manage Topics')

  const [editingTopicId, setEditingTopicId] = useState<string>()
  const [createTopicModal, setCreateTopicModal] = useState(false)

  return (
    <TopicsProvider limit={20}>
      <Stack gap='md'>
        <Group>
          <Title>Manage Topics</Title>
          <Button ml='auto' onClick={() => setCreateTopicModal(true)} visibleFrom='md'>
            Create Topic
          </Button>
        </Group>
        <ReplaceTopicModal opened={createTopicModal} onClose={() => setCreateTopicModal(false)} />
        <ReplaceTopicModal
          opened={!!editingTopicId}
          onClose={() => setEditingTopicId(undefined)}
          topicId={editingTopicId}
        />
        <Button ml='auto' onClick={() => setCreateTopicModal(true)} hiddenFrom='md'>
          Create Topic
        </Button>
        <TopicsFilters visible={['states']} />
        <TopicsTable
          columns={['state', 'title', 'types', 'examiner', 'supervisor', 'createdAt', 'actions']}
          extraColumns={{
            actions: {
              accessor: 'actions',
              title: 'Actions',
              textAlign: 'center',
              noWrap: true,
              width: 120,
              render: (topic) => (
                <Group
                  preventGrowOverflow={false}
                  justify='center'
                  onClick={(e) => e.stopPropagation()}
                  gap='xs'
                >
                  {topic.state !== TopicState.CLOSED && (
                    <Button size='xs' onClick={() => setEditingTopicId(topic.topicId)}>
                      <PencilIcon />
                    </Button>
                  )}
                  <CloseTopicButton size='xs' topic={topic} />
                </Group>
              ),
            },
          }}
        />
      </Stack>
    </TopicsProvider>
  )
}

export default ManageTopicsPage
