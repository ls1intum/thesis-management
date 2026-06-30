import type { PropsWithChildren } from 'react'
import React, { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import type {
  IThesesContext,
  IThesesFilters,
  IThesesSort,
} from '@/thesis/providers/ThesesProvider/context'
import { ThesesContext } from '@/thesis/providers/ThesesProvider/context'
import type { IThesisOverview, ThesisState } from '@/thesis/requests/responses/thesis'
import { doRequest } from '@/core/requests/request'
import type { PaginationResponse } from '@/core/requests/responses/pagination'
import { useDebouncedValue } from '@mantine/hooks'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'

interface IThesesProviderProps {
  fetchAll?: boolean
  limit: number
  defaultStates?: ThesisState[]
  hideIfEmpty?: boolean
  // When true, the provider reads its initial page/filters/sort from the URL
  // search params and pushes subsequent changes back to the URL so browser back
  // restores the previous state. Only enable on pages where this provider owns
  // the URL state (i.e. no other URL-synced provider on the same route).
  persistState?: boolean
}

const DEFAULT_SORT: IThesesSort = { column: 'startDate', direction: 'asc' }
const SORT_COLUMNS: IThesesSort['column'][] = ['startDate', 'endDate', 'createdAt']
const SORT_DIRECTIONS: IThesesSort['direction'][] = ['asc', 'desc']

const ThesesProvider = (props: PropsWithChildren<IThesesProviderProps>) => {
  const {
    children,
    fetchAll = false,
    limit,
    hideIfEmpty = false,
    defaultStates,
    persistState = false,
  } = props

  const [searchParams, setSearchParams] = useSearchParams()

  const [theses, setTheses] = useState<PaginationResponse<IThesisOverview>>()

  const [page, setPage] = useState(() => {
    if (!persistState) return 0
    const raw = parseInt(searchParams.get('page') ?? '', 10)
    return Number.isFinite(raw) && raw >= 0 ? raw : 0
  })

  const [filters, setFilters] = useState<IThesesFilters>(() => {
    if (!persistState) {
      return { states: defaultStates }
    }
    const statesParam = searchParams.get('states')
    const typesParam = searchParams.get('types')
    return {
      search: searchParams.get('search') ?? undefined,
      states: statesParam
        ? (statesParam.split(',').filter(Boolean) as ThesisState[])
        : defaultStates,
      types: typesParam ? typesParam.split(',').filter(Boolean) : undefined,
    }
  })

  const [sort, setSort] = useState<IThesesSort>(() => {
    if (!persistState) return DEFAULT_SORT
    const column = searchParams.get('sortBy')
    const direction = searchParams.get('sortOrder')
    return {
      column: SORT_COLUMNS.includes(column as IThesesSort['column'])
        ? (column as IThesesSort['column'])
        : DEFAULT_SORT.column,
      direction: SORT_DIRECTIONS.includes(direction as IThesesSort['direction'])
        ? (direction as IThesesSort['direction'])
        : DEFAULT_SORT.direction,
    }
  })

  const [debouncedSearch] = useDebouncedValue(filters.search ?? '', 500)

  const filterStatesKey = filters.states?.join(',')
  const filterTypesKey = filters.types?.join(',')

  useEffect(() => {
    setTheses(undefined)

    return doRequest<PaginationResponse<IThesisOverview>>(
      `/v2/theses`,
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          fetchAll: fetchAll ? 'true' : 'false',
          search: debouncedSearch,
          state: filters.states?.join(',') ?? '',
          type: filters.types?.join(',') ?? '',
          page,
          limit,
          sortBy: sort.column,
          sortOrder: sort.direction,
        },
      },
      (res) => {
        if (!res.ok) {
          showSimpleError(getApiResponseErrorMessage(res))

          return setTheses({
            content: [],
            totalPages: 0,
            totalElements: 0,
            last: true,
            pageNumber: 0,
            pageSize: limit,
          })
        }

        setTheses(res.data)
      },
    )
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- filters.states/types are tracked via joined keys below to avoid identity-based reruns
  }, [fetchAll, page, limit, sort, filterStatesKey, filterTypesKey, debouncedSearch])

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
    setOrDelete('types', filterTypesKey)
    setOrDelete('sortBy', sort.column !== DEFAULT_SORT.column ? sort.column : undefined)
    setOrDelete('sortOrder', sort.direction !== DEFAULT_SORT.direction ? sort.direction : undefined)

    setSearchParams(params, { replace: true })
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- searchParams/setSearchParams change on every navigation; only sync URL when actual state changes
  }, [persistState, page, filters.search, filterStatesKey, filterTypesKey, sort])

  const contextState = useMemo<IThesesContext>(() => {
    return {
      theses,
      filters,
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
      updateThesis: (newThesis) => {
        setTheses((prev) => {
          if (!prev) {
            return undefined
          }

          const index = (prev.content ?? []).findIndex((x) => x.thesisId === newThesis.thesisId)

          if (index >= 0) {
            const content = prev.content ?? []
            content[index] = newThesis

            return { ...prev, content }
          }

          return { ...prev }
        })
      },
    }
  }, [theses, filters, sort, page, limit])

  if (hideIfEmpty && page === 0 && (!theses || (theses.content ?? []).length === 0)) {
    return <></>
  }

  return <ThesesContext value={contextState}>{children}</ThesesContext>
}

export default ThesesProvider
