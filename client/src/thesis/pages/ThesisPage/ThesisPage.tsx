import { usePageTitle } from '@/core/hooks/theme'
import ThesisOverviewSection from '@/thesis/pages/ThesisPage/components/ThesisOverviewSection/ThesisOverviewSection'
import ThesisProposalSection from '@/thesis/pages/ThesisPage/components/ThesisProposalSection/ThesisProposalSection'
import ThesisWritingSection from '@/thesis/pages/ThesisPage/components/ThesisWritingSection/ThesisWritingSection'
import ThesisAssessmentSection from '@/thesis/pages/ThesisPage/components/ThesisAssessmentSection/ThesisAssessmentSection'
import ThesisFinalGradeSection from '@/thesis/pages/ThesisPage/components/ThesisFinalGradeSection/ThesisFinalGradeSection'
import ThesisPresentationSection from '@/thesis/pages/ThesisPage/components/ThesisPresentationSection/ThesisPresentationSection'
import ThesisProcessNav, {
  getAppShellHeaderOffset,
} from '@/thesis/pages/ThesisPage/components/ThesisProcessNav/ThesisProcessNav'
import type { IThesisProcessNavStep } from '@/thesis/pages/ThesisPage/components/ThesisProcessNav/ThesisProcessNav'
import { useParams } from 'react-router'
import { Alert, Stack } from '@mantine/core'
import { Warning } from '@phosphor-icons/react'
import ThesisHeader from '@/thesis/pages/ThesisPage/components/ThesisHeader/ThesisHeader'
import ThesisProvider from '@/thesis/providers/ThesisProvider/ThesisProvider'
import { useLoadedThesisContext } from '@/thesis/providers/ThesisProvider/hooks'
import { ThesisState } from '@/thesis/requests/responses/thesis'
import { checkMinimumThesisState, isThesisClosed } from '@/thesis/utils/thesis'
import { formatDate } from '@/core/utils/format'

const NAV_BAR_HEIGHT = 56
const SECTION_SCROLL_MARGIN = NAV_BAR_HEIGHT + 16

const ThesisPageContent = () => {
  const { thesis, access } = useLoadedThesisContext()

  const proposals = thesis.proposals ?? []
  const presentations = thesis.presentations ?? []
  const closed = isThesisClosed(thesis)

  const showProposal =
    checkMinimumThesisState(thesis, ThesisState.PROPOSAL) &&
    (thesis.state === ThesisState.PROPOSAL || proposals.length > 0)
  const showThesis = checkMinimumThesisState(thesis, ThesisState.WRITING)
  const showPresentation =
    checkMinimumThesisState(thesis, ThesisState.WRITING) &&
    (presentations.length > 0 || (access.student && !closed))
  const showAssessment = access.supervisor && checkMinimumThesisState(thesis, ThesisState.SUBMITTED)
  const showGrade = checkMinimumThesisState(thesis, ThesisState.ASSESSED) && access.student

  const reachedWriting = checkMinimumThesisState(thesis, ThesisState.WRITING)
  const reachedSubmitted = checkMinimumThesisState(thesis, ThesisState.SUBMITTED)
  const reachedAssessed = checkMinimumThesisState(thesis, ThesisState.ASSESSED)
  const reachedGraded = thesis.state === ThesisState.GRADED || thesis.state === ThesisState.FINISHED

  const steps: IThesisProcessNavStep[] = [
    { id: 'section-overview', label: 'Overview', isOverview: true },
  ]
  if (showProposal) {
    steps.push({
      id: 'section-proposal',
      label: 'Proposal',
      isCurrent: thesis.state === ThesisState.PROPOSAL,
      isCompleted: reachedWriting,
    })
  }
  if (showThesis) {
    steps.push({
      id: 'section-thesis',
      label: 'Thesis',
      isCurrent: thesis.state === ThesisState.WRITING,
      isCompleted: reachedSubmitted,
    })
  }
  if (showPresentation) {
    steps.push({
      id: 'section-presentation',
      label: 'Presentation',
      isCompleted: presentations.length > 0,
    })
  }
  if (showAssessment) {
    steps.push({
      id: 'section-assessment',
      label: 'Assessment',
      isCurrent: thesis.state === ThesisState.SUBMITTED,
      isCompleted: reachedAssessed,
    })
  }
  if (showGrade) {
    steps.push({
      id: 'section-grade',
      label: 'Grade',
      isCurrent: thesis.state === ThesisState.ASSESSED,
      isCompleted: reachedGraded,
    })
  }

  const anchorStyle = { scrollMarginTop: getAppShellHeaderOffset() + SECTION_SCROLL_MARGIN }

  return (
    <Stack>
      <ThesisHeader />
      {thesis.anonymizedAt && (
        <Alert color='orange' icon={<Warning />}>
          This thesis was anonymized on {formatDate(thesis.anonymizedAt)} per data retention policy.
          Personal data (files, comments, assessments, feedback, and role assignments) has been
          permanently removed.
        </Alert>
      )}
      <ThesisProcessNav steps={steps} />
      <section id='section-overview' style={anchorStyle}>
        <ThesisOverviewSection />
      </section>
      {showProposal && (
        <section id='section-proposal' style={anchorStyle}>
          <ThesisProposalSection />
        </section>
      )}
      {showThesis && (
        <section id='section-thesis' style={anchorStyle}>
          <ThesisWritingSection />
        </section>
      )}
      {showPresentation && (
        <section id='section-presentation' style={anchorStyle}>
          <ThesisPresentationSection />
        </section>
      )}
      {showAssessment && (
        <section id='section-assessment' style={anchorStyle}>
          <ThesisAssessmentSection />
        </section>
      )}
      {showGrade && (
        <section id='section-grade' style={anchorStyle}>
          <ThesisFinalGradeSection />
        </section>
      )}
    </Stack>
  )
}

const ThesisPage = () => {
  const { thesisId } = useParams<{ thesisId: string }>()

  usePageTitle('Thesis')

  return (
    <ThesisProvider thesisId={thesisId} requireLoadedThesis>
      <ThesisPageContent />
    </ThesisProvider>
  )
}

export default ThesisPage
