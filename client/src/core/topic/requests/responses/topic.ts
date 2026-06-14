import type {
  ILightResearchGroup,
  IMinimalResearchGroup,
} from '@/core/group/requests/responses/researchGroup'
import type { ILightUser, IMinimalUser } from '@/core/user/requests/responses/user'

export enum TopicState {
  OPEN = 'OPEN',
  DRAFT = 'DRAFT',
  CLOSED = 'CLOSED',
  EXPIRED = 'EXPIRED',
}

export interface ITopicOverview {
  topicId: string
  title: string
  state: TopicState
  thesisTypes?: string[]
  createdAt: string
  applicationDeadline: string | null
  supervisors: IMinimalUser[]
  examiners: IMinimalUser[]
  researchGroup?: IMinimalResearchGroup
}

export function toTopicOverview(topic: ITopic): ITopicOverview {
  return {
    topicId: topic.topicId,
    title: topic.title,
    state: topic.state,
    thesisTypes: topic.thesisTypes,
    createdAt: topic.createdAt,
    applicationDeadline: topic.applicationDeadline,
    supervisors: topic.supervisors,
    examiners: topic.examiners,
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
  supervisors: ILightUser[]
  examiners: ILightUser[]
}
