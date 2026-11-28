import { Badge, List, Stack, Text, Tooltip } from '@mantine/core'

interface IFeedbackTextLookup {
  feedbackId: string
  feedback: string
}

interface IFollowUpBadgeProps {
  followsUpId?: string | null
  deferredToSupervisor?: boolean
  // The existing feedback of the thesis, used to show the text of the entry being followed up.
  feedback: IFeedbackTextLookup[]
}

/**
 * Marks an entry that reports a defect from an earlier, already-addressed entry as recurring in a
 * newer version. A deferral is the follow-up that hands a repeatedly recurring defect to the
 * supervisor instead of explaining it once more. Renders nothing for an ordinary entry.
 */
export const FollowUpBadge = (props: IFollowUpBadgeProps) => {
  const { followsUpId, deferredToSupervisor, feedback } = props

  if (!followsUpId) {
    return null
  }

  const original = feedback.find((item) => item.feedbackId === followsUpId)
  const label = deferredToSupervisor ? 'Discuss with supervisor' : 'Follow-up'
  const explanation = deferredToSupervisor
    ? 'This issue kept recurring after written feedback, so it is best talked through with your supervisor.'
    : 'This issue was marked as addressed earlier, but it is still present in a newer version.'

  return (
    <Tooltip
      label={original ? `${explanation} Earlier entry: “${original.feedback}”` : explanation}
      withArrow
      multiline
      w={320}
      openDelay={300}
    >
      <Badge size='sm' color={deferredToSupervisor ? 'orange' : 'indigo'} variant='light'>
        {label}
      </Badge>
    </Tooltip>
  )
}

interface IResolvedFeedbackListProps {
  resolvedFeedbackIds?: string[] | null
  feedback: IFeedbackTextLookup[]
}

/**
 * Lists the earlier feedback entries an AI review found resolved in the reviewed version, so the
 * student sees what they fixed and not only what is still wrong. Renders nothing when there are
 * none, or when none of them still exist.
 */
export const ResolvedFeedbackList = (props: IResolvedFeedbackListProps) => {
  const { resolvedFeedbackIds, feedback } = props

  const resolved = (resolvedFeedbackIds ?? [])
    .map((id) => feedback.find((item) => item.feedbackId === id))
    .filter((item): item is IFeedbackTextLookup => Boolean(item))

  if (resolved.length === 0) {
    return null
  }

  return (
    <Stack gap={2}>
      <Text size='sm' fw={500} c='green'>
        Resolved since the last review
      </Text>
      <List size='sm' spacing={2}>
        {resolved.map((item) => (
          <List.Item key={item.feedbackId}>
            <Text size='sm' c='dimmed' lineClamp={2}>
              {item.feedback}
            </Text>
          </List.Item>
        ))}
      </List>
    </Stack>
  )
}
