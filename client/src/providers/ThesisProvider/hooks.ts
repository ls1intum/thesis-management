import { useContext, useMemo, useState } from 'react'
import { ThesisContext } from './context'
import { IPublishedThesis, IThesis } from '../../requests/responses/thesis'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'
import { useUser } from '../../hooks/authentication'

export function useThesisContext() {
  const data = useContext(ThesisContext)

  if (!data) {
    throw new Error('ThesisContext not initialized')
  }

  return data
}

export function useLoadedThesisContext() {
  const { thesis, access, updateThesis } = useThesisContext()

  if (!thesis) {
    throw new Error('Thesis not loaded')
  }

  return { thesis, access, updateThesis }
}

export function useThesisContextUpdater() {
  const data = useContext(ThesisContext)

  if (!data) {
    return () => undefined
  }

  return data.updateThesis
}

export function useThesisUpdateAction<T extends (...args: any[]) => any>(
  fn: (...args: Parameters<T>) => PromiseLike<IThesis>,
  successMessage?: string,
): [boolean, (...args: Parameters<T>) => unknown] {
  const updateThesis = useThesisContextUpdater()

  const [loading, setLoading] = useState(false)

  return [
    loading,
    async (...args: Parameters<T>) => {
      setLoading(true)
      try {
        updateThesis(await fn(...args))

        if (successMessage) {
          showSimpleSuccess(successMessage)
        }
      } catch (e) {
        if (e instanceof Error) {
          showSimpleError(e.message)
        } else {
          showSimpleError(String(e))
        }
      } finally {
        setLoading(false)
      }
    },
  ]
}

export function useThesisAccess(thesis: IThesis | IPublishedThesis | undefined | false) {
  const user = useUser()

  return useMemo(() => {
    const access = {
      examiner: false,
      supervisor: false,
      student: false,
    }

    if (user && thesis) {
      if (
        user.groups?.includes('admin') ||
        (thesis.examiners ?? []).some((examiner) => user.userId === examiner.userId)
      ) {
        access.examiner = true
      }

      if (
        access.examiner ||
        (thesis.supervisors ?? []).some((supervisor) => user.userId === supervisor.userId)
      ) {
        access.supervisor = true
      }

      if (
        access.supervisor ||
        (thesis.students ?? []).some((student) => user.userId === student.userId)
      ) {
        access.student = true
      }
    }

    return access
  }, [thesis, user])
}
