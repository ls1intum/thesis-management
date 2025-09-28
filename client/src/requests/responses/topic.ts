import { ILightResearchGroup } from './researchGroup'
import { ILightUser } from './user'

export interface ITopic {
  topicId: string
  title: string
  thesisTypes: string[] | null
  problemStatement: string
  requirements: string
  goals: string
  references: string
  closedAt: string | null
  updatedAt: string
  createdAt: string
  intendedStart: string | null
  applicationDeadline: string | null
  createdBy: ILightUser
  researchGroup: ILightResearchGroup
  advisors: ILightUser[]
  supervisors: ILightUser[]
}
