import React, { Dispatch, SetStateAction } from 'react'
import { ITopicOverview } from '../../requests/responses/topic'
import { PaginationResponse } from '../../requests/responses/pagination'

export interface ITopicsFilters {
  types?: string[]
  states?: string[]
  researchSpecific?: boolean
  search?: string
  researchGroupIds?: string[]
}

export interface ITopicsContext {
  topics: PaginationResponse<ITopicOverview> | undefined
  filters: ITopicsFilters
  setFilters: Dispatch<SetStateAction<ITopicsFilters>>
  page: number
  setPage: Dispatch<SetStateAction<number>>
  limit: number
  updateTopic: (topic: ITopicOverview) => unknown
  addTopic: (topic: ITopicOverview) => unknown
  isLoading: boolean
}

export const TopicsContext = React.createContext<ITopicsContext | undefined>(undefined)
