import type { IMinimalUser, IUser } from '@/core/user/requests/responses/user'
import type { ITopic } from '@/core/topic/requests/responses/topic'
import type { ILightResearchGroup } from '@/core/group/requests/responses/researchGroup'

export enum ApplicationState {
  NOT_ASSESSED = 'NOT_ASSESSED',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  INTERVIEWING = 'INTERVIEWING',
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
    user: IMinimalUser
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
  interests: string
  projects: string
  specialSkills: string
}
