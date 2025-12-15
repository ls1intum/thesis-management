import { ILightUser } from '../requests/responses/user'
import { IThesis, ThesisState } from '../requests/responses/thesis'
import { ApplicationState, IApplication } from '../requests/responses/application'
import { GLOBAL_CONFIG } from '../config/global'
import { InterviewState } from '../requests/responses/interview'
import { useMantineColorScheme } from '@mantine/core'

interface IFormatDateOptions {
  withTime: boolean
}

export function formatDate(
  date: string | Date | undefined | null,
  options: Partial<IFormatDateOptions> = {},
): string {
  const { withTime }: IFormatDateOptions = {
    withTime: true,
    ...options,
  }

  if (typeof date === 'undefined' || date === null) {
    return ''
  }

  const item = new Date(date)

  return item.toLocaleDateString(undefined, {
    year: '2-digit',
    month: '2-digit',
    day: 'numeric',
    hour: withTime ? 'numeric' : undefined,
    minute: withTime ? 'numeric' : undefined,
  })
}

interface IFormatUserOptions {
  withUniversityId: boolean
}

export function formatUser(user: ILightUser, options: Partial<IFormatUserOptions> = {}) {
  const { withUniversityId } = {
    withUniversityId: false,
    ...options,
  }

  let text = `${user.firstName} ${user.lastName}`

  if (withUniversityId) {
    text += ` (${user.universityId})`
  }

  return text
}

export function wordsToFilename(words: string) {
  return words.replace(' ', ' ')
}

export function formatUserFilename(user: ILightUser): string {
  return wordsToFilename(`${user.firstName} ${user.lastName}`)
}

export function formatUsersFilename(users: ILightUser[]) {
  return users.map((user) => formatUserFilename(user)).join(' ')
}

export function formatThesisFilename(
  thesis: IThesis,
  name: string,
  originalFilename: string,
  version: number,
) {
  let text = `${wordsToFilename(formatThesisType(thesis.type, true))}`

  if (name) {
    text += ` ${name}`
  }

  text += ` ${formatUsersFilename(thesis.students)}`

  if (version > 0) {
    text += ` v${version}`
  }

  const fileParts = originalFilename.split('.')

  text += `.${fileParts[fileParts.length - 1]}`

  return text
}

export function formatApplicationFilename(
  application: IApplication,
  name: string,
  originalFilename: string,
) {
  let text = `Application`

  if (name) {
    text += ` ${name}`
  }

  text += ` ${formatUserFilename(application.user)}`

  const fileParts = originalFilename.split('.')

  text += `.${fileParts[fileParts.length - 1]}`

  return text
}

export function formatThesisState(state: ThesisState) {
  const stateMap: Record<ThesisState, string> = {
    [ThesisState.PROPOSAL]: 'Proposal',
    [ThesisState.WRITING]: 'Writing',
    [ThesisState.SUBMITTED]: 'Submitted',
    [ThesisState.ASSESSED]: 'Assessed',
    [ThesisState.GRADED]: 'Graded',
    [ThesisState.FINISHED]: 'Finished',
    [ThesisState.DROPPED_OUT]: 'Dropped out',
  }

  return stateMap[state]
}

export function formatApplicationState(state: ApplicationState) {
  const stateMap: Record<ApplicationState, string> = {
    [ApplicationState.ACCEPTED]: 'Accepted',
    [ApplicationState.REJECTED]: 'Rejected',
    [ApplicationState.NOT_ASSESSED]: 'Not assessed',
    [ApplicationState.INTERVIEWING]: 'Interviewing',
  }

  return stateMap[state]
}

export function formatLanguage(language: string) {
  return GLOBAL_CONFIG.languages[language] ?? language
}

export function getDefaultLanguage() {
  const languages = Object.keys(GLOBAL_CONFIG.languages)

  if (languages.length === 1) {
    return languages[0]
  }

  return null
}

export function formatPresentationType(type: string) {
  if (type === 'INTERMEDIATE') {
    return 'Intermediate'
  }

  if (type === 'FINAL') {
    return 'Final'
  }

  return type
}

export function formatThesisType(type: string | null | undefined, short = false) {
  if (!type) {
    return ''
  }

  if (short) {
    return GLOBAL_CONFIG.thesis_types[type]?.short ?? type
  }

  return GLOBAL_CONFIG.thesis_types[type]?.long ?? type
}

export function pluralize(word: string, count: number) {
  if (count === 1) {
    return word
  }

  return `${word}s`
}

export function formatPresentationState(state: string) {
  if (state === 'DRAFTED') {
    return 'Draft'
  }

  if (state === 'SCHEDULED') {
    return 'Scheduled'
  }

  return state
}

export function scoreColorTranslate(score: number | null, light: boolean = true): string {
  switch (score) {
    case 1:
      return light ? 'red.2' : 'red.9'
    case 2:
      return light ? 'orange.2' : 'orange.9'
    case 3:
      return light ? 'yellow.2' : 'yellow.9'
    case 4:
      return light ? 'lime.2' : 'lime.9'
    case 5:
      return light ? 'green.2' : 'green.9'
    default:
      return light ? 'gray.2' : 'gray.9'
  }
}

export function createScoreLabel(score: number): string {
  switch (score) {
    case 0:
      return 'No Show'
    case 1:
      return 'Not a Fit'
    case 2:
      return 'Some Concerns'
    case 3:
      return 'Meets expectations'
    case 4:
      return 'Great Candidate'
    case 5:
      return 'Excelent'
    default:
      return 'No Score'
  }
}

export function createInterviewStageLabel(score: number): string {
  switch (score) {
    case 1:
      return 'Uncontacted'
    case 2:
      return 'Invited'
    case 3:
      return 'Scheduled'
    case 4:
      return 'Great Candidate'
    case 5:
      return 'Completed'
    default:
      return 'All'
  }
}

export function getInterviewStateColor(state: InterviewState): string {
  const colorScheme = useMantineColorScheme()

  switch (state) {
    case InterviewState.UNCONTACTED:
      return 'primary.1'
    case InterviewState.INVITED:
      return 'primary.3'
    case InterviewState.SCHEDULED:
      return 'primary.5'
    case InterviewState.COMPLETED:
      return colorScheme.colorScheme === 'dark' ? 'primary.8' : 'primary.10'
    default:
      return 'gray'
  }
}

export function formateStudyProgram(program: string) {
  return GLOBAL_CONFIG.study_programs[program] ?? program
}
