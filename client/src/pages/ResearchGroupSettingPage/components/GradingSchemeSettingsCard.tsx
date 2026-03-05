import {
  ActionIcon,
  Alert,
  Button,
  Checkbox,
  Group,
  NumberInput,
  Stack,
  Table,
  TextInput,
} from '@mantine/core'
import { ArrowDown, ArrowUp, Plus, Trash } from '@phosphor-icons/react'
import { useState } from 'react'
import { useParams } from 'react-router'
import { doRequest } from '../../../requests/request'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import {
  IGradingSchemeComponent,
  IResearchGroupSettings,
  IResearchGroupSettingsGradingScheme,
} from '../../../requests/responses/researchGroupSettings'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'

interface GradingSchemeSettingsCardProps {
  gradingSchemeSettings?: IResearchGroupSettingsGradingScheme
  setGradingSchemeSettings: (settings: IResearchGroupSettingsGradingScheme) => void
}

const GradingSchemeSettingsCard = ({
  gradingSchemeSettings,
  setGradingSchemeSettings,
}: GradingSchemeSettingsCardProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()
  const [saving, setSaving] = useState(false)

  const components = gradingSchemeSettings?.components ?? []

  const regularWeightSum = components
    .filter((c) => !c.isBonus)
    .reduce((sum, c) => sum + (c.weight ?? 0), 0)

  const weightsValid = components.length === 0 || regularWeightSum === 100

  const updateComponent = (index: number, updates: Partial<IGradingSchemeComponent>) => {
    const updated = components.map((c, i) => (i === index ? { ...c, ...updates } : c))
    setGradingSchemeSettings({ components: updated })
  }

  const addComponent = () => {
    setGradingSchemeSettings({
      components: [
        ...components,
        { name: '', weight: 0, isBonus: false, position: components.length },
      ],
    })
  }

  const removeComponent = (index: number) => {
    setGradingSchemeSettings({
      components: components.filter((_, i) => i !== index),
    })
  }

  const moveComponent = (index: number, direction: -1 | 1) => {
    const newIndex = index + direction
    if (newIndex < 0 || newIndex >= components.length) return
    const updated = [...components]
    ;[updated[index], updated[newIndex]] = [updated[newIndex], updated[index]]
    setGradingSchemeSettings({ components: updated })
  }

  const handleSave = () => {
    setSaving(true)
    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          gradingSchemeSettings: {
            components: components.map((c, i) => ({ ...c, position: i })),
          },
        },
      },
      (res) => {
        if (res.ok) {
          setGradingSchemeSettings(res.data.gradingSchemeSettings ?? { components: [] })
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setSaving(false)
      },
    )
  }

  const hasEmptyNames = components.some((c) => !c.name?.trim())

  return (
    <ResearchGroupSettingsCard
      title='Grading Scheme'
      subtle='Define the default grading components for thesis assessments in this research group. Supervisors can use these to break down grades into weighted components.'
    >
      <Stack>
        {components.length > 0 && (
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Name</Table.Th>
                <Table.Th w={120}>Weight (%)</Table.Th>
                <Table.Th w={80}>Bonus</Table.Th>
                <Table.Th w={120}>Actions</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {components.map((component, index) => (
                <Table.Tr key={index}>
                  <Table.Td>
                    <TextInput
                      value={component.name}
                      onChange={(e) => updateComponent(index, { name: e.target.value })}
                      placeholder='Component name'
                      size='sm'
                    />
                  </Table.Td>
                  <Table.Td>
                    <NumberInput
                      value={component.weight}
                      onChange={(val) =>
                        updateComponent(index, { weight: typeof val === 'number' ? val : 0 })
                      }
                      min={0}
                      max={100}
                      suffix='%'
                      size='sm'
                      disabled={component.isBonus}
                    />
                  </Table.Td>
                  <Table.Td>
                    <Checkbox
                      checked={component.isBonus}
                      onChange={(e) =>
                        updateComponent(index, {
                          isBonus: e.currentTarget.checked,
                          weight: e.currentTarget.checked ? 0 : component.weight,
                        })
                      }
                    />
                  </Table.Td>
                  <Table.Td>
                    <Group gap='xs'>
                      <ActionIcon
                        variant='subtle'
                        size='sm'
                        onClick={() => moveComponent(index, -1)}
                        disabled={index === 0}
                      >
                        <ArrowUp size={14} />
                      </ActionIcon>
                      <ActionIcon
                        variant='subtle'
                        size='sm'
                        onClick={() => moveComponent(index, 1)}
                        disabled={index === components.length - 1}
                      >
                        <ArrowDown size={14} />
                      </ActionIcon>
                      <ActionIcon
                        variant='subtle'
                        color='red'
                        size='sm'
                        onClick={() => removeComponent(index)}
                      >
                        <Trash size={14} />
                      </ActionIcon>
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}

        {!weightsValid && (
          <Alert color='yellow' title='Weight Warning'>
            Regular component weights sum to {regularWeightSum}%, but must sum to 100%.
          </Alert>
        )}

        <Group>
          <Button
            variant='outline'
            leftSection={<Plus size={14} />}
            onClick={addComponent}
            size='sm'
          >
            Add Component
          </Button>
          <Button
            onClick={handleSave}
            loading={saving}
            disabled={!weightsValid || hasEmptyNames}
            size='sm'
            ml='auto'
          >
            Save Grading Scheme
          </Button>
        </Group>
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default GradingSchemeSettingsCard
