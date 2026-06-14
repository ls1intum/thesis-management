import { usePageTitle } from '@/core/hooks/theme'
import ThesisConfigSection from '@/thesis/pages/ThesisPage/components/ThesisConfigSection/ThesisConfigSection'
import ThesisInfoSection from '@/thesis/pages/ThesisPage/components/ThesisInfoSection/ThesisInfoSection'
import ThesisProposalSection from '@/thesis/pages/ThesisPage/components/ThesisProposalSection/ThesisProposalSection'
import ThesisWritingSection from '@/thesis/pages/ThesisPage/components/ThesisWritingSection/ThesisWritingSection'
import ThesisAssessmentSection from '@/thesis/pages/ThesisPage/components/ThesisAssessmentSection/ThesisAssessmentSection'
import ThesisFinalGradeSection from '@/thesis/pages/ThesisPage/components/ThesisFinalGradeSection/ThesisFinalGradeSection'
import { useParams } from 'react-router'
import { Alert, Stack } from '@mantine/core'
import { Warning } from '@phosphor-icons/react'
import ThesisHeader from '@/thesis/pages/ThesisPage/components/ThesisHeader/ThesisHeader'
import ThesisProvider from '@/thesis/providers/ThesisProvider/ThesisProvider'
import ThesisSupervisorCommentsSection from '@/thesis/pages/ThesisPage/components/ThesisSupervisorCommentsSection/ThesisSupervisorCommentsSection'
import ThesisStudentInfoSection from '@/thesis/pages/ThesisPage/components/ThesisStudentInfoSection/ThesisStudentInfoSection'
import ThesisPresentationSection from '@/thesis/pages/ThesisPage/components/ThesisPresentationSection/ThesisPresentationSection'
import { useLoadedThesisContext } from '@/thesis/providers/ThesisProvider/hooks'
import { formatDate } from '@/core/utils/format'

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
      <ThesisSupervisorCommentsSection />
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
