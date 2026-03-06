import { ThesisState } from '../../../../requests/responses/thesis'
import { useState } from 'react'
import { Accordion, Badge, Button, Group, Stack, Table, Text } from '@mantine/core'
import ReplaceAssessmentModal from './components/ReplaceAssessmentModal/ReplaceAssessmentModal'
import DocumentEditor from '../../../../components/DocumentEditor/DocumentEditor'
import { checkMinimumThesisState, isThesisClosed } from '../../../../utils/thesis'
import { useLoadedThesisContext } from '../../../../providers/ThesisProvider/hooks'
import LabeledItem from '../../../../components/LabeledItem/LabeledItem'
import { formatThesisFilename } from '../../../../utils/format'
import { AuthenticatedFileDownloadButton } from '../../../../components/AuthenticatedFileDownloadButton/AuthenticatedFileDownloadButton'
import { calculateGradeFromComponents } from '../../../../utils/grade'

const ThesisAssessmentSection = () => {
  const { thesis, access } = useLoadedThesisContext()

  const [assessmentModal, setAssessmentModal] = useState(false)

  if (!access.supervisor || !checkMinimumThesisState(thesis, ThesisState.SUBMITTED)) {
    return <></>
  }

  const gradeComponents = thesis.assessment?.gradeComponents ?? []
  const calculatedGrade =
    gradeComponents.length > 0 ? calculateGradeFromComponents(gradeComponents) : null

  return (
    <Accordion variant='separated' defaultValue='open'>
      <Accordion.Item value='open'>
        <Accordion.Control>
          <Group gap='xs'>
            <Text>Assessment</Text>
            <Badge color='grey'>Not visible to student</Badge>
          </Group>
        </Accordion.Control>
        <Accordion.Panel>
          <Stack>
            {thesis.assessment ? (
              <Stack>
                <DocumentEditor label='Summary' value={thesis.assessment.summary} />
                <DocumentEditor label='Strengths' value={thesis.assessment.positives} />
                <DocumentEditor label='Weaknesses' value={thesis.assessment.negatives} />

                {gradeComponents.length > 0 && (
                  <Stack gap='xs'>
                    <Text fw={500} size='sm'>
                      Grade Components
                    </Text>
                    <Table>
                      <Table.Thead>
                        <Table.Tr>
                          <Table.Th>Name</Table.Th>
                          <Table.Th>Weight</Table.Th>
                          <Table.Th>Grade</Table.Th>
                        </Table.Tr>
                      </Table.Thead>
                      <Table.Tbody>
                        {gradeComponents.map((c) => (
                          <Table.Tr key={c.gradeComponentId}>
                            <Table.Td>{c.name}</Table.Td>
                            <Table.Td>
                              {c.isBonus ? <Badge color='blue'>Bonus</Badge> : `${c.weight}%`}
                            </Table.Td>
                            <Table.Td>{c.grade}</Table.Td>
                          </Table.Tr>
                        ))}
                      </Table.Tbody>
                    </Table>
                    <Text size='sm' fw={500}>
                      Calculated Grade: {calculatedGrade}
                    </Text>
                  </Stack>
                )}

                <LabeledItem label='Grade Suggestion' value={thesis.assessment.gradeSuggestion} />
              </Stack>
            ) : (
              <Text ta='center'>No assessment added yet</Text>
            )}
            <Group>
              {thesis.assessment && (
                <AuthenticatedFileDownloadButton
                  url={`/v2/theses/${thesis.thesisId}/assessment`}
                  filename={formatThesisFilename(thesis, 'Assessment', 'assessment.pdf', 0)}
                  variant='outline'
                >
                  Download as PDF
                </AuthenticatedFileDownloadButton>
              )}
              {access.supervisor && !isThesisClosed(thesis) && (
                <Button ml='auto' onClick={() => setAssessmentModal(true)}>
                  {thesis.assessment ? 'Edit Assessment' : 'Add Assessment'}
                </Button>
              )}
            </Group>
          </Stack>
          <ReplaceAssessmentModal
            opened={assessmentModal}
            onClose={() => setAssessmentModal(false)}
          />
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  )
}

export default ThesisAssessmentSection
