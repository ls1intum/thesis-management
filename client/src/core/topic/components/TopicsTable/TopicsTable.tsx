import type { DataTableColumn } from 'mantine-datatable'
import { DataTable } from 'mantine-datatable'
import React from 'react'
import { formatDate } from '@/core/utils/format'
import { useTopicsContext } from '@/core/topic/providers/TopicsProvider/hooks'
import type { ITopicOverview } from '@/core/topic/requests/responses/topic'
import { TopicState } from '@/core/topic/requests/responses/topic'
import { useNavigate } from 'react-router'
import { Badge, Center, Stack, Text } from '@mantine/core'
import AvatarUserList from '@/core/components/AvatarUserList/AvatarUserList'
import ThesisTypeBadge from '@/app/pages/LandingPage/components/ThesisTypBadge/ThesisTypBadge'

type TopicColumn =
  | 'title'
  | 'types'
  | 'supervisor'
  | 'examiner'
  | 'researchGroup'
  | 'state'
  | 'createdAt'
  | string

interface ITopicOverviewsTableProps {
  columns?: TopicColumn[]
  extraColumns?: Record<string, DataTableColumn<ITopicOverview>>
  noBorder?: boolean
}

const TopicsTable = (props: ITopicOverviewsTableProps) => {
  const {
    extraColumns = {},
    columns = ['title', 'types', 'examiner', 'supervisor'],
    noBorder = false,
  } = props

  const navigate = useNavigate()

  const { topics, page, setPage, limit, isLoading } = useTopicsContext()

  const openTopic = (topicId: string, openInNewTab: boolean) => {
    const url = `/topics/${topicId}`
    if (openInNewTab) {
      window.open(url, '_blank', 'noopener,noreferrer')
    } else {
      void navigate(url)
    }
  }

  const getTopicColor = (state: TopicState) => {
    switch (state) {
      case TopicState.OPEN:
        return 'green'
      case TopicState.CLOSED:
        return 'red'
      case TopicState.DRAFT:
        return 'yellow'
      case TopicState.EXPIRED:
        return 'orange'
      default:
        return 'gray'
    }
  }

  const columnConfig: Record<TopicColumn, DataTableColumn<ITopicOverview>> = {
    state: {
      accessor: 'state',
      title: 'State',
      textAlign: 'center',
      width: 100,
      render: (topic) => (
        <Center>
          <Badge color={getTopicColor(topic.state)} radius='sm'>
            {topic.state}
          </Badge>
        </Center>
      ),
    },
    title: {
      accessor: 'title',
      title: 'Title',
      cellsStyle: () => ({ minWidth: 200 }),
    },
    types: {
      accessor: 'thesisTypes',
      title: 'Thesis Types',
      width: 180,
      ellipsis: true,
      render: (topic) => (
        <Stack gap={2}>
          {topic.thesisTypes ? (
            topic.thesisTypes.map((type) => <ThesisTypeBadge type={type} key={type} />)
          ) : (
            <ThesisTypeBadge type='Any' key='any' />
          )}
        </Stack>
      ),
    },
    examiner: {
      accessor: 'examiner',
      title: 'Examiner',
      width: 180,
      ellipsis: true,
      render: (topic) => <AvatarUserList users={topic.examiners ?? []} />,
    },
    supervisor: {
      accessor: 'supervisor',
      title: 'Supervisor(s)',
      width: 180,
      ellipsis: true,
      render: (topic) => <AvatarUserList users={topic.supervisors ?? []} />,
    },
    researchGroup: {
      accessor: 'researchGroup.name',
      title: 'Research Group',
      width: 180,
      ellipsis: true,
      render: (topic) => (
        <Text size='sm' style={{ whiteSpace: 'normal', wordBreak: 'break-word' }}>
          {topic.researchGroup?.name ?? ''}
        </Text>
      ),
    },
    createdAt: {
      accessor: 'createdAt',
      title: 'Created At',
      width: 150,
      ellipsis: true,
      render: (record) => formatDate(record.createdAt),
    },
    ...extraColumns,
  }

  return (
    <DataTable
      fetching={isLoading}
      withTableBorder={!noBorder}
      minHeight={200}
      noRecordsText='No topics to show'
      borderRadius='sm'
      verticalSpacing='md'
      striped
      highlightOnHover
      totalRecords={topics?.totalElements ?? 0}
      recordsPerPage={limit}
      page={page + 1}
      onPageChange={(x) => setPage(x - 1)}
      records={topics?.content}
      idAccessor='topicId'
      columns={columns.map((column) => columnConfig[column])}
      onRowClick={({ record, event }) => {
        openTopic(record.topicId, event.metaKey || event.ctrlKey || event.shiftKey)
      }}
      customRowAttributes={(record) => ({
        onAuxClick: (event: React.MouseEvent) => {
          if (event.button === 1) {
            event.preventDefault()
            openTopic(record.topicId, true)
          }
        },
      })}
    />
  )
}

export default TopicsTable
