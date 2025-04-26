import { ILightUser } from './user'

export interface ILightResearchGroup {
  id: string
  head: ILightUser
  name: string
}

export interface IResearchGroup extends ILightResearchGroup {
  abbreviation: string
  description: string
  websiteUrl: string
  campus: string
}
