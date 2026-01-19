import { ApplicationState, IApplicationSummary } from './application'
import { ILightUser } from './user'

export enum InterviewState {
  UNCONTACTED = 'Uncontacted',
  INVITED = 'Invited',
  SCHEDULED = 'Scheduled',
  COMPLETED = 'Completed',
}

export interface IInterviewProcess {
  interviewProcessId: string
  topicTitle: string
  completed: boolean
  totalInterviewees: number
  statesNumbers: Record<InterviewState, number>
}

export interface IUpcomingInterview {
  interviewProcessId: string
  topicTitle: string
  slot: IInterviewSlot
}

export interface IInterviewSlot {
  slotId: string
  startDate: Date
  endDate: Date
  bookedBy: IIntervieweeLight | null
  location?: string
  streamUrl?: string
}

export interface IIntervieweeLight {
  intervieweeId: string
  user: ILightUser
  score: number | null
  lastInvited: Date | null
}

export interface IIntervieweeLightWithNextSlot extends IIntervieweeLight {
  nextSlot: IInterviewSlot | null
  applicationId: string
  applicationState: ApplicationState
}

export interface IInterviewee extends IIntervieweeLight {
  interviewNote: string | null
  application: IApplicationSummary | null
}

export interface ITopicInterviewProcess {
  topicId: string
  topicTitle: string
  interviewProcessExists: boolean
}

export interface IApplicationInterviewProcess {
  applicationId: string
  applicantName: string
  state: ApplicationState
}
