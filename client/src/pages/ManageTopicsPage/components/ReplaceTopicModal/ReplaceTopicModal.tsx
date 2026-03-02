import {
  Button,
  Center,
  Group,
  Loader,
  Modal,
  MultiSelect,
  Select,
  Stack,
  Text,
  TextInput,
} from '@mantine/core'
import { ITopic, TopicState, toTopicOverview } from '../../../../requests/responses/topic'
import { isNotEmpty, useForm } from '@mantine/form'
import { isNotEmptyUserList } from '../../../../utils/validation'
import { useEffect, useState } from 'react'
import { useTopic } from '../../../../hooks/fetcher'
import DocumentEditor from '../../../../components/DocumentEditor/DocumentEditor'
import { GLOBAL_CONFIG } from '../../../../config/global'
import { doRequest } from '../../../../requests/request'
import { showSimpleError, showSimpleSuccess } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { UserMultiSelect } from '../../../../components/UserMultiSelect/UserMultiSelect'
import { useTopicsContext } from '../../../../providers/TopicsProvider/hooks'
import { formatThesisType } from '../../../../utils/format'
import { PaginationResponse } from '../../../../requests/responses/pagination'
import { ILightResearchGroup } from '../../../../requests/responses/researchGroup'
import { useHasGroupAccess } from '../../../../hooks/authentication'
import { DateInput } from '@mantine/dates'

interface ICreateTopicModalProps {
  opened: boolean
  onClose: () => unknown
  topicId?: string
}

