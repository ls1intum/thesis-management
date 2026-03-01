import { usePageTitle } from '../../hooks/theme'
import ThesisConfigSection from './components/ThesisConfigSection/ThesisConfigSection'
import ThesisInfoSection from './components/ThesisInfoSection/ThesisInfoSection'
import ThesisProposalSection from './components/ThesisProposalSection/ThesisProposalSection'
import ThesisWritingSection from './components/ThesisWritingSection/ThesisWritingSection'
import ThesisAssessmentSection from './components/ThesisAssessmentSection/ThesisAssessmentSection'
import ThesisFinalGradeSection from './components/ThesisFinalGradeSection/ThesisFinalGradeSection'
import { useParams } from 'react-router'
import { Alert, Stack } from '@mantine/core'
import { Warning } from '@phosphor-icons/react'
import ThesisHeader from './components/ThesisHeader/ThesisHeader'
import ThesisProvider from '../../providers/ThesisProvider/ThesisProvider'
import ThesisAdvisorCommentsSection from './components/ThesisAdvisorCommentsSection/ThesisAdvisorCommentsSection'
import ThesisStudentInfoSection from './components/ThesisStudentInfoSection/ThesisStudentInfoSection'
import ThesisPresentationSection from './components/ThesisPresentationSection/ThesisPresentationSection'
import { useLoadedThesisContext } from '../../providers/ThesisProvider/hooks'
import { formatDate } from '../../utils/format'

const ThesisPageContent = () => {
  const { thesis } = useLoadedThesisContext()

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
      <ThesisConfigSection />
      <ThesisStudentInfoSection />
      <ThesisAdvisorCommentsSection />
      <ThesisInfoSection />
      <ThesisProposalSection />
      <ThesisWritingSection />
      <ThesisPresentationSection />
      <ThesisAssessmentSection />
      <ThesisFinalGradeSection />
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
