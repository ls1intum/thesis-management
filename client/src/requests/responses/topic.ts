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
  thesisTypes?: string[]
  createdAt: string
  advisors: IMinimalUser[]
  supervisors: IMinimalUser[]
  researchGroup?: IMinimalResearchGroup
}

export function toTopicOverview(topic: ITopic): ITopicOverview {
  return {
    topicId: topic.topicId,
    title: topic.title,
    state: topic.state,
    thesisTypes: topic.thesisTypes,
    createdAt: topic.createdAt,
    advisors: topic.advisors,
    supervisors: topic.supervisors,
    researchGroup: topic.researchGroup,
  }
}

export interface ITopic extends ITopicOverview {
  problemStatement?: string
  requirements?: string
  goals?: string
  references?: string
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
