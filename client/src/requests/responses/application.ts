import { ILightUser, IUser } from './user'
import { ITopic } from './topic'
import { ILightResearchGroup } from './researchGroup'

export enum ApplicationState {
  NOT_ASSESSED = 'NOT_ASSESSED',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
}

export interface IApplication {
  applicationId: string
  user: IUser
  topic: ITopic | null
  thesisTitle: string | null
  thesisType: string | null
  motivation: string
  state: ApplicationState
  desiredStartDate: string
  comment: string
  createdAt: string
  reviewers: Array<{
    user: ILightUser
    reason: string
    reviewedAt: string
  }> | null
  reviewedAt: string | null
  researchGroup: ILightResearchGroup
}

export interface IApplicationSummary {
  applicationId: string
  studyDegree: string
  studyProgram: string
  thesisTitle: string
  motivation: string
}
