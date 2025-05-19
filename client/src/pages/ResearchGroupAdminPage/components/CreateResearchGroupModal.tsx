import { Button, Modal, Select, Stack, Text, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useState } from 'react'
import KeycloakUserAutocomplete from '../../../components/KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'

interface ICreateResearchGroupModalProps {
  opened: boolean
  onClose: () => void
  onSubmit: (values: ResearchGroupFormValues) => void
}

export interface ResearchGroupFormValues {
  name: string
  abbreviation: string
  campus: string
  description: string
  websiteUrl: string
  headUsername: string
}

const CreateResearchGroupModal = ({
  opened,
  onClose,
  onSubmit,
}: ICreateResearchGroupModalProps) => {
  const form = useForm<ResearchGroupFormValues>({
    initialValues: {
      name: '',
      abbreviation: '',
      campus: '',
      description: '',
      websiteUrl: '',
      headUsername: '',
    },
    validateInputOnChange: true,
    validate: {
      name: (value) => (value.length < 2 ? 'Name must be at least 2 characters' : null),
      headUsername: (value) => (!value ? 'Please select a group head' : null),
    },
  })

  const [headDisplayLabel, setHeadDisplayLabel] = useState('')

  return (
    <Modal opened={opened} onClose={onClose} title='Create Research Group' centered>
      <form onSubmit={form.onSubmit((values) => onSubmit(values))}>
        <Stack>
          <TextInput
            label='Name'
            placeholder='e.g., Intelligent Systems'
            withAsterisk
            {...form.getInputProps('name')}
          />

          <KeycloakUserAutocomplete
            selectedLabel={headDisplayLabel}
            onSelect={(username, label) => {
              form.setFieldValue('headUsername', username)
              setHeadDisplayLabel(label)
            }}
            label='Group Head'
            placeholder='Search by name or email...'
            withAsterisk
          />

          {form.values.headUsername && (
            <Text size='xs' c='dimmed'>
              Selected Head Username: {form.values.headUsername}
            </Text>
          )}

          <TextInput
            label='Abbreviation'
            placeholder='e.g., IS'
            {...form.getInputProps('abbreviation')}
          />
          <Select
            label='Campus'
            placeholder='Select a campus'
            data={['Garching', 'Munich', 'Heilbronn', 'Weihenstephan']}
            {...form.getInputProps('campus')}
          />
          <TextInput
            label='Website'
            type='url'
            placeholder='https://group-website.example.com'
            {...form.getInputProps('websiteUrl')}
          />
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

          <Button type='submit' fullWidth mt='md' disabled={!form.isValid()}>
            Create Research Group
          </Button>
        </Stack>
      </form>
    </Modal>
  )
}

export default CreateResearchGroupModal
