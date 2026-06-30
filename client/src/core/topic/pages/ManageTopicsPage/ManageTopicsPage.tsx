import React, { useState } from 'react'
import { usePageTitle } from '@/core/hooks/theme'
import { Button, Group, Stack, Title } from '@mantine/core'
import TopicsProvider from '@/core/topic/providers/TopicsProvider/TopicsProvider'
import TopicsTable from '@/core/topic/components/TopicsTable/TopicsTable'
import { TopicState } from '@/core/topic/requests/responses/topic'
import { PencilIcon } from '@phosphor-icons/react'
import CloseTopicButton from '@/core/topic/pages/ManageTopicsPage/components/CloseTopicButton/CloseTopicButton'
import ReplaceTopicModal from '@/core/topic/pages/ManageTopicsPage/components/ReplaceTopicModal/ReplaceTopicModal'
import TopicsFilters from '@/core/topic/components/TopicsFilters/TopicsFilters'

const ManageTopicsPage = () => {
  usePageTitle('Manage Topics')

  const [editingTopicId, setEditingTopicId] = useState<string>()
  const [createTopicModal, setCreateTopicModal] = useState(false)

  return (
    <TopicsProvider limit={20} persistState>
      <Stack gap='md'>
        <Group>
          <Title>Manage Topics</Title>
          <Button ml='auto' onClick={() => setCreateTopicModal(true)} visibleFrom='md'>
            Create Topic
          </Button>
        </Group>
        <ReplaceTopicModal opened={createTopicModal} onClose={() => setCreateTopicModal(false)} />
        <ReplaceTopicModal
          opened={Boolean(editingTopicId)}
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
