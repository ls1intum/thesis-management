import { DataTable, DataTableColumn } from 'mantine-datatable'
import { formatDate, formatThesisType } from '../../utils/format'
import { useTopicsContext } from '../../providers/TopicsProvider/hooks'
import { ITopic } from '../../requests/responses/topic'
import { useNavigate } from 'react-router'
import { Badge, Center, Stack, Text } from '@mantine/core'
import AvatarUserList from '../AvatarUserList/AvatarUserList'
import React from 'react'

type TopicColumn = 'title' | 'types' | 'advisor' | 'supervisor' | 'state' | 'createdAt' | string

interface ITopicsTableProps {
  columns?: TopicColumn[]
  extraColumns?: Record<string, DataTableColumn<ITopic>>
  noBorder?: boolean
}

const TopicsTable = (props: ITopicsTableProps) => {
  const {
    extraColumns = {},
    columns = ['title', 'types', 'supervisor', 'advisor'],
    noBorder = false,
  } = props

  const navigate = useNavigate()

  const { topics, page, setPage, limit } = useTopicsContext()

  const columnConfig: Record<TopicColumn, DataTableColumn<ITopic>> = {
    state: {
      accessor: 'state',
      title: 'State',
      textAlign: 'center',
      width: 100,
      render: (topic) => (
        <Center>
          {topic.closedAt ? <Badge color='red'>Closed</Badge> : <Badge color='gray'>Open</Badge>}
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
      width: 150,
      ellipsis: true,
      render: (topic) => (
        <Stack gap={2}>
          {topic.thesisTypes ? (
            topic.thesisTypes.map((type) => (
              <Text key={type} size='sm'>
                {formatThesisType(type)}
              </Text>
            ))
          ) : (
            <Text size='sm'>Any</Text>
          )}
        </Stack>
      ),
    },
    supervisor: {
      accessor: 'supervisor',
      title: 'Supervisor',
      width: 180,
      ellipsis: true,
      render: (topic) => <AvatarUserList users={topic.supervisors} />,
    },
    advisor: {
      accessor: 'advisor',
      title: 'Advisor(s)',
      width: 180,
      ellipsis: true,
      render: (topic) => <AvatarUserList users={topic.advisors} />,
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
      fetching={!topics}
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
      onRowClick={({ record }) => navigate(`/topics/${record.topicId}`)}
    />
  )
}

export default TopicsTable
