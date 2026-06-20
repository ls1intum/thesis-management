import type { Dispatch, SetStateAction } from 'react'
import React from 'react'
import type { PaginationResponse } from '@/core/requests/responses/pagination'
import type {
  ApplicationState,
  IApplication,
} from '@/core/application/requests/responses/application'
import type { ITopicOverview } from '@/core/topic/requests/responses/topic'

export interface IApplicationsFilters {
  search?: string
  states?: ApplicationState[]
  topics?: string[]
  types?: string[]
  includeSuggestedTopics?: boolean
}

export interface IApplicationsSort {
  column: 'createdAt' | 'updatedAt'
  direction: 'asc' | 'desc'
}

export interface IApplicationsContext {
  topics: ITopicOverview[] | undefined
  applications: PaginationResponse<IApplication> | undefined
  filters: IApplicationsFilters
  setFilters: Dispatch<SetStateAction<IApplicationsFilters>>
  sort: IApplicationsSort
  setSort: Dispatch<SetStateAction<IApplicationsSort>>
  page: number
  setPage: Dispatch<SetStateAction<number>>
  limit: number
  updateApplication: (application: IApplication) => unknown
  fetchApplication: (applicationId: string) => Promise<IApplication | null>
}

export const ApplicationsContext = React.createContext<IApplicationsContext | undefined>(undefined)
