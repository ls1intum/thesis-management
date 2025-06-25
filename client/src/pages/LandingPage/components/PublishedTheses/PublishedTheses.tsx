import { DataTable } from 'mantine-datatable'
import { ActionIcon, Anchor, Center, Group, Modal, Stack, Text, Tooltip } from '@mantine/core'
import React, { useEffect, useState } from 'react'
import { PaginationResponse } from '../../../../requests/responses/pagination'
import { IPublishedThesis } from '../../../../requests/responses/thesis'
import { doRequest } from '../../../../requests/request'
import { showSimpleError } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { DownloadSimple, Eye } from 'phosphor-react'
import ThesisData from '../../../../components/ThesisData/ThesisData'
import AvatarUserList from '../../../../components/AvatarUserList/AvatarUserList'
import { formatThesisType } from '../../../../utils/format'
import { GLOBAL_CONFIG } from '../../../../config/global'
import AuthenticatedFilePreview from '../../../../components/AuthenticatedFilePreview/AuthenticatedFilePreview'
import TopicCardGrid from '../TopicCardGrid/TopicCardGrid'
import ThesisTypeBadge from '../ThesisTypBadge/ThesisTypBadge'

interface PublishedThesesProps {
  search: string
  representationType: string
  filters?: {
    researchGroupIds?: string[]
    types?: string[]
  }
}

const PublishedTheses = ({ search, representationType, filters }: PublishedThesesProps) => {
  const [page, setPage] = useState(0)
  const limit = 10

  const [theses, setTheses] = useState<PaginationResponse<IPublishedThesis>>()
  const [openedThesis, setOpenedThesis] = useState<IPublishedThesis>()

  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    setIsLoading(true)
    return doRequest<PaginationResponse<IPublishedThesis>>(
      '/v2/published-theses',
      {
        method: 'GET',
        requiresAuth: false,
        params: {
          page,
          limit,
          search: search,
          researchGroupIds: filters?.researchGroupIds?.join(',') ?? '',
          types: filters?.types?.join(',') ?? '',
        },
      },
      (response) => {
        setIsLoading(false)

        if (response.ok) {
          setTheses(response.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(response))

          return setTheses({
            content: [],
            totalPages: 0,
            totalElements: 0,
            last: true,
            pageNumber: 0,
            pageSize: limit,
          })
        }
      },
    )
  }, [page, limit, search, filters])

  const content =
    representationType === 'list' ? (
      <DataTable
        fetching={isLoading}
        withTableBorder={false}
        minHeight={200}
        noRecordsText='No theses published yet'
        borderRadius='sm'
        verticalSpacing='md'
        striped
        highlightOnHover
        totalRecords={theses?.totalElements ?? 0}
        recordsPerPage={limit}
        page={page + 1}
        onPageChange={(x) => setPage(x - 1)}
        records={theses?.content}
        idAccessor='thesisId'
        columns={[
          {
            accessor: 'title',
            title: 'Title',
            cellsStyle: () => ({ minWidth: 200 }),
          },
          {
            accessor: 'type',
            title: 'Type',
            ellipsis: true,
            width: 140,
            render: (thesis: IPublishedThesis) => (
              <ThesisTypeBadge type={thesis.type}></ThesisTypeBadge>
            ),
          },
          {
            accessor: 'students',
            title: 'Student(s)',
            ellipsis: true,
            width: 180,
            render: (thesis) => <AvatarUserList users={thesis.students} />,
          },
          {
            accessor: 'advisors',
            title: 'Advisor(s)',
            ellipsis: true,
            width: 180,
            render: (thesis) => <AvatarUserList users={thesis.advisors} />,
          },
          {
            accessor: 'researchGroup.name',
            title: 'Research Group',
            width: 180,
            ellipsis: true,
            render: (thesis) => (
              <Tooltip openDelay={500} label={thesis.researchGroup?.name ?? ''} withArrow>
                <Text size='sm' truncate>
                  {thesis.researchGroup?.name ?? ''}
                </Text>
              </Tooltip>
            ),
          },
          {
            accessor: 'actions',
            title: 'Actions',
            textAlign: 'center',
            width: 100,
            render: (thesis) => (
              <Center>
                <Group gap='xs' onClick={(e) => e.stopPropagation()} wrap='nowrap'>
                  <ActionIcon onClick={() => setOpenedThesis(thesis)}>
                    <Eye />
                  </ActionIcon>
                  <ActionIcon
                    component={Anchor<'a'>}
                    href={`${GLOBAL_CONFIG.server_host}/api/v2/published-theses/${thesis.thesisId}/thesis`}
                    target='_blank'
                  >
                    <DownloadSimple />
                  </ActionIcon>
                </Group>
              </Center>
            ),
          },
        ]}
        onRowClick={({ record }) => setOpenedThesis(record)}
      />
    ) : (
      <TopicCardGrid
        gridContent={{
          topics: theses ?? {
            content: [],
            totalPages: 0,
            totalElements: 0,
            last: true,
            pageNumber: page,
            pageSize: limit,
          },
          page,
          setPage,
          limit,
          isLoading: !theses,
        }}
        setOpenTopic={setOpenedThesis}
      />
    )

  return (
    <>
      {content}
      <Modal
        title={openedThesis?.title}
        opened={!!openedThesis}
        onClose={() => setOpenedThesis(undefined)}
        size='xl'
      >
        {openedThesis && (
          <Stack>
            <ThesisData thesis={openedThesis} additionalInformation={['abstract']} />
            <AuthenticatedFilePreview
              url={`/v2/published-theses/${openedThesis.thesisId}/thesis`}
              filename={`${openedThesis.title.toLowerCase().replaceAll(' ', '-')}.pdf`}
              type='pdf'
            />
          </Stack>
        )}
      </Modal>
    </>
  )
}

export default PublishedTheses
