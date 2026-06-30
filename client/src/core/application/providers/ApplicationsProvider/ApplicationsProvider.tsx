import type { PropsWithChildren, ReactNode } from 'react'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import { doRequest } from '@/core/requests/request'
import type { PaginationResponse } from '@/core/requests/responses/pagination'
import type {
  IApplicationsContext,
  IApplicationsFilters,
  IApplicationsSort,
} from '@/core/application/providers/ApplicationsProvider/context'
import { ApplicationsContext } from '@/core/application/providers/ApplicationsProvider/context'
import type {
  ApplicationState,
  IApplication,
} from '@/core/application/requests/responses/application'
import { useDebouncedValue } from '@mantine/hooks'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import { useAllTopics } from '@/core/hooks/fetcher'
import { useLoggedInUser } from '@/core/hooks/authentication'

interface IApplicationsProviderProps {
  fetchAll?: boolean
  limit: number
  defaultStates?: ApplicationState[]
  defaultTopics?: string[]
  showOnlyAssignedTopics?: boolean
  hideIfEmpty?: boolean
  emptyComponent?: ReactNode
  // When true, the provider reads its initial page/filters/sort from the URL
  // search params and pushes subsequent changes back to the URL. Only enable on
  // routes where this provider owns the URL state.
  persistState?: boolean
}

const DEFAULT_SORT: IApplicationsSort = { column: 'createdAt', direction: 'desc' }
const SORT_COLUMNS: IApplicationsSort['column'][] = ['createdAt', 'updatedAt']
const SORT_DIRECTIONS: IApplicationsSort['direction'][] = ['asc', 'desc']

