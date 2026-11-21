import { ThesisStateColor } from '@/core/config/colors'
import { formatThesisState } from '@/core/utils/format'
import { Badge } from '@mantine/core'
import React from 'react'
import type { ThesisState } from '@/thesis/requests/responses/thesis'

interface IThesisStateBadgeProps {
  state: ThesisState
}

const ThesisStateBadge = (props: IThesisStateBadgeProps) => {
  const { state } = props

  return <Badge color={ThesisStateColor[state] ?? 'gray'}>{formatThesisState(state)}</Badge>
}

export default ThesisStateBadge
