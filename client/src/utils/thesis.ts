import type { IPublishedThesis, IThesis } from '../requests/responses/thesis'
import { ThesisState } from '../requests/responses/thesis'
import type { ILightUser } from '../requests/responses/user'

export function isThesisClosed(thesis: IThesis | IPublishedThesis) {
  return thesis.state === ThesisState.FINISHED || thesis.state === ThesisState.DROPPED_OUT
}

export function checkMinimumThesisState(thesis: IThesis, state: ThesisState) {
  return (thesis.states ?? []).some((s) => s.state === state)
}

export function hasStudentAccess(
  thesis: IPublishedThesis | undefined,
  user: ILightUser | undefined,
) {
  if (!thesis) {
    return false
  }

  const users = [
    ...(thesis.students ?? []),
    ...(thesis.supervisors ?? []),
    ...(thesis.examiners ?? []),
  ]

  return Boolean(
    users.some((row) => row.userId === user?.userId) ||
    user?.groups?.some((name) => name === 'admin'),
  )
}

export function hasSupervisorAccess(
  thesis: IPublishedThesis | undefined,
  user: ILightUser | undefined,
) {
  if (!thesis) {
    return false
  }

  const users = [...(thesis.supervisors ?? []), ...(thesis.examiners ?? [])]

  return Boolean(
    users.some((row) => row.userId === user?.userId) ||
    user?.groups?.some((name) => name === 'admin'),
  )
}

export function hasExaminerAccess(
  thesis: IPublishedThesis | undefined,
  user: ILightUser | undefined,
) {
  return Boolean(
    (thesis?.examiners ?? []).some((row) => row.userId === user?.userId) ||
    user?.groups?.some((name) => name === 'admin'),
  )
}