const ApplicationsProvider = (props: PropsWithChildren<IApplicationsProviderProps>) => {
  const {
    children,
    limit,
    defaultStates,
    defaultTopics,
    showOnlyAssignedTopics,
    fetchAll = false,
    hideIfEmpty = false,
    emptyComponent,
    persistState = false,
  } = props

  const user = useLoggedInUser()
  const topics = useAllTopics()

  const [searchParams, setSearchParams] = useSearchParams()

  const [applications, setApplications] = useState<PaginationResponse<IApplication>>()

  const [page, setPage] = useState(() => {
    if (!persistState) return 0
    const raw = parseInt(searchParams.get('page') ?? '', 10)
    return Number.isFinite(raw) && raw >= 0 ? raw : 0
  })

  const previousContent = useRef<string[]>([])

  const [filters, setFilters] = useState<IApplicationsFilters>(() => {
    const base: IApplicationsFilters = {
      states: defaultStates,
      topics: defaultTopics,
      includeSuggestedTopics: true,
    }
    if (!persistState) return base

    const statesParam = searchParams.get('states')
    const topicsParam = searchParams.get('topics')
    const typesParam = searchParams.get('types')
    return {
      ...base,
      search: searchParams.get('search') ?? base.search,
      states: statesParam
        ? (statesParam.split(',').filter(Boolean) as ApplicationState[])
        : base.states,
      topics: topicsParam ? topicsParam.split(',').filter(Boolean) : base.topics,
      types: typesParam ? typesParam.split(',').filter(Boolean) : base.types,
    }
  })
  const [sort, setSort] = useState<IApplicationsSort>(() => {
    if (!persistState) return DEFAULT_SORT
    const column = searchParams.get('sortBy')
    const direction = searchParams.get('sortOrder')
    return {
      column: SORT_COLUMNS.includes(column as IApplicationsSort['column'])
        ? (column as IApplicationsSort['column'])
        : DEFAULT_SORT.column,
      direction: SORT_DIRECTIONS.includes(direction as IApplicationsSort['direction'])
        ? (direction as IApplicationsSort['direction'])
        : DEFAULT_SORT.direction,
    }
  })

  const adjustedFilters = useMemo(() => {
    const copiedFilters = { ...filters }

    if (showOnlyAssignedTopics && typeof copiedFilters.topics === 'undefined') {
      copiedFilters.topics = [
        'NO_TOPIC',
        ...(topics ?? [])
          .filter(
            (topic) =>
              (topic.examiners ?? []).some((examiner) => examiner.userId === user.userId) ||
              (topic.supervisors ?? []).some((supervisor) => supervisor.userId === user.userId),
          )
          .map((topic) => topic.topicId),
      ]
    }

    return copiedFilters
  }, [filters, topics, user.userId, showOnlyAssignedTopics])

  const [debouncedSearch] = useDebouncedValue(adjustedFilters.search ?? '', 500)

  const filterStatesKey = adjustedFilters.states?.join(',')
  const filterTopicsKey = adjustedFilters.topics?.join(',')
  const filterTypesKey = adjustedFilters.types?.join(',')
  // URL sync uses the raw user-selected topics so the implicit
  // `showOnlyAssignedTopics` expansion is not leaked into `?topics=` params.
  const urlTopicsKey = filters.topics?.join(',')
  const topicsLoaded = !!topics

  const didMountRef = useRef(false)
  useEffect(() => {
    if (!didMountRef.current) {
      didMountRef.current = true
      return
    }
    setPage(0)
  }, [sort, adjustedFilters])

  useEffect(() => {
    setApplications(undefined)

    if (!topics) {
      return
    }

    if (page === 0) {
      previousContent.current = []
    }

    return doRequest<PaginationResponse<IApplication>>(
      `/v2/applications`,
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          fetchAll: fetchAll ? 'true' : 'false',
          previous: previousContent.current.join(','),
          search: debouncedSearch,
          state: adjustedFilters.states?.join(',') ?? '',
          type: adjustedFilters.types?.join(',') ?? '',
          topic:
            adjustedFilters.topics
              ?.map((topicId) =>
                topicId === 'NO_TOPIC' ? '00000000-0000-0000-0000-000000000000' : topicId,
              )
              .join(',') ?? '',
          includeSuggestedTopics:
            adjustedFilters.includeSuggestedTopics === false ? 'false' : 'true',
          limit,
          page,
          sortBy: sort.column,
          sortOrder: sort.direction,
        },
      },
      (res) => {
        if (!res.ok) {
          showSimpleError(getApiResponseErrorMessage(res))

          return setApplications({
            content: [],
            totalPages: 0,
            totalElements: 0,
            last: true,
            pageNumber: 0,
            pageSize: limit,
          })
        }

        previousContent.current.push(...(res.data.content ?? []).map((item) => item.applicationId))
        setApplications(res.data)
      },
    )
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- adjustedFilters.states/topics/types are tracked via the joined key consts above to avoid identity-based reruns
  }, [
    fetchAll,
    page,
    limit,
    sort,
    filterStatesKey,
    filterTopicsKey,
    filterTypesKey,
    adjustedFilters.includeSuggestedTopics,
    debouncedSearch,
    topicsLoaded,
  ])

  useEffect(() => {
    if (!persistState) return

    const params = new URLSearchParams(searchParams)

    const setOrDelete = (key: string, value: string | undefined) => {
      if (value && value.length > 0) {
        params.set(key, value)
      } else {
        params.delete(key)
      }
    }

    setOrDelete('page', page > 0 ? String(page) : undefined)
    setOrDelete('search', filters.search)
    setOrDelete('states', filterStatesKey)
    setOrDelete('topics', urlTopicsKey)
    setOrDelete('types', filterTypesKey)
    setOrDelete('sortBy', sort.column !== DEFAULT_SORT.column ? sort.column : undefined)
    setOrDelete('sortOrder', sort.direction !== DEFAULT_SORT.direction ? sort.direction : undefined)

    setSearchParams(params, { replace: true })
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- searchParams/setSearchParams change on every navigation; only sync URL when actual state changes
  }, [persistState, page, filters.search, filterStatesKey, urlTopicsKey, filterTypesKey, sort])

  const fetchApplication = async (applicationId: string): Promise<IApplication | null> => {
    return new Promise((resolve) => {
      doRequest<IApplication>(
        `/v2/applications/${applicationId}`,
        {
          method: 'GET',
          requiresAuth: true,
        },
        (res) => {
          if (res.ok) {
            resolve(res.data)
          } else {
            showSimpleError(getApiResponseErrorMessage(res))
            resolve(null)
          }
        },
      )
    })
  }

  const contextState = useMemo<IApplicationsContext>(() => {
    return {
      topics,
      applications,
      filters: adjustedFilters,
      setFilters: (value) => {
        setPage(0)
        setFilters(value)
      },
      sort,
      setSort: (value) => {
        setPage(0)
        setSort(value)
      },
      page,
      setPage,
      limit,
      updateApplication: (newApplication) => {
        setApplications((prev) => {
          if (!prev) {
            return undefined
          }

          const index = (prev.content ?? []).findIndex(
            (x) => x.applicationId === newApplication.applicationId,
          )

          if (index >= 0) {
            const content = prev.content ?? []
            content[index] = newApplication

            return { ...prev, content }
          }

          return { ...prev }
        })
      },
      fetchApplication,
    }
  }, [topics, applications, adjustedFilters, sort, page, limit])

  if (hideIfEmpty && page === 0 && (!applications || (applications.content ?? []).length === 0)) {
    return <>{emptyComponent}</>
  }

  return <ApplicationsContext value={contextState}>{children}</ApplicationsContext>
}

export default ApplicationsProvider
