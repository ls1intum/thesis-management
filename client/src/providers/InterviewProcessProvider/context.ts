import React, { Dispatch, SetStateAction } from 'react'
import { IIntervieweeLightWithNextSlot, IInterviewSlot } from '../../requests/responses/interview' // adjust path if needed

export interface IInterviewProcessContext {
  processId: string | undefined

  // slots
  interviewSlots: Record<string, IInterviewSlot[]>
  interviewSlotsLoading: boolean
  fetchInterviewSlots: () => void

  // booking
  bookingLoading: boolean
  bookSlot: (slotId: string, intervieweeUserId: string) => void

  // interviewees
  interviewees: IIntervieweeLightWithNextSlot[]
  intervieweesLoading: boolean
  fetchPossibleInterviewees: (searchQuery?: string, state?: string) => void

  // cancel interview
  cancelSlot: (slotId: string, onCancelSucessfull?: () => void) => void
}

export const InterviewProcessContext = React.createContext<IInterviewProcessContext | undefined>(
  undefined,
)
