import type { PropsWithChildren } from 'react'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import type { ITopicOverview } from '@/core/topic/requests/responses/topic'
import { TopicState } from '@/core/topic/requests/responses/topic'
import type { ITopicsContext, ITopicsFilters } from '@/core/topic/providers/TopicsProvider/context'
import { TopicsContext } from '@/core/topic/providers/TopicsProvider/context'
import type { PaginationResponse } from '@/core/requests/responses/pagination'

interface ITopicsProviderProps {
  limit: number
  hideIfEmpty?: boolean
  researchSpecific?: boolean
  initialFilters?: Partial<ITopicsFilters>
  states?: TopicState[]
  // When true, the provider reads its initial page/search/state/type filters
  // from the URL search params and pushes subsequent changes back to the URL.
  // Only enable on pages where this provider owns the URL state (i.e. not on
  // the LandingPage, which manages its own search params).
  persistState?: boolean
}

const TopicsProvider = (props: PropsWithChildren<ITopicsProviderProps>) => {
  const {
    children,
    limit,
    hideIfEmpty = false,
    researchSpecific = true,
    initialFilters,
    states = [],
    persistState = false,
  } = props

  const [searchParams, setSearchParams] = useSearchParams()

  const [topics, setTopics] = useState<PaginationResponse<ITopicOverview>>()

  const [page, setPage] = useState(() => {
    if (!persistState) return 0
    const raw = parseInt(searchParams.get('page') ?? '', 10)
    return Number.isFinite(raw) && raw >= 0 ? raw : 0
  })

  const [filters, setFilters] = useState<ITopicsFilters>(() => {
    const base: ITopicsFilters = {
      states: states,
      researchSpecific: researchSpecific,
      ...initialFilters,
    }
    if (!persistState) return base

    const statesParam = searchParams.get('states')
    const typesParam = searchParams.get('types')
    return {
      ...base,
      search: searchParams.get('search') ?? base.search,
      states: statesParam ? statesParam.split(',').filter(Boolean) : base.states,
      types: typesParam ? typesParam.split(',').filter(Boolean) : base.types,
    }
  })

  const [isLoading, setIsLoading] = useState(false)

  const fetchTopics = () => {
    setIsLoading(true)

    return doRequest<PaginationResponse<ITopicOverview>>(
      `/v2/topics`,
      {
        method: 'GET',
        requiresAuth: filters.researchSpecific ? true : false,
        params: {
          page,
          limit,
          type: filters.types?.join(',') ?? '',
          states: filters.states?.join(',') ?? '',
          onlyOwnResearchGroup: filters.researchSpecific ? 'true' : 'false',
          search: filters.search ?? '',
          researchGroupIds: filters.researchGroupIds?.join(',') ?? '',
        },
      },
      (res) => {
        setIsLoading(false)

        if (!res.ok) {
          showSimpleError(`Could not fetch topics: ${res.status}`)

          return setTopics({
            content: [],
            totalPages: 0,
            totalElements: 0,
            last: true,
            pageNumber: 0,
            pageSize: limit,
          })
        }

        setTopics(res.data)
      },
    )
  }

  useEffect(() => {
    return fetchTopics()
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- fetchTopics is recreated each render; only refetch when filters/pagination change
  }, [filters, page, limit])

  const filterStatesKey = filters.states?.join(',')
  const filterTypesKey = filters.types?.join(',')

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

    setSearchParams(params, { replace: true })
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- searchParams/setSearchParams change on every navigation; only sync URL when actual state changes
  }, [persistState, page, filters.search, filterStatesKey, filterTypesKey])

  const initialFiltersKey = JSON.stringify(initialFilters)
  const prevInitialFiltersKeyRef = useRef(initialFiltersKey)

  useEffect(() => {
    if (prevInitialFiltersKeyRef.current === initialFiltersKey) {
      return
    }
    prevInitialFiltersKeyRef.current = initialFiltersKey

    setFilters((prev) => ({
      ...prev,
      states: states,
      researchSpecific: researchSpecific,
      ...initialFilters,
    }))
    setPage(0)
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- initialFilters/states/researchSpecific are captured at the time initialFiltersKey changes; tracking the raw values would re-run on every render
  }, [initialFiltersKey])

  const contextState = useMemo<ITopicsContext>(() => {
    return {
      topics,
      filters,
      setFilters,
      page,
      setPage,
      limit,
      isLoading,
      updateTopic: (newTopic) => {
        setTopics((prev) => {
          if (!prev) {
            return undefined
          }

          const content = prev.content ?? []
          const index = content.findIndex((x) => x.topicId === newTopic.topicId)

          let newFetchRequired = false

          if (index >= 0) {
            newFetchRequired = newTopic.state !== content[index].state
            content[index] = newTopic
            prev.content = content
          }

          if (newFetchRequired) {
            // If state changed, refetch to update based on filters
            fetchTopics()
          }

          return { ...prev }
        })
      },
      addTopic: (newTopic) => {
        setTopics((prev) => {
          if (!prev) {
            return undefined
          }

          const topicStates = filters.states ?? [TopicState.OPEN.toString()]
          const newHasState = topicStates.includes(newTopic.state)

          if (newHasState) {
            prev.content = [newTopic, ...(prev.content ?? [])].slice(0, limit)
            prev.totalElements += 1
            prev.totalPages = Math.ceil(prev.totalElements / limit)
          }

          return { ...prev }
        })
      },
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- fetchTopics is recreated each render and is only called from within callbacks at invocation time
  }, [topics, filters, page, limit, isLoading])

  if (hideIfEmpty && page === 0 && (!topics || (topics.content?.length ?? 0) === 0)) {
    return <></>
  }

  return <TopicsContext value={contextState}>{children}</TopicsContext>
}

export default TopicsProvider
