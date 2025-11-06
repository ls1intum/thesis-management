import { IApplicationSummary } from './application'
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
  intervieweeId: string
  interviewProcessId: string
  user: ILightUser
  startDate: Date
  endDate: Date
  topicTitle: string
  location?: string
  streamUrl?: string
}

export interface IIntervieweeSlot {
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

export interface IInterviewee extends IIntervieweeLight {
  interviewNote: string | null
  application: IApplicationSummary | null
}

export interface ITopicInterviewProcess {
  topicId: string
  topicTitle: string
  interviewProcessExists: boolean
}
