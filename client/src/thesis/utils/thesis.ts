import type { IPublishedThesis, IThesis } from '@/thesis/requests/responses/thesis'
import { ThesisState } from '@/thesis/requests/responses/thesis'
import type { ILightUser } from '@/core/user/requests/responses/user'
import { GLOBAL_CONFIG } from '@/core/config/global'

export function isThesisClosed(thesis: IThesis | IPublishedThesis) {
  return thesis.state === ThesisState.FINISHED || thesis.state === ThesisState.DROPPED_OUT
}

/**
 * The portal students submit the final thesis to: the one of the school the thesis belongs to,
 * otherwise the instance default.
 */
export function getThesisSubmissionPortalUrl(thesis: Pick<IThesis, 'submissionPortalUrl'>) {
  return thesis.submissionPortalUrl ?? GLOBAL_CONFIG.thesis_portal_url
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
