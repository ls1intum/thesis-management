import type { ILightUser } from '@/core/user/requests/responses/user'

export interface IMinimalResearchGroup {
  id: string
  name: string
}

export interface ILightResearchGroup extends IMinimalResearchGroup {
  head: ILightUser
  abbreviation: string
}

export interface IResearchGroup extends ILightResearchGroup {
  description: string
  websiteUrl: string
  campus: string
  memberCount?: number
}
