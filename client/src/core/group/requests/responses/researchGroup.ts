import type { IMinimalUser } from '@/core/user/requests/responses/user'

export interface IMinimalResearchGroup {
  id: string
  name: string
}

export interface ILightResearchGroup extends IMinimalResearchGroup {
  head: IMinimalUser
  abbreviation: string
}

export interface IResearchGroup extends ILightResearchGroup {
  description: string
  websiteUrl: string
  campus: string
  memberCount?: number
}
