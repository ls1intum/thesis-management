import { ILightResearchGroup } from './researchGroup'

export interface IEmailTemplate {
  id: string
  researchGroup: ILightResearchGroup
  templateCase: string
  description: string
  subject: string
  bodyHtml: string
  language: string
}
