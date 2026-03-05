import { IThesis } from '../../../../../../requests/responses/thesis'
import { Alert, Button, Modal, Stack, Text, TextInput } from '@mantine/core'
import { doRequest } from '../../../../../../requests/request'
import { useEffect, useState } from 'react'
import DocumentEditor from '../../../../../../components/DocumentEditor/DocumentEditor'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '../../../../../../providers/ThesisProvider/hooks'
import { ApiError } from '../../../../../../requests/handler'
import ThesisVisibilitySelect from '../../../ThesisVisibilitySelect/ThesisVisibilitySelect'

interface ISubmitFinalGradeModalProps {
  opened: boolean
  onClose: () => unknown
}

const SubmitFinalGradeModal = (props: ISubmitFinalGradeModalProps) => {
  const { opened, onClose } = props

  const { thesis } = useLoadedThesisContext()

  const [finalGrade, setFinalGrade] = useState('')
  const [feedback, setFeedback] = useState('')
  const [visibility, setVisibility] = useState(thesis.visibility)

  useEffect(() => {
    setFinalGrade(thesis.grade?.finalGrade || '')
    setFeedback(thesis.grade?.feedback || '')
    setVisibility(thesis.visibility)
  }, [thesis])

  const gradeComponents = thesis.assessment?.gradeComponents ?? []
  const calculatedGrade =
    gradeComponents.length > 0
      ? (() => {
          let weightedSum = 0
          let bonusSum = 0
          for (const c of gradeComponents) {
            if (c.isBonus) {
              bonusSum += c.grade
            } else {
              weightedSum += c.weight * c.grade
            }
          }
          let calc = weightedSum / 100 + bonusSum
          calc = Math.max(1.0, Math.min(5.0, calc))
          return Math.round(calc * 10) / 10
        })()
      : null

  const finalGradeNum = parseFloat(finalGrade)
  const deviationWarning =
    calculatedGrade !== null &&
    !isNaN(finalGradeNum) &&
    Math.abs(finalGradeNum - calculatedGrade) > 0.3

  const isEmpty = !finalGrade

  const [submitting, onGradeSubmit] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}/grade`, {
      method: 'POST',
      requiresAuth: true,
      data: {
        finalGrade,
        finalFeedback: feedback,
        visibility: visibility,
      },
    })

    if (response.ok) {
      onClose()

      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Final Grade submitted successfully')

  return (
    <Modal opened={opened} onClose={onClose} size='xl' title='Submit Final Grade'>
      <Stack gap='md'>
        <ThesisVisibilitySelect
          required
          label='Thesis Visibility'
          value={visibility}
          onChange={(e) => e && setVisibility(e)}
        />
        {calculatedGrade !== null && (
          <Text size='sm' c='dimmed'>
            Calculated from assessment components: {calculatedGrade}
          </Text>
        )}
        <TextInput
          required
          label='Final Grade'
          value={finalGrade}
          onChange={(e) => setFinalGrade(e.target.value)}
        />
        {deviationWarning && (
          <Alert color='orange' title='Grade Deviation'>
            The final grade ({finalGrade}) deviates from the calculated assessment grade (
            {calculatedGrade}) by more than 0.3.
          </Alert>
        )}
        <DocumentEditor
          label='Feedback (Visible to student)'
          value={feedback}
          onChange={(e) => setFeedback(e.target.value)}
          editMode={true}
        />
        <Button loading={submitting} onClick={onGradeSubmit} disabled={isEmpty}>
          Submit Grade
        </Button>
      </Stack>
    </Modal>
  )
}

export default SubmitFinalGradeModal
