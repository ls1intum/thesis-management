import { Select, Textarea, TextInput, Text, Button, Grid, Group } from '@mantine/core'
import { useForm } from '@mantine/form'
import KeycloakUserAutocomplete from '../KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'
import { GLOBAL_CONFIG } from '../../config/global'
import { useState } from 'react'
import type { ResearchGroupFormValues } from '../../pages/ResearchGroupAdminPage/components/CreateResearchGroupModal'
import type { IResearchGroup } from '../../requests/responses/researchGroup'

interface IResearchGroupFormProps {
  initialResearchGroup?: Partial<IResearchGroup>
  onSubmit: (values: ResearchGroupFormValues) => void
  submitLabel?: string
  layout?: 'grid' | 'stack'
}

const getInitialValues = (initial: Partial<IResearchGroup> | undefined) => ({
  name: initial?.name ?? '',
  abbreviation: initial?.abbreviation ?? '',
  campus: initial?.campus ?? '',
  description: initial?.description ?? '',
  websiteUrl: initial?.websiteUrl ?? '',
  headUsername: initial?.head?.universityId ?? '',
})

const getInitialHeadLabel = (initial: Partial<IResearchGroup> | undefined): string =>
  initial?.head ? `${initial.head.firstName} ${initial.head.lastName}` : ''

const ResearchGroupForm = ({
  initialResearchGroup: initialFormValues = {},
  onSubmit,
  submitLabel = 'Submit',
  layout = 'stack',
}: IResearchGroupFormProps) => {
  const descriptionMaxLength = 500
  const initialValues = getInitialValues(initialFormValues)
  // Discard only makes sense in the edit flow — on create there's nothing
  // meaningful to revert to.
  const isEditing = Boolean(initialFormValues?.id) || Boolean(initialFormValues?.name)

  const form = useForm({
    initialValues,
    validateInputOnChange: true,
    validate: {
      name: (value) => (value.trim().length < 2 ? 'Name must be at least 2 characters' : null),
      headUsername: (value) => (!value ? 'Please select a group head' : null),
      websiteUrl: (value) => {
        if (value && !/^https?:\/\/[^\s/$.?#].[^\s]*$/.test(value)) {
          return 'Please enter a valid URL'
        }
        return null
      },
      description: (value) =>
        value.length > descriptionMaxLength
          ? `Description must be ${descriptionMaxLength} characters or less`
          : null,
      abbreviation: (value) => {
        if (!value) {
          return 'Abbreviation is required'
        }
        if (value.length > 10) {
          return 'Abbreviation must be 10 characters or less'
        }
        return null
      },
    },
  })

  const [headDisplayLabel, setHeadDisplayLabel] = useState(getInitialHeadLabel(initialFormValues))

  // Bumping this counter on Discard remounts the KeycloakUserAutocomplete
  // child so its internal selectedUsername state is cleared along with the
  // parent's headUsername / headDisplayLabel. Without this, the autocomplete
  // would still show the previously-selected head as "already selected".
  const [autocompleteResetKey, setAutocompleteResetKey] = useState(0)

  const hasChanges = (Object.keys(initialValues) as Array<keyof typeof initialValues>).some(
    (key) => initialValues[key] !== form.values[key],
  )

  const handleDiscard = () => {
    // setInitialValues + reset re-syncs Mantine's internal dirty/touched
    // tracking and clearErrors drops any stale messages from
    // validateInputOnChange — without this the form keeps showing red
    // errors on fields that were just restored to valid values.
    form.setInitialValues(initialValues)
    form.reset()
    form.clearErrors()
    setHeadDisplayLabel(getInitialHeadLabel(initialFormValues))
    setAutocompleteResetKey((k) => k + 1)
  }

  return (
    <form onSubmit={form.onSubmit(onSubmit)}>
      <Grid gap='md'>
        <Grid.Col span={layout === 'grid' ? { base: 12, md: 6 } : 12}>
          <TextInput
            label='Name'
            placeholder='e.g., Intelligent Systems'
            withAsterisk
            {...form.getInputProps('name')}
          />
        </Grid.Col>

        <Grid.Col span={layout === 'grid' ? { base: 12, md: 6 } : 12}>
          <KeycloakUserAutocomplete
            key={`head-autocomplete-${autocompleteResetKey}`}
            selectedLabel={headDisplayLabel}
            onSelect={(username, label) => {
              form.setFieldValue('headUsername', username)
              setHeadDisplayLabel(label)
            }}
            label='Group Head'
            placeholder='Search by name or email...'
            withAsterisk
            previousUser={initialFormValues.head}
          />
        </Grid.Col>

        <Grid.Col span={layout === 'grid' ? { base: 12, md: 6 } : 12}>
          <TextInput
            label='Abbreviation'
            placeholder='e.g., IS'
            {...form.getInputProps('abbreviation')}
            withAsterisk
          />
        </Grid.Col>

        <Grid.Col span={layout === 'grid' ? { base: 12, md: 6 } : 12}>
          <Select
            label='Campus'
            placeholder='Select a campus'
            data={Object.values(GLOBAL_CONFIG.research_groups_location)}
            {...form.getInputProps('campus')}
          />
        </Grid.Col>

        <Grid.Col span={12}>
          <TextInput
            label='Website'
            type='url'
            placeholder='https://group-website.example.com'
            {...form.getInputProps('websiteUrl')}
          />
        </Grid.Col>

        <Grid.Col span={12}>
          <Textarea
            label='Description'
            autosize
            minRows={3}
            maxLength={descriptionMaxLength}
            {...form.getInputProps('description')}
          />
          <Text size='xs' c='dimmed'>
            {(form.values.description ?? '').length}/{descriptionMaxLength} characters
          </Text>
        </Grid.Col>

        <Grid.Col span={12}>
          <Group justify='flex-end' mt='md'>
            {isEditing && (
              <Button variant='default' disabled={!hasChanges} onClick={handleDiscard}>
                Discard changes
              </Button>
            )}
            <Button type='submit' disabled={!form.isValid() || !hasChanges}>
              {submitLabel}
            </Button>
          </Group>
        </Grid.Col>
      </Grid>
    </form>
  )
}

export default ResearchGroupForm
