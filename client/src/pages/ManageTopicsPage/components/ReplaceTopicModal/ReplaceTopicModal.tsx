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
import type { ITopic } from '../../../../requests/responses/topic'
import { TopicState, toTopicOverview } from '../../../../requests/responses/topic'
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
import type { PaginationResponse } from '../../../../requests/responses/pagination'
import type { ILightResearchGroup } from '../../../../requests/responses/researchGroup'
import type { ILightUser } from '../../../../requests/responses/user'
import { useHasGroupAccess, useUser } from '../../../../hooks/authentication'
import { DateInput } from '@mantine/dates'

interface ICreateTopicModalProps {
  opened: boolean
  onClose: () => unknown
  topicId?: string
}

const ReplaceTopicModal = (props: ICreateTopicModalProps) => {
  const { topicId, opened, onClose } = props

  const fetchedTopic = useTopic(opened ? topicId : undefined)
  const topic = fetchedTopic === false ? undefined : (fetchedTopic ?? undefined)
  const fetchError = fetchedTopic === false && Boolean(topicId)
  const isTopicLoading = Boolean(topicId) && opened && fetchedTopic === undefined

  const { addTopic, updateTopic } = useTopicsContext()
  const [researchGroups, setResearchGroups] = useState<PaginationResponse<ILightResearchGroup>>()
  const [autoSelectedExaminers, setAutoSelectedExaminers] = useState<ILightUser[]>([])
  const hasAdminAccess = useHasGroupAccess('admin')
  const currentUser = useUser()
  // Only admins can actually move a topic between research groups: the server
  // ties every non-admin to a single research group and rejects target groups
  // outside that one (see CurrentUserProvider.assertCanAccessResearchGroup).
  // For everyone else we render the field as read-only.
  const lockedResearchGroupName = topic?.researchGroup?.name ?? currentUser?.researchGroupName ?? ''
  const lockedResearchGroupId = topic?.researchGroup?.id ?? currentUser?.researchGroupId ?? ''

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
        thesisTypes: topic.thesisTypes ?? [],
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
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- form is stable; including it would loop on every form value change
  }, [topic, opened])

  useEffect(() => {
    if (!opened) {
      return
    }
    // AuthenticationProvider initializes `user` as undefined while
    // /v2/user-info is loading; without this guard we would briefly treat
    // an admin as a non-admin and pin the field.
    if (!currentUser) {
      return
    }

    if (!hasAdminAccess) {
      setResearchGroups(undefined)
      if (lockedResearchGroupId) {
        form.setValues({ researchGroupId: lockedResearchGroupId })
      }
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

          // Only auto-select the single available group (and its head as
          // examiner) on the create flow. When editing an existing topic
          // (`topic` is set), we must NOT overwrite the topic's existing
          // examiners.
          if (!topic && (res.data.content ?? []).length === 1) {
            const onlyGroup = (res.data.content ?? [])[0]
            form.setValues({
              researchGroupId: onlyGroup.id,
              examinerIds: onlyGroup.head?.userId ? [onlyGroup.head.userId] : [],
            })
            setAutoSelectedExaminers(onlyGroup.head ? [onlyGroup.head] : [])
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
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- form is stable; we re-run when admin status, the current user, or the locked RG id changes
  }, [opened, hasAdminAccess, currentUser?.userId, lockedResearchGroupId])

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
              initialUsers={topic?.examiners ?? autoSelectedExaminers}
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
            {hasAdminAccess ? (
              <Select
                label='Research Group'
                required
                nothingFoundMessage={!loading ? 'Nothing found...' : 'Loading...'}
                disabled={loading || !researchGroups}
                data={(researchGroups?.content ?? []).map((researchGroup: ILightResearchGroup) => ({
                  label: researchGroup.name,
                  value: researchGroup.id,
                }))}
                {...form.getInputProps('researchGroupId')}
              />
            ) : (
              <TextInput
                label='Research Group'
                description='Only administrators can move a topic to a different research group.'
                readOnly
                value={lockedResearchGroupName}
              />
            )}
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
                  onClick={() => void onSubmit(true)}
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
