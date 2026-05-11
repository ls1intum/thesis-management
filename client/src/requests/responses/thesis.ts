import type { ILightResearchGroup, IMinimalResearchGroup } from './researchGroup'
import type { ILightUser, IMinimalUser } from './user'

export enum ThesisState {
  PROPOSAL = 'PROPOSAL',
  WRITING = 'WRITING',
  SUBMITTED = 'SUBMITTED',
  ASSESSED = 'ASSESSED',
  GRADED = 'GRADED',
  FINISHED = 'FINISHED',
  DROPPED_OUT = 'DROPPED_OUT',
}

export interface IThesisPresentationOverview {
  presentationId: string
  type: string
  scheduledAt: string
}

export interface IThesisPresentation {
  thesisId: string
  presentationId: string
  state: string
  type: string
  visibility: string
  location: string | null
  streamUrl: string | null
  language: string
  presentationNoteHtml: string | null
  scheduledAt: string
  createdAt: string
  createdBy: ILightUser
}

export interface IThesisOverview {
  thesisId: string
  title: string
  type: string
  state: ThesisState
  startDate: string | null
  endDate: string | null
  createdAt: string
  keywords?: string[]
  students?: IMinimalUser[]
  supervisors?: IMinimalUser[]
  examiners?: IMinimalUser[]
  researchGroup?: IMinimalResearchGroup
  states?: Array<{
    state: ThesisState
    startedAt: string
    endedAt: string
  }>
  presentations?: IThesisPresentationOverview[]
}

export interface IThesis extends IThesisOverview {
  language: string
  metadata: {
    credits: Record<string, number>
    titles: Record<string, string>
  }
  visibility: string
  infoText?: string
  abstractText?: string
  applicationId: string | null
  anonymized?: boolean
  anonymizedAt?: string
  researchGroup: ILightResearchGroup
  students?: ILightUser[]
  supervisors?: ILightUser[]
  examiners?: ILightUser[]
  files?: Array<{
    fileId: string
    type: string
    filename: string
    uploadName: string
    uploadedAt: string
    uploadedBy: ILightUser
  }>
  assessment: null | {
    summary: string
    positives: string
    negatives: string
    gradeSuggestion: string
    createdAt: string
    createdBy: ILightUser
    gradeComponents?: Array<{
      gradeComponentId: string
      name: string
      weight: number
      isBonus: boolean
      grade: number
      position: number
    }>
  }
  proposals?: Array<{
    proposalId: string
    filename: string
    createdAt: string
    createdBy: ILightUser
    approvedAt: string | null
    approvedBy: ILightUser | null
  }>
  feedback?: Array<{
    feedbackId: string
    type: string
    feedback: string
    requestedBy: ILightUser
    requestedAt: string
    completedAt: string | null
  }>
  grade: null | {
    finalGrade: string
    feedback: string
  }
  presentations?: IThesisPresentation[]
}

export interface IThesisComment {
  commentId: string
  message: string
  filename: string | null
  uploadName: string | null
  createdAt: string
  createdBy: ILightUser
}

export interface IPublishedThesis {
  thesisId: string
  state: ThesisState
  title: string
  type: string
  startDate: string | null
  endDate: string | null
  abstractText?: string
  students?: IMinimalUser[]
  supervisors?: IMinimalUser[]
  examiners?: IMinimalUser[]
  researchGroup: ILightResearchGroup
}

export interface IPublishedPresentation {
  thesisId: string
  presentationId: string
  state: string
  type: string
  visibility: string
  location: string | null
  streamUrl: string | null
  language: string
  scheduledAt: string
  thesis: IPublishedThesis
}

export function isThesis(thesis: unknown): thesis is IThesis {
  if (!thesis || typeof thesis !== 'object') return false
  const obj = thesis as Record<string, unknown>
  return !!obj.thesisId && !!obj.states && 'language' in obj
}

export function isThesisPresentation(presentation: unknown): presentation is IThesisPresentation {
  if (!presentation || typeof presentation !== 'object') return false
  const obj = presentation as Record<string, unknown>
  return !!obj.presentationId && !obj.thesis
}

export function isPublishedPresentation(
  presentation: unknown,
): presentation is IPublishedPresentation {
  if (!presentation || typeof presentation !== 'object') return false
  const obj = presentation as Record<string, unknown>
  return !!obj.presentationId && !!obj.thesis
}
