import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import {
  Badge,
  Center,
  Checkbox,
  Group,
  Input,
  Pagination,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
} from '@mantine/core'
import type { IThesis } from '@/thesis/requests/responses/thesis'
import {
  ThesisFeedbackCategory,
  ThesisFeedbackSeverity,
  ThesisFeedbackSource,
} from '@/thesis/requests/responses/thesis'
import React from 'react'
import AvatarUser from '@/core/components/AvatarUser/AvatarUser'
import { formatDate } from '@/core/utils/format'
import { doRequest } from '@/core/requests/request'
import { ApiError } from '@/core/requests/handler'
import { MagnifyingGlass, Trash } from '@phosphor-icons/react'
import ConfirmationButton from '@/core/components/ConfirmationButton/ConfirmationButton'

const PAGE_SIZE = 10

type ResolvedFilter = 'ALL' | 'OPEN' | 'RESOLVED'

interface IThesisFeedbackOverviewProps {
  type: string
  allowEdit: boolean
}

const SEVERITY_COLOR: Record<ThesisFeedbackSeverity, string> = {
  [ThesisFeedbackSeverity.CRITICAL]: 'red',
  [ThesisFeedbackSeverity.MAJOR]: 'orange',
  [ThesisFeedbackSeverity.MINOR]: 'yellow',
  [ThesisFeedbackSeverity.SUGGESTION]: 'blue',
}

const SOURCE_LABEL: Record<ThesisFeedbackSource, string> = {
  [ThesisFeedbackSource.AI]: 'AI',
  [ThesisFeedbackSource.HUMAN]: 'Instructor',
  [ThesisFeedbackSource.AI_REVIEWED_BY_HUMAN]: 'AI + Instructor',
}

const SOURCE_COLOR: Record<ThesisFeedbackSource, string> = {
  [ThesisFeedbackSource.AI]: 'grape',
  [ThesisFeedbackSource.HUMAN]: 'gray',
  [ThesisFeedbackSource.AI_REVIEWED_BY_HUMAN]: 'teal',
}

const humanizeCategory = (value: ThesisFeedbackCategory | string | null | undefined): string => {
  if (!value) return ''
  return String(value)
    .toLowerCase()
    .replace(/(^\w|_\w)/g, (m) => m.replace('_', ' ').toUpperCase())
}

/**
 * Build a "documentVersionId → v{n}" map so each feedback row can render the revision it was
 * written against. Proposals and thesis files are both stored newest-first, so the LAST entry
 * in each list is v1 and the FIRST is the current version.
 */
const buildVersionLabelMap = (
  proposals: IThesis['proposals'] | undefined,
  files: IThesis['files'] | undefined,
): Map<string, string> => {
  const map = new Map<string, string>()
  const proposalList = proposals ?? []
  proposalList.forEach((proposal, index) => {
    map.set(proposal.proposalId, `v${proposalList.length - index}`)
  })
  const thesisFiles = (files ?? []).filter((file) => file.type === 'THESIS')
  thesisFiles.forEach((file, index) => {
    map.set(file.fileId, `v${thesisFiles.length - index}`)
  })
  return map
}

