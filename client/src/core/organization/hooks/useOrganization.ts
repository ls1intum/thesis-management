import { useCallback, useEffect, useState } from 'react'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import type { ISchool, IStudyProgram } from '@/core/organization/requests/responses/organization'

export interface IOrganization {
  schools: ISchool[]
  studyPrograms: IStudyProgram[]
  isLoading: boolean
  reload: () => void
}

/**
 * Loads the schools (with their departments) and study programs configured for this instance.
 * The lists are small reference data, so every consumer simply loads them once.
 */
export function useOrganization(): IOrganization {
  const [schools, setSchools] = useState<ISchool[]>([])
  const [studyPrograms, setStudyPrograms] = useState<IStudyProgram[]>([])
  const [pending, setPending] = useState(2)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    setPending(2)

    const cancelSchools = doRequest<ISchool[]>(
      '/v2/schools',
      { method: 'GET', requiresAuth: true },
      (res) => {
        if (res.ok) {
          setSchools(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }

        setPending((value) => value - 1)
      },
    )

    const cancelStudyPrograms = doRequest<IStudyProgram[]>(
      '/v2/study-programs',
      { method: 'GET', requiresAuth: true },
      (res) => {
        if (res.ok) {
          setStudyPrograms(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }

        setPending((value) => value - 1)
      },
    )

    return () => {
      cancelSchools()
      cancelStudyPrograms()
    }
  }, [version])

  const reload = useCallback(() => setVersion((value) => value + 1), [])

  return { schools, studyPrograms, isLoading: pending > 0, reload }
}
