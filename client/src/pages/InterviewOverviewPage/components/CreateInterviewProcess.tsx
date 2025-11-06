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
import {
  IApplicationInterviewProcess,
  ITopicInterviewProcess,
} from '../../../requests/responses/interview'
import { doRequest } from '../../../requests/request'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { CheckCircleIcon, CircleIcon, MagnifyingGlassIcon } from '@phosphor-icons/react'
import { showNotification } from '@mantine/notifications'
import { ApplicationState } from '../../../requests/responses/application'

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
              </ScrollArea.Autosize>
            )}
          </Collapse>
        </Input.Wrapper>

        <Input.Wrapper
          label='Select Applicants (optional)'
          description='Select applicants for this interview process. You can also add them later.'
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
                <Stack
                  bg={colorScheme.colorScheme === 'dark' ? 'dark.8' : 'gray.0'}
                  bdrs={'md'}
                  gap={0}
                >
                  <Group
                    w={'100%'}
                    justify='space-between'
                    px={'1rem'}
                    py={'0.5rem'}
                    bg={
                      selectedApplicants.length > 0
                        ? colorScheme.colorScheme === 'dark'
                          ? 'primary.11'
                          : 'primary.2'
                        : colorScheme.colorScheme === 'dark'
                          ? 'dark.9'
                          : 'gray.2'
                    }
                  >
                    <Text fw={500}>{`${selectedApplicants.length} selected`}</Text>
                    <Button
                      variant={'subtle'}
                      onClick={() => {
                        if (selectedApplicants.length === possibleInterviewApplicants.length) {
                          setSelectedApplicants([])
                        } else {
                          setSelectedApplicants(
                            possibleInterviewApplicants.map((applicant) => applicant.applicationId),
                          )
                        }
                      }}
                      style={{ flexShrink: 0 }}
                      c={colorScheme.colorScheme === 'dark' ? 'primary.3' : 'primary'}
                      size='xs'
                    >
                      {selectedApplicants.length === possibleInterviewApplicants.length
                        ? 'Deselect All'
                        : 'Select All'}
                    </Button>
                  </Group>
                  <ScrollArea.Autosize w={'100%'} type='hover' mih={'50px'} mah={'30vh'}>
                    {possibleInterviewApplicants.map((applicant, index) => (
                      <Stack
                        key={applicant.applicationId}
                        gap={0}
                        style={{ cursor: 'pointer' }}
                        onClick={() => {
                          if (selectedApplicants.includes(applicant.applicationId)) {
                            setSelectedApplicants(
                              selectedApplicants.filter((id) => id !== applicant.applicationId),
                            )
                          } else {
                            setSelectedApplicants([...selectedApplicants, applicant.applicationId])
                          }
                        }}
                        bg={
                          selectedApplicants.includes(applicant.applicationId)
                            ? colorScheme.colorScheme === 'dark'
                              ? 'primary.3'
                              : 'primary.0'
                            : undefined
                        }
                        c={
                          selectedApplicants.includes(applicant.applicationId)
                            ? colorScheme.colorScheme === 'dark'
                              ? 'primary.10'
                              : 'primary'
                            : undefined
                        }
                      >
                        <Group justify='space-between' align='center' p={'1rem'}>
                          <Group wrap='nowrap' justify='center' align='center' gap={'0.5rem'}>
                            {selectedApplicants.includes(applicant.applicationId) ? (
                              <CheckCircleIcon
                                size={24}
                                style={{ flexShrink: 0 }}
                                weight={'bold'}
                              />
                            ) : (
                              <CircleIcon size={24} style={{ flexShrink: 0 }} weight={'bold'} />
                            )}
                            <Text fw={500}>{applicant.applicantName}</Text>
                          </Group>
                          {applicant.state === ApplicationState.INTERVIEWING && (
                            <Badge color='gray' radius='sm'>
                              Already Invited
                            </Badge>
                          )}
                        </Group>
                        {index < possibleInterviewApplicants.length - 1 && <Divider />}
                      </Stack>
                    ))}
                  </ScrollArea.Autosize>
                </Stack>
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
