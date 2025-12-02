import { ILightUser } from './user'

export interface ILightResearchGroup {
  id: string
  head: ILightUser
  name: string
  abbreviation: string
}

export interface IResearchGroup extends ILightResearchGroup {
  description: string
  websiteUrl: string
  campus: string
  applicationNotificationEmail?: string | null
}
