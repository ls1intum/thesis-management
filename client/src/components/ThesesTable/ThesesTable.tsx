import { useAutoAnimate } from '@formkit/auto-animate/react'
import { DataTable, DataTableColumn } from 'mantine-datatable'
import { formatDate, formatUser } from '../../utils/format'
import React from 'react'
import { useThesesContext } from '../../contexts/ThesesProvider/hooks'
import { IThesesSort } from '../../contexts/ThesesProvider/context'
import { useNavigate } from 'react-router-dom'
import { IThesis } from '../../requests/responses/thesis'
import { GLOBAL_CONFIG } from '../../config/global'
import ThesisStateBadge from '../ThesisStateBadge/ThesisStateBadge'

type ThesisColumn =
  | 'state'
  | 'supervisors'
  | 'advisors'
  | 'students'
  | 'type'
  | 'title'
  | 'start_date'
  | 'end_date'

interface IThesesTableProps {
  columns?: ThesisColumn[]
}

const ThesesTable = (props: IThesesTableProps) => {
  const { columns = ['state', 'students', 'advisors', 'type', 'title', 'start_date', 'end_date'] } =
    props

  const [bodyRef] = useAutoAnimate<HTMLTableSectionElement>()

  const { theses, sort, setSort, page, setPage, limit } = useThesesContext()

  const navigate = useNavigate()

  const onThesisClick = (thesis: IThesis) => {
    navigate(`/theses/${thesis.thesisId}`)
  }

  const columnConfig: Record<ThesisColumn, DataTableColumn<IThesis>> = {
    state: {
      accessor: 'state',
      title: 'State',
      textAlign: 'center',
      render: (thesis: IThesis) => {
        return <ThesisStateBadge state={thesis.state} />
      },
    },
    supervisors: {
      accessor: 'supervisors',
      title: 'Supervisor',
      render: (thesis: IThesis) => thesis.supervisors.map((user) => formatUser(user)).join(', '),
    },
    advisors: {
      accessor: 'advisors',
      title: 'Advisor',
      render: (thesis: IThesis) => thesis.advisors.map((user) => formatUser(user)).join(', '),
    },
    students: {
      accessor: 'students',
      title: 'Student',
      render: (thesis: IThesis) => thesis.students.map((user) => formatUser(user)).join(', '),
    },
    type: {
      accessor: 'type',
      title: 'Type',
      render: (thesis: IThesis) => GLOBAL_CONFIG.thesis_types[thesis.type] ?? thesis.type,
    },
    title: {
      accessor: 'title',
      title: 'Thesis Title',
      ellipsis: true,
      width: 200,
    },
    start_date: {
      accessor: 'startDate',
      title: 'Start Date',
      sortable: true,
      render: (thesis: IThesis) => formatDate(thesis.startDate, { withTime: false }),
    },
    end_date: {
      accessor: 'endDate',
      title: 'End Date',
      sortable: true,
      render: (thesis: IThesis) => formatDate(thesis.endDate, { withTime: false }),
    },
  }

  return (
    <DataTable
      fetching={!theses}
      withTableBorder
      minHeight={200}
      noRecordsText='No theses to show'
      borderRadius='sm'
      verticalSpacing='md'
      striped
      highlightOnHover
      totalRecords={theses?.totalElements ?? 0}
      recordsPerPage={limit}
      page={page + 1}
      onPageChange={(x) => setPage(x - 1)}
      sortStatus={{
        direction: sort.direction,
        columnAccessor: sort.column,
      }}
      onSortStatusChange={(newSort) => {
        setSort({
          column: newSort.columnAccessor as IThesesSort['column'],
          direction: newSort.direction,
        })
      }}
      bodyRef={bodyRef}
      records={theses?.content}
      idAccessor='thesisId'
      columns={columns.map((column) => columnConfig[column])}
      onRowClick={({ record: thesis }) => onThesisClick(thesis)}
    />
  )
}

export default ThesesTable
