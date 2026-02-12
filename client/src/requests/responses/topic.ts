import { ILightResearchGroup, IMinimalResearchGroup } from './researchGroup'
import { ILightUser, IMinimalUser } from './user'

export enum TopicState {
  OPEN = 'OPEN',
  DRAFT = 'DRAFT',
  CLOSED = 'CLOSED',
}

export interface ITopicOverview {
  topicId: string
  title: string
  state: TopicState
  thesisTypes: string[] | null
  createdAt: string
  advisors: IMinimalUser[]
  supervisors: IMinimalUser[]
  researchGroup: IMinimalResearchGroup
}

export interface ITopic extends ITopicOverview {
  problemStatement: string
  requirements: string
  goals: string
  references: string
  closedAt: string | null
  publishedAt: string | null
  updatedAt: string
  intendedStart: string | null
  applicationDeadline: string | null
  createdBy: ILightUser
  researchGroup: ILightResearchGroup
  advisors: ILightUser[]
  supervisors: ILightUser[]
}
