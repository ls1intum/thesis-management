import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Group,
  Modal,
  NumberInput,
  Stack,
  Table,
  Text,
  TextInput,
} from '@mantine/core'
import { Plus, Trash } from '@phosphor-icons/react'
import { useEffect, useState } from 'react'
import DocumentEditor from '../../../../../../components/DocumentEditor/DocumentEditor'
import { doRequest } from '../../../../../../requests/request'
import { IThesis } from '../../../../../../requests/responses/thesis'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '../../../../../../providers/ThesisProvider/hooks'
import { ApiError } from '../../../../../../requests/handler'
import { IResearchGroupSettingsGradingScheme } from '../../../../../../requests/responses/researchGroupSettings'
import { calculateGradeFromComponents } from '../../../../../../utils/grade'

interface IGradeComponent {
  key: string
  name: string
  weight: number
  isBonus: boolean
  grade: number | string
}

function calculateGrade(components: IGradeComponent[]): number | null {
  if (components.length === 0) return null
  if (
    components.some((c) => c.grade === '' || c.grade === undefined || typeof c.grade !== 'number')
  )
    return null

  return calculateGradeFromComponents(
    components.map((c) => ({ weight: c.weight, isBonus: c.isBonus, grade: Number(c.grade) })),
  )
}

interface IReplaceAssessmentModalProps {
  opened: boolean
  onClose: () => unknown
}

