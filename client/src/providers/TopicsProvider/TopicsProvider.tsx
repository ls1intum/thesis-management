import React, { PropsWithChildren, useEffect, useMemo, useState } from 'react'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { ITopic, TopicState } from '../../requests/responses/topic'
import { ITopicsContext, ITopicsFilters, TopicsContext } from './context'
import { PaginationResponse } from '../../requests/responses/pagination'

interface ITopicsProviderProps {
  limit: number
  hideIfEmpty?: boolean
  researchSpecific?: boolean
  initialFilters?: Partial<ITopicsFilters>
  states?: TopicState[]
}

const TopicsProvider = (props: PropsWithChildren<ITopicsProviderProps>) => {
  const {
    children,
    limit,
    hideIfEmpty = false,
    researchSpecific = true,
    initialFilters,
    states = [],
  } = props

  const [topics, setTopics] = useState<PaginationResponse<ITopic>>()
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState<ITopicsFilters>({
    states: states,
    researchSpecific: researchSpecific,
    ...initialFilters,
  })

  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    setIsLoading(true)

    return doRequest<PaginationResponse<ITopic>>(
      `/v2/topics`,
      {
        method: 'GET',
        requiresAuth: filters.researchSpecific ? true : false,
        params: {
          page,
          limit,
          type: filters.types?.join(',') || '',
          states: filters.states?.join(',') || '',
          onlyOwnResearchGroup: filters.researchSpecific ? 'true' : 'false',
          search: filters.search ?? '',
          researchGroupIds: filters.researchGroupIds?.join(',') || '',
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
  }, [filters, page, limit])

  useEffect(() => {
    setFilters((prev) => ({
      ...prev,
      states: states,
      researchSpecific: researchSpecific,
      ...initialFilters,
    }))
    setPage(0)
  }, [initialFilters])

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

          const index = prev.content.findIndex((x) => x.topicId === newTopic.topicId)

          if (index >= 0) {
            prev.content[index] = newTopic
          }

          return { ...prev }
        })
      },
      addTopic: (newTopic) => {
        setTopics((prev) => {
          if (!prev) {
            return undefined
          }

          prev.content = [newTopic, ...prev.content].slice(-limit)
          prev.totalElements += 1
          prev.totalPages = Math.ceil(prev.totalElements / limit)

          return { ...prev }
        })
      },
    }
  }, [topics, filters, page, limit, isLoading])

  if (hideIfEmpty && page === 0 && (!topics || topics.content.length === 0)) {
    return <></>
  }

  return <TopicsContext.Provider value={contextState}>{children}</TopicsContext.Provider>
}

export default TopicsProvider