const ReplaceTopicModal = (props: ICreateTopicModalProps) => {
  const { topicId, opened, onClose } = props

  const fetchedTopic = useTopic(opened ? topicId : undefined)
  const topic = fetchedTopic === false ? undefined : fetchedTopic || undefined
  const fetchError = fetchedTopic === false && !!topicId
  const isTopicLoading = !!topicId && opened && fetchedTopic === undefined

  const { addTopic, updateTopic } = useTopicsContext()
  const [researchGroups, setResearchGroups] = useState<PaginationResponse<ILightResearchGroup>>()
  const hasAdminAccess = useHasGroupAccess('admin')

  const form = useForm<{
    title: string
    problemStatement: string
    requirements: string
    goals: string
    references: string
    thesisTypes: string[]
    examinerIds: string[]
    supervisorIds: string[]
    researchGroupId: string
    intendedStart: Date | undefined
    applicationDeadline: Date | undefined
  }>({
    mode: 'controlled',
    initialValues: {
      title: '',
      thesisTypes: [],
      problemStatement: '',
      requirements: '',
      goals: '',
      references: '',
      examinerIds: [],
      supervisorIds: [],
      researchGroupId: '',
      intendedStart: undefined,
      applicationDeadline: undefined,
    },
    validateInputOnBlur: true,
    validate: {
      title: isNotEmpty('Title is required'),
      problemStatement: isNotEmpty('Problem statement is required'),
      examinerIds: isNotEmptyUserList('examiner'),
      supervisorIds: isNotEmptyUserList('supervisor'),
      researchGroupId: isNotEmpty('Research group is required'),
    },
  })

  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (opened && topic) {
      form.setInitialValues({
        title: topic.title,
        thesisTypes: topic.thesisTypes || [],
        problemStatement: topic.problemStatement ?? '',
        requirements: topic.requirements ?? '',
        goals: topic.goals ?? '',
        references: topic.references ?? '',
        examinerIds: (topic.examiners ?? []).map((examiner) => examiner.userId),
        supervisorIds: (topic.supervisors ?? []).map((supervisor) => supervisor.userId),
        researchGroupId: topic.researchGroup.id,
        intendedStart: topic.intendedStart ? new Date(topic.intendedStart) : undefined,
        applicationDeadline: topic.applicationDeadline
          ? new Date(topic.applicationDeadline)
          : undefined,
      })
    }

    form.reset()
  }, [topic, opened])

  useEffect(() => {
    if (!hasAdminAccess && topic?.researchGroup) {
      setResearchGroups({
        content: [topic.researchGroup],
        totalPages: 1,
        totalElements: 1,
        last: true,
        pageNumber: 0,
        pageSize: -1,
      })
      form.setValues({ researchGroupId: topic?.researchGroup.id })
      return
    }
    setLoading(true)
    return doRequest<PaginationResponse<ILightResearchGroup>>(
      '/v2/research-groups',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          page: 0,
          limit: -1,
        },
      },
      (res) => {
        if (res.ok) {
          setResearchGroups({
            ...res.data,
            content: res.data.content,
          })

          if ((res.data.content ?? []).length === 1) {
            form.setValues({
              researchGroupId: (res.data.content ?? [])[0].id,
              examinerIds: [(res.data.content ?? [])[0].head.userId],
            })
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))

          setResearchGroups({
            content: [],
            totalPages: 0,
            totalElements: 0,
            last: true,
            pageNumber: 0,
            pageSize: -1,
          })
        }
        setLoading(false)
      },
    )
  }, [opened])

  const onSubmit = async (isDraft = false) => {
    setLoading(true)

    try {
      const response = await doRequest<ITopic>(`/v2/topics${topic ? `/${topic.topicId}` : ''}`, {
        method: topic ? 'PUT' : 'POST',
        requiresAuth: true,
        data: {
          title: form.values.title,
          thesisTypes: form.values.thesisTypes.length > 0 ? form.values.thesisTypes : null,
          problemStatement: form.values.problemStatement,
          requirements: form.values.requirements,
          goals: form.values.goals,
          references: form.values.references,
          examinerIds: form.values.examinerIds,
          supervisorIds: form.values.supervisorIds,
          researchGroupId: form.values.researchGroupId,
          intendedStart: form.values.intendedStart ?? null,
          applicationDeadline: form.values.applicationDeadline ?? null,
          isDraft: isDraft,
        },
      })

      if (response.ok) {
        const overview = toTopicOverview(response.data)
        if (topic) {
          updateTopic(overview)
        } else {
          addTopic(overview)
        }

        onClose()

        showSimpleSuccess(topic ? 'Topic updated successfully' : 'Topic created successfully')
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      size='xl'
      title={fetchError ? 'Failed to load topic' : topic ? 'Edit Topic' : 'Create Topic'}
      opened={opened}
      onClose={onClose}
    >
      {fetchError ? (
        <Center py='xl'>
          <Text c='dimmed'>Could not load topic data.</Text>
        </Center>
      ) : isTopicLoading ? (
        <Center py='xl'>
          <Loader size={32} />
        </Center>
      ) : (
        <form onSubmit={form.onSubmit(() => onSubmit(false))}>
          <Stack gap='md'>
            <TextInput label='Title' required {...form.getInputProps('title')} />
            <MultiSelect
              label='Thesis Types'
              placeholder={form.values.thesisTypes.length > 0 ? undefined : 'All Thesis Types'}
              data={Object.keys(GLOBAL_CONFIG.thesis_types).map((key) => ({
                value: key,
                label: formatThesisType(key),
              }))}
              {...form.getInputProps('thesisTypes')}
            />
            <UserMultiSelect
              label='Examiner'
              required
              groups={['supervisor']}
              initialUsers={topic?.examiners ?? []}
              maxValues={1}
              {...form.getInputProps('examinerIds')}
            />
            <UserMultiSelect
              label='Supervisor(s)'
              required
              groups={['advisor', 'supervisor']}
              initialUsers={topic?.supervisors ?? []}
              {...form.getInputProps('supervisorIds')}
            />
            <Select
              label='Research Group'
              required
              nothingFoundMessage={!loading ? 'Nothing found...' : 'Loading...'}
              disabled={loading || !researchGroups || !hasAdminAccess}
              data={(researchGroups?.content ?? []).map((researchGroup: ILightResearchGroup) => ({
                label: researchGroup.name,
                value: researchGroup.id,
              }))}
              {...form.getInputProps('researchGroupId')}
            />
            <DateInput
              clearable
              minDate={new Date()}
              label='Intended Start'
              placeholder='Select intended start date'
              value={form.values.intendedStart ?? null}
              onChange={(date) => {
                form.setFieldValue('intendedStart', date ? new Date(date) : undefined)
              }}
            />
            <DateInput
              clearable
              minDate={new Date()}
              label='Application Deadline'
              placeholder='Select application deadline'
              value={form.values.applicationDeadline ?? null}
              onChange={(date) => {
                form.setFieldValue('applicationDeadline', date ? new Date(date) : undefined)
              }}
            />
            <DocumentEditor
              label='Problem Statement'
              required
              editMode={true}
              {...form.getInputProps('problemStatement')}
            />
            <DocumentEditor
              label='Requirements'
              editMode={true}
              {...form.getInputProps('requirements')}
            />
            <DocumentEditor label='Goals' editMode={true} {...form.getInputProps('goals')} />
            <DocumentEditor
              label='References'
              editMode={true}
              {...form.getInputProps('references')}
            />
            <Group>
              {(!topic || topic.state === TopicState.DRAFT) && (
                <Button
                  variant='default'
                  onClick={() => onSubmit(true)}
                  disabled={!form.isValid()}
                  loading={loading}
                >
                  {topic ? 'Save Changes' : 'Create Draft'}
                </Button>
              )}
              <Button type='submit' flex={1} disabled={!form.isValid()} loading={loading}>
                {topic
                  ? topic.state === TopicState.DRAFT
                    ? 'Save & Create Topic'
                    : 'Save Changes'
                  : 'Create Topic'}
              </Button>
            </Group>
          </Stack>
        </form>
      )}
    </Modal>
  )
}

export default ReplaceTopicModal
