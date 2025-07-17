import { Select, Textarea, TextInput, Text, Button, Grid } from '@mantine/core'
import { useForm } from '@mantine/form'
import KeycloakUserAutocomplete from '../KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'
import { GLOBAL_CONFIG } from '../../config/global'
import { useState } from 'react'
import { ResearchGroupFormValues } from '../../pages/ResearchGroupAdminPage/components/CreateResearchGroupModal'
import { IResearchGroup } from '../../requests/responses/researchGroup'

interface IResearchGroupFormProps {
  initialResearchGroup?: Partial<IResearchGroup>
  onSubmit: (values: ResearchGroupFormValues) => void
  submitLabel?: string
  layout?: 'grid' | 'stack'
}

const ResearchGroupForm = ({
  initialResearchGroup: initialFormValues = {},
  onSubmit,
  submitLabel = 'Submit',
  layout = 'stack',
}: IResearchGroupFormProps) => {
  const form = useForm({
    initialValues: {
      name: initialFormValues?.name || '',
      abbreviation: initialFormValues?.abbreviation || '',
      campus: initialFormValues?.campus || '',
      description: initialFormValues?.description || '',
      websiteUrl: initialFormValues?.websiteUrl || '',
      headUsername: initialFormValues?.head?.universityId || '',
    },
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
        value.length > 300 ? 'Description must be 300 characters or less' : null,
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

  const [headDisplayLabel, setHeadDisplayLabel] = useState(
    initialFormValues?.head
      ? `${initialFormValues.head.firstName} ${initialFormValues.head.lastName}`
      : '',
  )

  return (
    <form onSubmit={form.onSubmit(onSubmit)}>
      <Grid gutter='md'>
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
            maxLength={300}
            {...form.getInputProps('description')}
          />
          <Text size='xs' c='dimmed'>
            {form.values.description.length}/300 characters
          </Text>
        </Grid.Col>

        <Grid.Col span={12}>
          <Button type='submit' fullWidth mt='md' disabled={!form.isValid()}>
            {submitLabel}
          </Button>
        </Grid.Col>
      </Grid>
    </form>
  )
}

export default ResearchGroupForm
