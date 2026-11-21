import type { IThesis } from '@/thesis/requests/responses/thesis'
import { ThesisState } from '@/thesis/requests/responses/thesis'
import { useState } from 'react'
import { Accordion, Button, Group, Stack, Text } from '@mantine/core'
import SubmitFinalGradeModal from '@/thesis/pages/ThesisPage/components/ThesisFinalGradeSection/components/SubmitFinalGradeModal/SubmitFinalGradeModal'
import { doRequest } from '@/core/requests/request'
import DocumentEditor from '@/core/components/DocumentEditor/DocumentEditor'
import { checkMinimumThesisState, isThesisClosed } from '@/thesis/utils/thesis'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '@/thesis/providers/ThesisProvider/hooks'
import { ApiError } from '@/core/requests/handler'
import LabeledItem from '@/core/components/LabeledItem/LabeledItem'

const ThesisFinalGradeSection = () => {
  const { thesis, access } = useLoadedThesisContext()

  const [finalGradeModal, setFinalGradeModal] = useState(false)

  const [submitting, onThesisComplete] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}/complete`, {
      method: 'POST',
      requiresAuth: true,
    })

    if (response.ok) {
      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Thesis successfully marked as finished')

  if (!checkMinimumThesisState(thesis, ThesisState.ASSESSED)) {
    return null
  }

  if (!access.student) {
    return null
  }

  return (
    <Accordion variant='separated' defaultValue='open'>
      <Accordion.Item value='open'>
        <Accordion.Control>Final Grade</Accordion.Control>
        <Accordion.Panel>
          <Stack>
            {thesis.grade ? (
              <Stack>
                {thesis.grade.feedback && (
                  <DocumentEditor label='Feedback' value={thesis.grade.feedback} />
                )}
                <LabeledItem label='Grade' value={thesis.grade.finalGrade} />
              </Stack>
            ) : (
              <Text ta='center'>No grade added yet</Text>
            )}
            <Group ml='auto'>
              {access.examiner && !isThesisClosed(thesis) && (
                <Button ml='auto' onClick={() => setFinalGradeModal(true)}>
                  {thesis.grade ? 'Edit Final Grade' : 'Add Final Grade'}
                </Button>
              )}
              {access.examiner && thesis.state === ThesisState.GRADED && (
                <Button ml='auto' onClick={onThesisComplete} loading={submitting}>
                  Mark thesis as finished
                </Button>
              )}
            </Group>
          </Stack>
          <SubmitFinalGradeModal
            opened={finalGradeModal}
            onClose={() => setFinalGradeModal(false)}
          />
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  )
}

export default ThesisFinalGradeSection
