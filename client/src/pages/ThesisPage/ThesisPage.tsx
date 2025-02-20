import { usePageTitle } from '../../hooks/theme'
import ThesisConfigSection from './components/ThesisConfigSection/ThesisConfigSection'
import ThesisInfoSection from './components/ThesisInfoSection/ThesisInfoSection'
import ThesisProposalSection from './components/ThesisProposalSection/ThesisProposalSection'
import ThesisWritingSection from './components/ThesisWritingSection/ThesisWritingSection'
import ThesisAssessmentSection from './components/ThesisAssessmentSection/ThesisAssessmentSection'
import ThesisFinalGradeSection from './components/ThesisFinalGradeSection/ThesisFinalGradeSection'
import { useParams } from 'react-router'
import { Stack } from '@mantine/core'
import ThesisHeader from './components/ThesisHeader/ThesisHeader'
import ThesisProvider from '../../providers/ThesisProvider/ThesisProvider'
import ThesisAdvisorCommentsSection from './components/ThesisAdvisorCommentsSection/ThesisAdvisorCommentsSection'
import ThesisStudentInfoSection from './components/ThesisStudentInfoSection/ThesisStudentInfoSection'
import ThesisPresentationSection from './components/ThesisPresentationSection/ThesisPresentationSection'

const ThesisPage = () => {
  const { thesisId } = useParams<{ thesisId: string }>()

  usePageTitle('Thesis')

  return (
    <ThesisProvider thesisId={thesisId} requireLoadedThesis>
      <Stack>
        <ThesisHeader />
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
    </ThesisProvider>
  )
}

export default ThesisPage
