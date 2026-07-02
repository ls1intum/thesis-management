import { Select, Textarea, TextInput, Text, Button, Grid, Group } from '@mantine/core'
import { useForm } from '@mantine/form'
import KeycloakUserAutocomplete from '@/core/user/components/KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'
import { GLOBAL_CONFIG } from '@/core/config/global'
import { useEffect, useRef, useState } from 'react'
import type { ResearchGroupFormValues } from '@/core/group/pages/ResearchGroupAdminPage/components/CreateResearchGroupModal'
import type { IResearchGroup } from '@/core/group/requests/responses/researchGroup'

interface IResearchGroupFormProps {
  initialResearchGroup?: Partial<IResearchGroup>
  onSubmit: (values: ResearchGroupFormValues) => void
  submitLabel?: string
  layout?: 'grid' | 'stack'
}

// On edit, headUsername starts empty and is only populated when the user picks
// a new head via the autocomplete — the server treats an empty value as
// "keep the existing head", which lets the form avoid requiring the current
// head's universityId (no longer returned by the public research-group DTO).
const getInitialValues = (initial: Partial<IResearchGroup> | undefined) => ({
  name: initial?.name ?? '',
  abbreviation: initial?.abbreviation ?? '',
  campus: initial?.campus ?? '',
  description: initial?.description ?? '',
  websiteUrl: initial?.websiteUrl ?? '',
  headUsername: '',
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
      headUsername: (value) => (!isEditing && !value ? 'Please select a group head' : null),
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

  const [headDisplayLabel, setHeadDisplayLabel] = useState(() =>
    getInitialHeadLabel(initialFormValues),
  )

  // Tracks whether the user has interacted with the head autocomplete. The
  // `headUsername` form value alone can't tell us this in edit mode because
  // its resting state is the empty-sentinel "keep the existing head", so a
  // fresh empty field and "user cleared the field" look identical. When the
  // flag is set, hasChanges flips true and — if the user hasn't picked a
  // replacement — the Save button is blocked with an inline error.
  const [headTouched, setHeadTouched] = useState(false)

  // Bumping this counter on Discard remounts the KeycloakUserAutocomplete
  // child so its internal selectedUsername state is cleared along with the
  // parent's headUsername / headDisplayLabel. Without this, the autocomplete
  // would still show the previously-selected head as "already selected".
  const [autocompleteResetKey, setAutocompleteResetKey] = useState(0)

  // Resync form state when the parent hands over a fresh IResearchGroup
  // (typically after a successful save that returned a new head or updated
  // fields). Without this, form.values keep whatever the user last typed and
  // hasChanges stays stuck at true even though the server has accepted the
  // change. Skip the first run so mounting doesn't cause an extra remount.
  const isFirstSync = useRef(true)
  useEffect(() => {
    if (isFirstSync.current) {
      isFirstSync.current = false
      return
    }
    const nextValues = getInitialValues(initialFormValues)
    form.setInitialValues(nextValues)
    form.setValues(nextValues)
    form.clearErrors()
    setHeadDisplayLabel(getInitialHeadLabel(initialFormValues))
    setHeadTouched(false)
    setAutocompleteResetKey((k) => k + 1)
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- `form` is stable across renders; depending on it would loop
  }, [
    initialFormValues.id,
    initialFormValues.head?.userId,
    initialFormValues.head?.firstName,
    initialFormValues.head?.lastName,
    initialFormValues.name,
    initialFormValues.abbreviation,
    initialFormValues.campus,
    initialFormValues.description,
    initialFormValues.websiteUrl,
  ])

  const hasFieldChanges = (Object.keys(initialValues) as Array<keyof typeof initialValues>).some(
    (key) => initialValues[key] !== form.values[key],
  )
  const hasChanges = hasFieldChanges || headTouched

  // In edit mode the user may clear the autocomplete without picking anyone;
  // headUsername stays '' (the "keep current" sentinel) but the user's intent
  // is now ambiguous, so we block Save and surface an inline error until
  // they either pick a replacement or Discard.
  const headMissing = isEditing && headTouched && !form.values.headUsername

  const handleDiscard = () => {
    // setInitialValues + reset re-syncs Mantine's internal dirty/touched
    // tracking and clearErrors drops any stale messages from
    // validateInputOnChange — without this the form keeps showing red
    // errors on fields that were just restored to valid values.
    form.setInitialValues(initialValues)
    form.reset()
    form.clearErrors()
    setHeadDisplayLabel(getInitialHeadLabel(initialFormValues))
    setHeadTouched(false)
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
              setHeadTouched(true)
            }}
            label='Group Head'
            placeholder='Search by name or email...'
            withAsterisk
            error={headMissing ? 'Please select a group head' : undefined}
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
            <Button type='submit' disabled={!form.isValid() || !hasChanges || headMissing}>
              {submitLabel}
            </Button>
          </Group>
        </Grid.Col>
      </Grid>
    </form>
  )
}

export default ResearchGroupForm
