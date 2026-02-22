import React, { Dispatch, SetStateAction } from 'react'
import { IThesisOverview, ThesisState } from '../../requests/responses/thesis'
import { PaginationResponse } from '../../requests/responses/pagination'

export interface IThesesFilters {
  search?: string
  states?: ThesisState[]
  types?: string[]
}

export interface IThesesSort {
  column: 'startDate' | 'endDate' | 'createdAt'
  direction: 'asc' | 'desc'
}

export interface IThesesContext {
  theses: PaginationResponse<IThesisOverview> | undefined
  filters: IThesesFilters
  setFilters: Dispatch<SetStateAction<IThesesFilters>>
  sort: IThesesSort
  setSort: Dispatch<SetStateAction<IThesesSort>>
  page: number
  setPage: Dispatch<SetStateAction<number>>
  limit: number
  updateThesis: (thesis: IThesisOverview) => unknown
}

export const ThesesContext = React.createContext<IThesesContext | undefined>(undefined)
