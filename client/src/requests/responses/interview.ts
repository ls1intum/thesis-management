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
  user: ILightUser
  startDate: Date
  endDate: Date
  topicTitle: string
  location?: string
  streamUrl?: string
}
