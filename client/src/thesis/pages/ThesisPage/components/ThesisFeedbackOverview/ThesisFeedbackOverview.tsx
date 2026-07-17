import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import { Badge, Center, Checkbox, Group, Input, Stack, Table, Text } from '@mantine/core'
import type { IThesis, ThesisFeedbackCategory } from '@/thesis/requests/responses/thesis'
import { ThesisFeedbackSeverity, ThesisFeedbackSource } from '@/thesis/requests/responses/thesis'
import React from 'react'
import AvatarUser from '@/core/components/AvatarUser/AvatarUser'
import { formatDate } from '@/core/utils/format'
import { doRequest } from '@/core/requests/request'
import { ApiError } from '@/core/requests/handler'
import { Trash } from '@phosphor-icons/react'
import ConfirmationButton from '@/core/components/ConfirmationButton/ConfirmationButton'

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

  const feedbackItems = thesis.feedback ?? []

  if (feedbackItems.length === 0) {
    return null
  }

  return (
    <Input.Wrapper label='Feedback'>
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
            {feedbackItems
              .filter((item) => item.type === type)
              .map((item) => (
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
    </Input.Wrapper>
  )
}

export default ThesisFeedbackOverview
