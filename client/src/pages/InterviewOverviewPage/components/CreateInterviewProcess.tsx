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
  useMantineColorScheme,
} from '@mantine/core'
import { Dispatch, SetStateAction, useEffect, useState } from 'react'
import { PaginationResponse } from '../../../requests/responses/pagination'
import {
  IApplicationInterviewProcess,
  IInterviewProcess,
  ITopicInterviewProcess,
} from '../../../requests/responses/interview'
import { doRequest } from '../../../requests/request'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { CheckCircleIcon, MagnifyingGlassIcon } from '@phosphor-icons/react'
import { showNotification } from '@mantine/notifications'
import SelectApplicantsList from './SelectApplicantsList'
import SelectTopicInterviewProcessItem from './SelectTopicInterviewProcessItem'

interface CreateInterviewProcessProps {
  opened: boolean
  onClose: () => void
  setInterviewProcesses: Dispatch<SetStateAction<IInterviewProcess[]>>
}

const CreateInterviewProcess = ({
  opened,
  onClose,
  setInterviewProcesses,
}: CreateInterviewProcessProps) => {
  const [possibleInterviewTopics, setPossibleInterviewTopics] =
    useState<PaginationResponse<ITopicInterviewProcess>>()
  const [filteredTopics, setFilteredTopics] = useState<ITopicInterviewProcess[]>([])
  const [topicsLoading, setTopicsLoading] = useState(false)

  const [selectedTopic, setSelectedTopic] = useState<ITopicInterviewProcess | null>(null)
  const [searchKey, setSearchKey] = useState('')

  const [possibleInterviewApplicants, setPossibleInterviewApplicants] = useState<
    IApplicationInterviewProcess[]
  >([])
  const [applicantsLoading, setApplicantsLoading] = useState(false)

  const [selectedApplicants, setSelectedApplicants] = useState<string[]>([])

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

  const fetchPossibleInterviewApplicantsByTopic = async () => {
    if (!selectedTopic) {
      setPossibleInterviewApplicants([])
      return
    }

    setApplicantsLoading(true)
    doRequest<PaginationResponse<IApplicationInterviewProcess>>(
      `/v2/applications/interview-applications`,
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          topicId: selectedTopic.topicId,
          limit: -1,
        },
      },
      (res) => {
        if (res.ok) {
          setPossibleInterviewApplicants(res.data.content)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setApplicantsLoading(false)
      },
    )
  }

  const createInterviewProcess = () => {
    if (!selectedTopic) return

    doRequest<IInterviewProcess>(
      '/v2/interview-process',
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          topicId: selectedTopic.topicId,
          intervieweeApplicationIds: selectedApplicants,
        },
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: 'Interview process created successfully.',
            color: 'green',
          })
          setInterviewProcesses((prev) => [res.data, ...prev])
          setPossibleInterviewTopics((prev) => {
            if (!prev) return prev
            return {
              ...prev,
              content: prev.content.map((topic) => {
                if (topic.topicId === selectedTopic.topicId) {
                  return { ...topic, interviewProcessExists: true }
                }
                return topic
              }),
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
    setSelectedApplicants([])
  }

  useEffect(() => {
    fetchPossibleInterviewTopics()
  }, [])

  useEffect(() => {
    fetchPossibleInterviewApplicantsByTopic()
  }, [selectedTopic])

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
                  onClick={() => {
                    setSelectedTopic(null)
                    setSearchKey('')
                    setSelectedApplicants([])
                  }}
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
              <ScrollArea.Autosize
                mih={'50px'}
                mah={'30vh'}
                w={'100%'}
                type='hover'
                bdrs={'md'}
                bg={colorScheme.colorScheme === 'dark' ? 'dark.8' : 'gray.0'}
              >
                {filteredTopics.length === 0 ? (
                  <Paper bg={colorScheme.colorScheme === 'dark' ? 'dark.8' : 'gray.0'} h={'50px'}>
                    <Center>
                      <Text c='dimmed' m={'xs'}>
                        No topics found.
                      </Text>
                    </Center>
                  </Paper>
                ) : (
                  filteredTopics.map((topic, index) => (
                    <SelectTopicInterviewProcessItem
                      key={topic.topicTitle}
                      topic={topic}
                      setSelectedTopic={setSelectedTopic}
                      isLastItem={index === filteredTopics.length - 1}
                    />
                  ))
                )}
              </ScrollArea.Autosize>
            )}
          </Collapse>
        </Input.Wrapper>

        <Input.Wrapper
          label='Select Applicants (optional)'
          description='Select applicants for this interview process. You can also add them later or while reviewing applications.'
        >
          {applicantsLoading ? (
            <Center h={'10vh'} w={'100%'}>
              <Loader />
            </Center>
          ) : (
            <Collapse
              in={selectedTopic !== null}
              m={'xs'}
              h={
                selectedTopic !== null
                  ? possibleInterviewApplicants.length === 0
                    ? '50px'
                    : 'fit-content'
                  : '0'
              }
            >
              {possibleInterviewApplicants.length === 0 ? (
                <Paper bg={colorScheme.colorScheme === 'dark' ? 'dark.8' : 'gray.0'} h={'50px'}>
                  <Center>
                    <Text c='dimmed' m={'xs'}>
                      No applicants found for the selected topic.
                    </Text>
                  </Center>
                </Paper>
              ) : (
                <SelectApplicantsList
                  possibleInterviewApplicants={possibleInterviewApplicants}
                  selectedApplicants={selectedApplicants}
                  setSelectedApplicants={setSelectedApplicants}
                />
              )}
            </Collapse>
          )}
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
