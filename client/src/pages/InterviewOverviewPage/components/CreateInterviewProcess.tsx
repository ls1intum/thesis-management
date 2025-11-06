import {
  Button,
  Center,
  Collapse,
  Group,
  Loader,
  Modal,
  Paper,
  ScrollArea,
  Stack,
  TextInput,
  Title,
  Text,
  Input,
  Badge,
  Divider,
  useMantineColorScheme,
} from '@mantine/core'
import { useEffect, useState } from 'react'
import { PaginationResponse } from '../../../requests/responses/pagination'
import { ITopicInterviewProcess } from '../../../requests/responses/interview'
import { doRequest } from '../../../requests/request'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { CheckCircleIcon, MagnifyingGlassIcon } from '@phosphor-icons/react'
import { showNotification } from '@mantine/notifications'

interface CreateInterviewProcessProps {
  opened: boolean
  onClose: () => void
}

const CreateInterviewProcess = ({ opened, onClose }: CreateInterviewProcessProps) => {
  const [possibleInterviewTopics, setPossibleInterviewTopics] =
    useState<PaginationResponse<ITopicInterviewProcess>>()
  const [filteredTopics, setFilteredTopics] = useState<ITopicInterviewProcess[]>([])
  const [topicsLoading, setTopicsLoading] = useState(false)

  const [selectedTopic, setSelectedTopic] = useState<ITopicInterviewProcess | null>(null)
  const [searchKey, setSearchKey] = useState('')

  const fetchPossibleInterviewTopics = async () => {
    setTopicsLoading(true)
    doRequest<PaginationResponse<ITopicInterviewProcess>>(
      '/v2/topics/interview-topics',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          limit: -1,
        },
      },
      (res) => {
        if (res.ok) {
          setPossibleInterviewTopics(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setTopicsLoading(false)
      },
    )
  }

  const createInterviewProcess = () => {
    if (!selectedTopic) return

    doRequest(
      '/v2/interview-process',
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          topicId: selectedTopic.topicId,
        },
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: 'Research group created.',
            color: 'green',
          })
          setPossibleInterviewTopics((prev) => {
            if (!prev) return prev
            const updatedContent = prev.content.map((topic) =>
              topic.topicId === selectedTopic.topicId
                ? { ...topic, interviewProcessExists: true }
                : topic,
            )
            return {
              ...prev,
              content: updatedContent,
            }
          })
          closeModal()
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  const closeModal = () => {
    onClose()
    setSelectedTopic(null)
    setSearchKey('')
  }

  useEffect(() => {
    fetchPossibleInterviewTopics()
  }, [])

  useEffect(() => {
    if (searchKey.trim() !== '') {
      const filtered = possibleInterviewTopics?.content.filter((topic) =>
        topic.topicTitle.toLowerCase().includes(searchKey.toLowerCase()),
      )
      setFilteredTopics(filtered || [])
    } else {
      setFilteredTopics(possibleInterviewTopics?.content || [])
    }
  }, [searchKey, possibleInterviewTopics])

  const colorScheme = useMantineColorScheme()

  return (
    <Modal
      opened={opened}
      onClose={closeModal}
      centered
      size='xl'
      title={<Title order={3}>Create Interview Process</Title>}
    >
      <Stack>
        <Input.Wrapper label='Select Topic' withAsterisk>
          {selectedTopic ? (
            <Paper
              withBorder
              bg={colorScheme.colorScheme === 'dark' ? 'primary.3' : 'primary.0'}
              c={colorScheme.colorScheme === 'dark' ? 'primary.10' : 'primary'}
              m={'xs'}
            >
              <Group p={'xs'} justify='space-between' align='center' wrap='nowrap'>
                <Group wrap='nowrap' justify='center' align='center' gap={'0.5rem'}>
                  <CheckCircleIcon size={24} weight={'bold'} style={{ flexShrink: 0 }} />
                  <Text fw={600} pt={'2px'} lineClamp={2} unselectable='on'>
                    {selectedTopic.topicTitle}
                  </Text>
                </Group>
                <Button
                  variant={'subtle'}
                  onClick={() => setSelectedTopic(null)}
                  style={{ flexShrink: 0 }}
                  c={colorScheme.colorScheme === 'dark' ? 'primary.10' : 'primary'}
                >
                  Change
                </Button>
              </Group>
            </Paper>
          ) : (
            <TextInput
              placeholder='Select topic...'
              leftSection={<MagnifyingGlassIcon size={16} />}
              value={searchKey}
              onChange={(x) => setSearchKey(x.target.value || '')}
              m={'xs'}
            />
          )}
          <Collapse in={!selectedTopic} m={'xs'}>
            {topicsLoading ? (
              <Center h={'30vh'} w={'100%'}>
                <Loader />
              </Center>
            ) : (
              <ScrollArea
                h={'30vh'}
                w={'100%'}
                type='hover'
                offsetScrollbars
                bdrs={'md'}
                bg={colorScheme.colorScheme === 'dark' ? 'dark.8' : 'gray.0'}
              >
                {filteredTopics.map((topic, index) => (
                  <Stack key={topic.topicTitle} gap={0} pr={5} style={{ cursor: 'pointer' }}>
                    <Group
                      justify='space-between'
                      align='center'
                      onClick={() => {
                        if (!topic.interviewProcessExists) {
                          setSelectedTopic(topic)
                        }
                      }}
                      p={'1rem'}
                    >
                      <Text
                        size='sm'
                        fw={600}
                        c={topic.interviewProcessExists ? 'dimmed' : undefined}
                      >
                        {topic.topicTitle}
                      </Text>
                      {topic.interviewProcessExists && (
                        <Badge color={'gray'} radius={'sm'}>
                          Process exists
                        </Badge>
                      )}
                    </Group>
                    {index < filteredTopics.length - 1 && <Divider />}
                  </Stack>
                ))}
              </ScrollArea>
            )}
          </Collapse>
        </Input.Wrapper>
        <Button
          onClick={() => {
            createInterviewProcess()
          }}
          disabled={!selectedTopic}
        >
          Create Interview Process
        </Button>
      </Stack>
    </Modal>
  )
}

export default CreateInterviewProcess
