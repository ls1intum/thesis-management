import {
  ThesisFeedbackCategory,
  ThesisFeedbackSeverity,
  ThesisFeedbackSource,
} from '@/thesis/requests/responses/thesis'

export const humanizeFeedbackCategory = (
  value: ThesisFeedbackCategory | string | null | undefined,
): string => {
  if (!value) return ''
  return String(value)
    .toLowerCase()
    .replace(/(^\w|_\w)/g, (m) => m.replace('_', ' ').toUpperCase())
}

export const SEVERITY_COLOR: Record<ThesisFeedbackSeverity, string> = {
  [ThesisFeedbackSeverity.CRITICAL]: 'red',
  [ThesisFeedbackSeverity.MAJOR]: 'orange',
  [ThesisFeedbackSeverity.MINOR]: 'yellow',
  [ThesisFeedbackSeverity.SUGGESTION]: 'blue',
}

export const SEVERITY_DESCRIPTION: Record<ThesisFeedbackSeverity, string> = {
  [ThesisFeedbackSeverity.CRITICAL]: 'Must be fixed before submission.',
  [ThesisFeedbackSeverity.MAJOR]: 'Should be fixed before submission.',
  [ThesisFeedbackSeverity.MINOR]: 'Nice to fix, but not blocking.',
  [ThesisFeedbackSeverity.SUGGESTION]: 'An optional improvement.',
}

export const CATEGORY_DESCRIPTION: Record<ThesisFeedbackCategory, string> = {
  [ThesisFeedbackCategory.FORMATTING]: 'Layout, headings, and general document formatting.',
  [ThesisFeedbackCategory.STRUCTURE]: 'Required sections, chapter order, and overall structure.',
  [ThesisFeedbackCategory.CITATION]: 'Bibliography, references, and citation style.',
  [ThesisFeedbackCategory.METHODOLOGY]: 'Research approach, design, and rigor.',
  [ThesisFeedbackCategory.WRITING]: 'Writing style, grammar, and clarity.',
  [ThesisFeedbackCategory.FIGURES]: 'Figures, diagrams, and tables.',
  [ThesisFeedbackCategory.LOGIC]: 'Argumentation and logical consistency.',
  [ThesisFeedbackCategory.COMPLETENESS]: 'Missing content or insufficient detail.',
  [ThesisFeedbackCategory.OTHER]: "Doesn't fit any other category.",
}

export const SOURCE_LABEL: Record<ThesisFeedbackSource, string> = {
  [ThesisFeedbackSource.AI]: 'AI',
  [ThesisFeedbackSource.HUMAN]: 'Instructor',
  [ThesisFeedbackSource.AI_REVIEWED_BY_HUMAN]: 'AI + Instructor',
}

export const SOURCE_COLOR: Record<ThesisFeedbackSource, string> = {
  [ThesisFeedbackSource.AI]: 'grape',
  [ThesisFeedbackSource.HUMAN]: 'gray',
  [ThesisFeedbackSource.AI_REVIEWED_BY_HUMAN]: 'teal',
}

export const SOURCE_DESCRIPTION: Record<ThesisFeedbackSource, string> = {
  [ThesisFeedbackSource.AI]: 'Generated automatically by the AI review — not reviewed by a human.',
  [ThesisFeedbackSource.HUMAN]: 'Written manually by an instructor.',
  [ThesisFeedbackSource.AI_REVIEWED_BY_HUMAN]:
    'An AI-generated finding an instructor reviewed and approved before saving.',
}

export type AIAssessment = 'GOOD' | 'ACCEPTABLE' | 'NEEDS_WORK'

export const ASSESSMENT_LABEL: Record<AIAssessment, string> = {
  GOOD: 'Good — ready to submit with minor tweaks',
  ACCEPTABLE: 'Acceptable — some work needed',
  NEEDS_WORK: 'Needs work — significant issues remain',
}

export const ASSESSMENT_COLOR: Record<AIAssessment, string> = {
  GOOD: 'green',
  ACCEPTABLE: 'yellow',
  NEEDS_WORK: 'red',
}
