import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import {
  Alert,
  Badge,
  Center,
  Checkbox,
  Group,
  Input,
  Pagination,
  Progress,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Tooltip,
} from '@mantine/core'
import type { IThesis } from '@/thesis/requests/responses/thesis'
import { ThesisFeedbackSource } from '@/thesis/requests/responses/thesis'
import React from 'react'
import AvatarUser from '@/core/components/AvatarUser/AvatarUser'
import { formatDate } from '@/core/utils/format'
import { doRequest } from '@/core/requests/request'
import { ApiError } from '@/core/requests/handler'
import { MagnifyingGlass, Trash } from '@phosphor-icons/react'
import ConfirmationButton from '@/core/components/ConfirmationButton/ConfirmationButton'
import FeedbackCategoryCounts from '@/thesis/components/FeedbackCategoryCounts/FeedbackCategoryCounts'
import {
  ASSESSMENT_COLOR,
  ASSESSMENT_LABEL,
  CATEGORY_DESCRIPTION,
  countByCategory,
  FEEDBACK_CATEGORY_OPTIONS,
  humanizeFeedbackCategory,
  SEVERITY_COLOR,
  SEVERITY_DESCRIPTION,
  SOURCE_COLOR,
  SOURCE_DESCRIPTION,
  SOURCE_LABEL,
} from '@/thesis/utils/feedbackLabels'

const PAGE_SIZE = 10

type ResolvedFilter = 'ALL' | 'OPEN' | 'RESOLVED'

interface IThesisFeedbackOverviewProps {
  type: string
  allowEdit: boolean
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

/**
 * The id of the newest revision of the document this feedback section is about — the current
 * proposal for PROPOSAL, the current thesis file for THESIS. Both lists are newest-first.
 */
const resolveLatestVersionId = (
  type: string,
  proposals: IThesis['proposals'] | undefined,
  files: IThesis['files'] | undefined,
): string | undefined => {
  if (type === 'PROPOSAL') {
    return (proposals ?? [])[0]?.proposalId
  }
  if (type === 'THESIS') {
    return (files ?? []).find((file) => file.type === 'THESIS')?.fileId
  }
  return undefined
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

  const latestVersionId = React.useMemo(
    () => resolveLatestVersionId(type, thesis.proposals, thesis.files),
    [type, thesis.proposals, thesis.files],
  )

  // A summary describes one specific revision, so it only stands for the document currently on
  // screen. Once a newer proposal or thesis file is uploaded (or for legacy rows that predate
  // the version column and cannot be placed), drop it rather than passing off an obsolete score
  // as the current one — the next AI review writes a summary for the new revision.
  const reviewSummary = React.useMemo(
    () =>
      (thesis.aiReviewSummaries ?? []).find(
        (summary) =>
          summary.type === type &&
          Boolean(latestVersionId) &&
          summary.documentVersionId === latestVersionId,
      ),
    [thesis.aiReviewSummaries, type, latestVersionId],
  )

  const categoryCounts = React.useMemo(() => countByCategory(feedbackForType), [feedbackForType])

  const addressedCount = React.useMemo(
    () => feedbackForType.filter((item) => item.completedAt).length,
    [feedbackForType],
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

  // Say where the score came from. It is the AI's read of one specific revision at one point in
  // time, not a live verdict on the feedback list it sits above — supervisors keep adding entries
  // afterwards without the score moving, so the alert has to date itself.
  const summaryVersionLabel = reviewSummary?.documentVersionId
    ? versionLabelById.get(reviewSummary.documentVersionId)
    : undefined
  const summaryProvenance = [
    summaryVersionLabel ? `AI review of ${summaryVersionLabel}` : 'AI review',
    reviewSummary?.updatedAt ? formatDate(reviewSummary.updatedAt, { withTime: false }) : '',
  ]
    .filter(Boolean)
    .join(' · ')

  const summaryAlert =
    reviewSummary && (Boolean(reviewSummary.summary) || typeof reviewSummary.score === 'number') ? (
      <Alert
        color={reviewSummary.assessment ? ASSESSMENT_COLOR[reviewSummary.assessment] : 'gray'}
        variant='light'
        title={
          typeof reviewSummary.score === 'number'
            ? `Overall Score: ${reviewSummary.score}/100`
            : 'AI review summary'
        }
      >
        <Stack gap={4}>
          {reviewSummary.assessment && (
            <Text size='sm' fw={500}>
              {ASSESSMENT_LABEL[reviewSummary.assessment]}
            </Text>
          )}
          {reviewSummary.summary && (
            <Text size='sm' c='dimmed'>
              {reviewSummary.summary}
            </Text>
          )}
          <Text size='xs' c='dimmed'>
            {summaryProvenance}
          </Text>
        </Stack>
      </Alert>
    ) : null

  // A review that flags nothing still records a score, so an empty feedback list does not mean
  // there is nothing to show — just nothing to count, filter, or tabulate.
  if (feedbackForType.length === 0) {
    return summaryAlert ? <Input.Wrapper label='Feedback'>{summaryAlert}</Input.Wrapper> : null
  }

  return (
    <Input.Wrapper label='Feedback'>
      <Stack gap='sm'>
        {summaryAlert}
        <Group gap='lg' align='center' wrap='wrap'>
          <Group gap={6} align='center'>
            <Text size='sm' fw={500}>
              {addressedCount}/{feedbackForType.length} addressed
            </Text>
            <Progress.Root size={8} radius='xl' style={{ width: 120 }}>
              <Progress.Section
                value={feedbackForType.length ? (addressedCount / feedbackForType.length) * 100 : 0}
                color='green'
              />
              <Progress.Section
                value={
                  feedbackForType.length
                    ? ((feedbackForType.length - addressedCount) / feedbackForType.length) * 100
                    : 0
                }
                color='gray.3'
              />
            </Progress.Root>
          </Group>
          <FeedbackCategoryCounts counts={categoryCounts} />
        </Group>
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
            data={FEEDBACK_CATEGORY_OPTIONS}
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
                      <Text
                        td={item.completedAt ? 'line-through' : undefined}
                        c={item.completedAt ? 'dimmed' : undefined}
                      >
                        {item.feedback}
                      </Text>
                      <Group gap={4}>
                        {item.completedAt && (
                          <Badge size='sm' color='green' variant='light'>
                            Addressed
                          </Badge>
                        )}
                        {item.severity && (
                          <Tooltip
                            label={SEVERITY_DESCRIPTION[item.severity]}
                            withArrow
                            openDelay={300}
                          >
                            <Badge size='sm' color={SEVERITY_COLOR[item.severity]} variant='light'>
                              {item.severity}
                            </Badge>
                          </Tooltip>
                        )}
                        {item.category && (
                          <Tooltip
                            label={CATEGORY_DESCRIPTION[item.category]}
                            withArrow
                            openDelay={300}
                          >
                            <Badge size='sm' color='gray' variant='outline'>
                              {humanizeFeedbackCategory(item.category)}
                            </Badge>
                          </Tooltip>
                        )}
                        {item.generationSource &&
                          item.generationSource !== ThesisFeedbackSource.HUMAN && (
                            <Tooltip
                              label={SOURCE_DESCRIPTION[item.generationSource]}
                              withArrow
                              openDelay={300}
                            >
                              <Badge
                                size='sm'
                                color={SOURCE_COLOR[item.generationSource]}
                                variant='light'
                              >
                                {SOURCE_LABEL[item.generationSource]}
                              </Badge>
                            </Tooltip>
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