const ReplaceAssessmentModal = (props: IReplaceAssessmentModalProps) => {
  const { opened, onClose } = props

  const { thesis } = useLoadedThesisContext()

  const [summary, setSummary] = useState('')
  const [positives, setPositives] = useState('')
  const [negatives, setNegatives] = useState('')
  const [gradeSuggestion, setGradeSuggestion] = useState('')
  const [gradeComponents, setGradeComponents] = useState<IGradeComponent[]>([])
  const [schemeLoaded, setSchemeLoaded] = useState(false)
  const [gradeSuggestionManuallyEdited, setGradeSuggestionManuallyEdited] = useState(false)

  const isEmpty = !summary || !positives || !negatives || !gradeSuggestion

  const calculatedGrade = calculateGrade(gradeComponents)

  const regularWeightSum = gradeComponents
    .filter((c) => !c.isBonus)
    .reduce((sum, c) => sum + (c.weight ?? 0), 0)
  const weightsValid = gradeComponents.length === 0 || Math.abs(regularWeightSum - 100) < 0.01
  const allGradesFilled = gradeComponents.every(
    (c) => typeof c.grade === 'number' && !isNaN(c.grade),
  )
  const allNamesFilled = gradeComponents.every((c) => c.name?.trim())
  const gradeComponentsValid =
    gradeComponents.length === 0 || (weightsValid && allGradesFilled && allNamesFilled)

  const gradeSuggestionNum = parseFloat(gradeSuggestion)
  const deviationWarning =
    calculatedGrade !== null &&
    !isNaN(gradeSuggestionNum) &&
    Math.abs(gradeSuggestionNum - calculatedGrade) > 0.3

  const [submitting, onSave] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}/assessment`, {
      method: 'POST',
      requiresAuth: true,
      data: {
        summary,
        positives,
        negatives,
        gradeSuggestion,
        gradeComponents:
          gradeComponents.length > 0
            ? gradeComponents.map((c, i) => ({
                name: c.name,
                weight: c.weight,
                isBonus: c.isBonus,
                grade: Number(c.grade),
                position: i,
              }))
            : undefined,
      },
    })

    if (response.ok) {
      onClose()

      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Assessment submitted successfully')

  useEffect(() => {
    setSummary(thesis.assessment?.summary || '')
    setPositives(thesis.assessment?.positives || '')
    setNegatives(thesis.assessment?.negatives || '')
    setGradeSuggestion(thesis.assessment?.gradeSuggestion || '')

    setGradeSuggestionManuallyEdited(false)

    if (thesis.assessment?.gradeComponents && thesis.assessment.gradeComponents.length > 0) {
      setGradeComponents(
        thesis.assessment.gradeComponents.map((c) => ({
          key: crypto.randomUUID(),
          name: c.name,
          weight: c.weight,
          isBonus: c.isBonus,
          grade: c.grade,
        })),
      )
      setSchemeLoaded(true)
    } else {
      setSchemeLoaded(false)
    }
  }, [thesis.assessment])

  useEffect(() => {
    if (opened && !schemeLoaded && thesis.researchGroup?.id) {
      doRequest<IResearchGroupSettingsGradingScheme>(
        `/v2/research-group-settings/${thesis.researchGroup.id}/grading-scheme`,
        { method: 'GET', requiresAuth: true },
        (res) => {
          if (!res.ok) {
            return
          }
          if (res.data.components && res.data.components.length > 0) {
            setGradeComponents(
              res.data.components.map((c) => ({
                key: crypto.randomUUID(),
                name: c.name,
                weight: c.weight,
                isBonus: c.isBonus,
                grade: '' as const,
              })),
            )
          }
          setSchemeLoaded(true)
        },
      )
    }
  }, [opened, schemeLoaded, thesis.researchGroup?.id])

  useEffect(() => {
    if (calculatedGrade !== null && !gradeSuggestionManuallyEdited) {
      setGradeSuggestion(String(calculatedGrade))
    }
  }, [calculatedGrade, gradeSuggestionManuallyEdited])

  const updateComponent = (index: number, updates: Partial<IGradeComponent>) => {
    setGradeComponents((prev) => prev.map((c, i) => (i === index ? { ...c, ...updates } : c)))
  }

  const addComponent = () => {
    setGradeComponents((prev) => [
      ...prev,
      { key: crypto.randomUUID(), name: '', weight: 0, isBonus: false, grade: '' },
    ])
  }

  const removeComponent = (index: number) => {
    setGradeComponents((prev) => prev.filter((_, i) => i !== index))
  }

  return (
    <Modal opened={opened} onClose={onClose} size='xl' title='Submit Assessment'>
      <Stack gap='md'>
        <DocumentEditor
          required
          label='Summary'
          value={summary}
          editMode={true}
          onChange={(e) => setSummary(e.target.value)}
          maxLength={2000}
        />
        <DocumentEditor
          required
          label='Strengths'
          value={positives}
          editMode={true}
          onChange={(e) => setPositives(e.target.value)}
          maxLength={2000}
        />
        <DocumentEditor
          required
          label='Weaknesses'
          value={negatives}
          editMode={true}
          onChange={(e) => setNegatives(e.target.value)}
          maxLength={2000}
        />

        {gradeComponents.length > 0 && (
          <Stack gap='xs'>
            <Text fw={500} size='sm'>
              Grade Components
            </Text>
            <Table>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Name</Table.Th>
                  <Table.Th w={100}>Weight</Table.Th>
                  <Table.Th w={100}>Grade</Table.Th>
                  <Table.Th w={60}></Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {gradeComponents.map((component, index) => (
                  <Table.Tr key={component.key}>
                    <Table.Td>
                      <TextInput
                        value={component.name}
                        onChange={(e) => updateComponent(index, { name: e.target.value })}
                        size='sm'
                      />
                    </Table.Td>
                    <Table.Td>
                      {component.isBonus ? (
                        <Badge color='teal'>Bonus</Badge>
                      ) : (
                        <NumberInput
                          value={component.weight}
                          onChange={(val) =>
                            updateComponent(index, {
                              weight: typeof val === 'number' ? val : 0,
                            })
                          }
                          min={0}
                          max={100}
                          suffix='%'
                          size='sm'
                        />
                      )}
                    </Table.Td>
                    <Table.Td>
                      <NumberInput
                        value={component.grade}
                        onChange={(val) =>
                          updateComponent(index, {
                            grade: val === '' ? '' : val,
                          })
                        }
                        min={component.isBonus ? -5 : 1}
                        max={5}
                        step={0.1}
                        decimalScale={1}
                        allowDecimal
                        placeholder={component.isBonus ? 'e.g. 0.3 or -0.3' : 'e.g. 1.3'}
                        size='sm'
                      />
                    </Table.Td>
                    <Table.Td>
                      <ActionIcon
                        variant='subtle'
                        color='red'
                        size='sm'
                        onClick={() => removeComponent(index)}
                        aria-label='Remove component'
                      >
                        <Trash size={14} />
                      </ActionIcon>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>

            <Group>
              <Button
                variant='subtle'
                leftSection={<Plus size={14} />}
                onClick={addComponent}
                size='xs'
              >
                Add Component
              </Button>
            </Group>

            {!weightsValid && (
              <Alert color='yellow' title='Weight Warning'>
                Regular component weights sum to {regularWeightSum}%, but must equal 100%.
              </Alert>
            )}

            {gradeComponents.some((c) => c.isBonus) && (
              <Text size='xs' c='dimmed'>
                Bonus components adjust the final grade directly (e.g. 0.3 improves by 0.3, -0.3
                penalizes by 0.3).
              </Text>
            )}

            {calculatedGrade !== null && (
              <Text size='sm' fw={500}>
                Calculated Grade: {calculatedGrade}
              </Text>
            )}
          </Stack>
        )}

        {gradeComponents.length === 0 && (
          <Button variant='subtle' leftSection={<Plus size={14} />} onClick={addComponent}>
            Add Grade Components
          </Button>
        )}

        {deviationWarning && (
          <Alert color='orange' title='Grade Deviation'>
            The grade suggestion ({gradeSuggestion}) deviates from the calculated grade (
            {calculatedGrade}) by more than 0.3.
          </Alert>
        )}

        <TextInput
          label='Grade Suggestion'
          required
          value={gradeSuggestion}
          onChange={(e) => {
            setGradeSuggestion(e.target.value)
            setGradeSuggestionManuallyEdited(true)
          }}
        />
        <Button onClick={onSave} disabled={isEmpty || !gradeComponentsValid} loading={submitting}>
          Submit Assessment
        </Button>
      </Stack>
    </Modal>
  )
}

export default ReplaceAssessmentModal
