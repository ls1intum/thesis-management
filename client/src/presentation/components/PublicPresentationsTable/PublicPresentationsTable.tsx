import PresentationsTable from '@/presentation/components/PresentationsTable/PresentationsTable'
import React, { useEffect, useState } from 'react'
import type { PaginationResponse } from '@/core/requests/responses/pagination'
import type { IPublishedPresentation } from '@/thesis/requests/responses/thesis'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import { useNavigate } from 'react-router'

interface IPublicPresentationsTableProps {
  includeDrafts?: boolean
  limit?: number
  reducedData?: boolean
  researchGroupId?: string | null
}

const PublicPresentationsTable = (props: IPublicPresentationsTableProps) => {
  const { includeDrafts = false, limit = 10, reducedData = false, researchGroupId } = props

  const navigate = useNavigate()

  const openPresentation = (presentationId: string, openInNewTab: boolean) => {
    const url = `/presentations/${presentationId}`
    if (openInNewTab) {
      window.open(url, '_blank', 'noopener,noreferrer')
    } else {
      void navigate(url)
    }
  }

  const [presentations, setPresentations] = useState<PaginationResponse<IPublishedPresentation>>()
  const [page, setPage] = useState(0)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    return doRequest<PaginationResponse<IPublishedPresentation>>(
      `/v2/published-presentations`,
      {
        method: 'GET',
        requiresAuth: false,
        params: {
          page,
          limit,
          includeDrafts,
          ...(researchGroupId ? { researchGroupId } : {}),
        },
      },
      (res) => {
        if (res.ok) {
          setPresentations(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [page, limit, includeDrafts, version, researchGroupId])

  return (
    <PresentationsTable
      columns={
        reducedData
          ? [includeDrafts ? 'state' : '', 'students', 'location', 'scheduledAt']
          : [
              includeDrafts ? 'state' : '',
              'thesisTitle',
              'students',
              'type',
              'location',
              'streamUrl',
              'language',
              'scheduledAt',
              'actions',
            ]
      }
      presentations={presentations?.content}
      theses={(presentations?.content ?? []).map((row) => row.thesis)}
      onRowClick={(presentation, event) =>
        openPresentation(
          presentation.presentationId,
          event.metaKey || event.ctrlKey || event.shiftKey,
        )
      }
      customRowAttributes={(presentation) => ({
        onAuxClick: (event: React.MouseEvent) => {
          if (event.button === 1) {
            event.preventDefault()
            openPresentation(presentation.presentationId, true)
          }
        },
      })}
      pagination={{
        totalRecords: presentations?.totalElements ?? 0,
        recordsPerPage: limit,
        page: page + 1,
        onPageChange: (newPage) => setPage(newPage - 1),
      }}
      onChange={() => setVersion((prev) => prev + 1)}
    />
  )
}

export default PublicPresentationsTable
