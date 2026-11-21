import { Badge, Group, Tooltip } from '@mantine/core'
import React from 'react'

import type { ThesisFeedbackCategory } from '@/thesis/requests/responses/thesis'
import { CATEGORY_DESCRIPTION, humanizeFeedbackCategory } from '@/thesis/utils/feedbackLabels'

interface IFeedbackCategoryCountsProps {
  counts: Map<ThesisFeedbackCategory, number>
}

/**
 * "Structure: 3  Citation: 1" badges summarising how feedback items are spread over the
 * categories. Rendered identically for saved feedback (overview) and unsaved AI drafts
 * (request-changes dialog), so it lives here rather than in either of them.
 */
const FeedbackCategoryCounts = (props: IFeedbackCategoryCountsProps) => {
  const { counts } = props

  if (counts.size === 0) {
    return null
  }

  return (
    <Group gap={6}>
      {Array.from(counts.entries()).map(([category, count]) => (
        <Tooltip key={category} label={CATEGORY_DESCRIPTION[category]} withArrow openDelay={300}>
          <Badge size='sm' color='gray' variant='outline'>
            {humanizeFeedbackCategory(category)}: {count}
          </Badge>
        </Tooltip>
      ))}
    </Group>
  )
}

export default FeedbackCategoryCounts
