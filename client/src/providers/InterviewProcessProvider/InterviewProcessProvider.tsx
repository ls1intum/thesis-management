import React, { PropsWithChildren, useCallback, useEffect, useMemo, useState } from 'react'
import { InterviewProcessContext, IInterviewProcessContext } from './context'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { IIntervieweeLightWithNextSlot, IInterviewSlot } from '../../requests/responses/interview'
import { PaginationResponse } from '../../requests/responses/pagination'

interface IInterviewProcessProviderProps {
  processId: string
}

const InterviewProcessProvider = (props: PropsWithChildren<IInterviewProcessProviderProps>) => {
  const { children, processId } = props

  const [interviewSlots, setInterviewSlots] = useState<Record<string, IInterviewSlot[]>>({})
  const [interviewSlotsLoading, setInterviewSlotsLoading] = useState(false)

  const [bookingLoading, setBookingLoading] = useState(false)

  const [interviewees, setInterviewees] = useState<IIntervieweeLightWithNextSlot[]>([])
  const [intervieweesLoading, setIntervieweesLoading] = useState(false)

  function groupSlotsByDate(slots: IInterviewSlot[]): Record<string, IInterviewSlot[]> {
    return slots
      .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())
      .reduce(
        (acc, slot) => {
          const startDate = new Date(slot.startDate)
          const endDate = new Date(slot.endDate)
          const dateKey = startDate.toISOString().slice(0, 10)
          const slotWithDates = { ...slot, startDate, endDate }
          if (!acc[dateKey]) acc[dateKey] = []
          acc[dateKey].push(slotWithDates)
          return acc
        },
        {} as Record<string, IInterviewSlot[]>,
      )
  }

  const fetchInterviewSlots = useCallback(() => {
    setInterviewSlotsLoading(true)

    return doRequest<IInterviewSlot[]>(
      `/v2/interview-process/${processId}/interview-slots`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setInterviewSlots(groupSlotsByDate(res.data))
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setInterviewSlotsLoading(false)
      },
    )
  }, [processId])

  const bookSlot = useCallback(
    (slotId: string, intervieweeUserId: string) => {
      setBookingLoading(true)

      return doRequest<IInterviewSlot>(
        `/v2/interview-process/${processId}/slot/${slotId}/book`,
        {
          method: 'PUT',
          requiresAuth: true,
          data: {
            intervieweeUserId: intervieweeUserId,
          },
        },
        (res) => {
          setBookingLoading(false)

          if (res.ok) {
            fetchInterviewSlots()
          } else {
            showSimpleError(getApiResponseErrorMessage(res))
          }
        },
      )
    },
    [processId, fetchInterviewSlots],
  )

  const fetchPossibleInterviewees = useCallback(
    (searchQuery: string = '', state: string = ``) => {
      setIntervieweesLoading(true)

      return doRequest<PaginationResponse<IIntervieweeLightWithNextSlot>>(
        `/v2/interview-process/${processId}/interviewees`,
        {
          method: 'GET',
          requiresAuth: true,
          params: {
            searchQuery: searchQuery,
            limit: 100,
            state: state !== 'ALL' ? state : '',
          },
        },
        (res) => {
          if (res.ok) {
            setInterviewees(res.data.content)
          } else {
            showSimpleError(getApiResponseErrorMessage(res))
          }
          setIntervieweesLoading(false)
        },
      )
    },
    [processId],
  )

  useEffect(() => {
    // reset when process changes
    setInterviewSlots({})
    setInterviewees([])

    fetchInterviewSlots()
    fetchPossibleInterviewees()
  }, [processId, fetchInterviewSlots, fetchPossibleInterviewees])

  const contextState = useMemo<IInterviewProcessContext>(() => {
    return {
      processId,

      interviewSlots,
      interviewSlotsLoading,
      fetchInterviewSlots,

      bookingLoading,
      bookSlot,

      interviewees,
      intervieweesLoading,
      fetchPossibleInterviewees,
    }
  }, [
    processId,
    interviewSlots,
    interviewSlotsLoading,
    fetchInterviewSlots,
    bookingLoading,
    bookSlot,
    interviewees,
    intervieweesLoading,
    fetchPossibleInterviewees,
  ])

  return (
    <InterviewProcessContext.Provider value={contextState}>
      {children}
    </InterviewProcessContext.Provider>
  )
}

export default InterviewProcessProvider
