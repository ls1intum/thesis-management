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

export interface IMailVariableDto {
  label: string
  templateVariable: string
  example: string
  group: string
}
