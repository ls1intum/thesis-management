import { ITopic } from '../../../../requests/responses/topic'

interface CallerContext {
  userId: string
  isAdmin: boolean
}

/**
 * Decides whether the calling user is allowed to change the research group of
 * a topic in the ReplaceTopicModal. Mirrors the server-side rule in
 * `TopicService.updateTopic` so the client can disable the dropdown when the
 * server would reject the change.
 *
 * Rule (matches issue #764): allowed if the caller is an admin OR is the
 * topic's creator OR is listed on the topic as a supervisor/examiner.
 */
export const canEditResearchGroup = (
  topic: ITopic | undefined,
  caller: CallerContext | undefined,
): boolean => {
  if (!caller) {
    return false
  }
  if (caller.isAdmin) {
    return true
  }
  if (!topic) {
    return false
  }
  if (topic.createdBy?.userId === caller.userId) {
    return true
  }
  if ((topic.supervisors ?? []).some((s) => s.userId === caller.userId)) {
    return true
  }
  if ((topic.examiners ?? []).some((e) => e.userId === caller.userId)) {
    return true
  }
  return false
}