const ThesisFeedbackOverview = (props: IThesisFeedbackOverviewProps) => {
  const { type, allowEdit } = props

  const { thesis, access } = useLoadedThesisContext()
  const versionLabelById = React.useMemo(
    () => buildVersionLabelMap(thesis.proposals, thesis.files),
    [thesis.proposals, thesis.files],
  )

  const [loading, toggleFeedback] = useThesisUpdateAction(
    async (feedback: NonNullable<IThesis['feedback']>[number]) => {
      const response = await doRequest<IThesis>(
        `/v2/theses/${thesis.thesisId}/feedback/${feedback.feedbackId}/${feedback.completedAt ? 'request' : 'complete'}`,
        {
          method: 'PUT',
          requiresAuth: true,
        },
      )

      if (response.ok) {
        return response.data
      } else {
        throw new ApiError(response)
      }
    },
    'Feedback state successfully changed',
  )

  const [deleting, deleteFeedback] = useThesisUpdateAction(
    async (feedback: NonNullable<IThesis['feedback']>[number]) => {
      const response = await doRequest<IThesis>(
        `/v2/theses/${thesis.thesisId}/feedback/${feedback.feedbackId}`,
        {
          method: 'DELETE',
          requiresAuth: true,
        },
      )

      if (response.ok) {
        return response.data
      } else {
        throw new ApiError(response)
      }
    },
    'Feedback successfully deleted',
  )

  const feedbackForType = React.useMemo(
    () => (thesis.feedback ?? []).filter((item) => item.type === type),
    [thesis.feedback, type],
  )

  const [search, setSearch] = React.useState('')
  const [categoryFilter, setCategoryFilter] = React.useState<string | null>(null)
  const [resolvedFilter, setResolvedFilter] = React.useState<ResolvedFilter>('ALL')
  const [page, setPage] = React.useState(1)

  const filteredItems = React.useMemo(() => {
    const query = search.trim().toLowerCase()
    return feedbackForType.filter((item) => {
      if (query && !item.feedback.toLowerCase().includes(query)) {
        return false
      }
      if (categoryFilter && item.category !== categoryFilter) {
        return false
      }
      if (resolvedFilter === 'OPEN' && item.completedAt) {
        return false
      }
      if (resolvedFilter === 'RESOLVED' && !item.completedAt) {
        return false
      }
      return true
    })
  }, [feedbackForType, search, categoryFilter, resolvedFilter])

  React.useEffect(() => {
    setPage(1)
  }, [search, categoryFilter, resolvedFilter, feedbackForType.length])

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE))
  const clampedPage = Math.min(page, totalPages)
  const visibleItems = filteredItems.slice((clampedPage - 1) * PAGE_SIZE, clampedPage * PAGE_SIZE)

  if (feedbackForType.length === 0) {
    return null
  }

  const categoryOptions = Object.values(ThesisFeedbackCategory).map((value) => ({
    value,
    label: humanizeCategory(value),
  }))

  return (
    <Input.Wrapper label='Feedback'>
      <Stack gap='sm'>
        <Group gap='sm' align='flex-end' wrap='wrap'>
          <TextInput
            style={{ flex: 1, minWidth: 200 }}
            placeholder='Search feedback...'
            leftSection={<MagnifyingGlass size={16} />}
            value={search}
            onChange={(event) => setSearch(event.currentTarget.value)}
          />
          <Select
            placeholder='All categories'
            data={categoryOptions}
            value={categoryFilter}
            onChange={setCategoryFilter}
            clearable
            w={200}
          />
          <Select
            data={[
              { value: 'ALL', label: 'All' },
              { value: 'OPEN', label: 'Open' },
              { value: 'RESOLVED', label: 'Resolved' },
            ]}
            value={resolvedFilter}
            onChange={(value) => setResolvedFilter((value as ResolvedFilter) ?? 'ALL')}
            allowDeselect={false}
            w={160}
          />
        </Group>
        <Table.ScrollContainer minWidth={600}>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th></Table.Th>
                <Table.Th>Requested Change</Table.Th>
                <Table.Th>Requested By</Table.Th>
                <Table.Th>Requested At</Table.Th>
                <Table.Th></Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {visibleItems.length === 0 && (
                <Table.Tr>
                  <Table.Td colSpan={5}>
                    <Text c='dimmed' ta='center' py='sm'>
                      No feedback matches the current filters.
                    </Text>
                  </Table.Td>
                </Table.Tr>
              )}
              {visibleItems.map((item) => (
                <Table.Tr key={item.feedbackId}>
                  <Table.Td ta='center' width={50}>
                    <Checkbox
                      checked={Boolean(item.completedAt)}
                      disabled={loading || !access.student || !allowEdit}
                      onChange={() => toggleFeedback(item)}
                    />
                  </Table.Td>
                  <Table.Td>
                    <Stack gap={4}>
                      <Text>{item.feedback}</Text>
                      <Group gap={4}>
                        {item.severity && (
                          <Badge size='sm' color={SEVERITY_COLOR[item.severity]} variant='light'>
                            {item.severity}
                          </Badge>
                        )}
                        {item.category && (
                          <Badge size='sm' color='gray' variant='outline'>
                            {humanizeCategory(item.category)}
                          </Badge>
                        )}
                        {item.generationSource &&
                          item.generationSource !== ThesisFeedbackSource.HUMAN && (
                            <Badge
                              size='sm'
                              color={SOURCE_COLOR[item.generationSource]}
                              variant='light'
                            >
                              {SOURCE_LABEL[item.generationSource]}
                            </Badge>
                          )}
                        {item.documentVersionId && versionLabelById.get(item.documentVersionId) && (
                          <Badge size='sm' color='blue' variant='outline'>
                            {versionLabelById.get(item.documentVersionId)}
                          </Badge>
                        )}
                      </Group>
                    </Stack>
                  </Table.Td>
                  <Table.Td width={200}>
                    <AvatarUser user={item.requestedBy} />
                  </Table.Td>
                  <Table.Td width={170}>{formatDate(item.requestedAt)}</Table.Td>
                  <Table.Td width={80}>
                    {access.supervisor && (
                      <Center>
                        <ConfirmationButton
                          size='xs'
                          loading={deleting}
                          confirmationTitle='Delete feedback?'
                          confirmationText='This will permanently remove this feedback entry. Continue?'
                          onClick={() => deleteFeedback(item)}
                        >
                          <Trash />
                        </ConfirmationButton>
                      </Center>
                    )}
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
        {filteredItems.length > PAGE_SIZE && (
          <Group justify='space-between' align='center'>
            <Text size='sm' c='dimmed'>
              {(clampedPage - 1) * PAGE_SIZE + 1}–
              {Math.min(clampedPage * PAGE_SIZE, filteredItems.length)} of {filteredItems.length}
            </Text>
            <Pagination
              value={clampedPage}
              onChange={setPage}
              total={totalPages}
              size='sm'
              siblings={1}
            />
          </Group>
        )}
      </Stack>
    </Input.Wrapper>
  )
}

export default ThesisFeedbackOverview
