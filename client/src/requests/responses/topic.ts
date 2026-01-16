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
  publishedAt: string | null
  updatedAt: string
  createdAt: string
  intendedStart: string | null
  applicationDeadline: string | null
  state: TopicState
  createdBy: ILightUser
  researchGroup: ILightResearchGroup
  advisors: ILightUser[]
  supervisors: ILightUser[]
}

export enum TopicState {
  OPEN = 'OPEN',
  CLOSED = 'CLOSED',
  DRAFT = 'DRAFT',
}
