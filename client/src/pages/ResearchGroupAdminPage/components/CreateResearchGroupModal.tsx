import {
  Autocomplete,
  Button,
  Modal,
  Select,
  Stack,
  Text,
  Textarea,
  TextInput,
} from '@mantine/core'
import { useForm } from '@mantine/form'
import { useDebouncedValue } from '@mantine/hooks'
import React, { useEffect, useState } from 'react'
import { doRequest } from '../../../requests/request'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'

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
  headId: string
}

interface KeycloakUserElement {
  id: string
  username: string
  firstName: string
  lastName: string
  email: string
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
      headId: '',
    },

    validate: {
      name: (value) => (value.length < 2 ? 'Name must be at least 2 characters' : null),
      abbreviation: (value) => (!value ? 'Abbreviation is required' : null),
      campus: (value) => (!value ? 'Campus is required' : null),
      websiteUrl: (value) =>
        value && !/^https?:\/\/.+\..+/.test(value)
          ? 'Enter a valid website URL starting with http:// or https://'
          : null,
      headId: (value) => (!value ? 'Please select a group head' : null),
    },
  })

  const [headSearchKey, setHeadSearchKey] = useState('')
  const [debouncedSearch] = useDebouncedValue(headSearchKey, 300)

  const [headOptions, setHeadOptions] = useState<KeycloakUserElement[]>([])

  useEffect(() => {
    if (!debouncedSearch.trim()) {
      setHeadOptions([])
      return
    }

    doRequest<KeycloakUserElement[]>(
      '/v2/users/keycloak-users',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          searchKey: debouncedSearch.trim(),
        },
      },
      (res) => {
        if (res.ok) {
          setHeadOptions(res.data)
        } else {
          setHeadOptions([])
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [debouncedSearch])

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
          <TextInput
            label='Abbreviation'
            placeholder='e.g., IS'
            withAsterisk
            {...form.getInputProps('abbreviation')}
          />
          <Select
            label='Campus'
            placeholder='Select a campus'
            withAsterisk
            data={['Garching', 'Munich', 'Heilbronn', 'Weihenstephan']}
            {...form.getInputProps('campus')}
          />
          <TextInput
            label='Website'
            withAsterisk
            type='url'
            placeholder='https://group-website.example.com'
            {...form.getInputProps('websiteUrl')}
          />
          <Textarea
            label='Description'
            autosize
            minRows={3}
            maxLength={300}
            withAsterisk
            {...form.getInputProps('description')}
          />
          <Text size='xs' c='dimmed'>
            {form.values.description.length}/300 characters
          </Text>

          <Autocomplete
            label='Group Head'
            placeholder='Search by name or email...'
            value={headSearchKey}
            onChange={setHeadSearchKey}
            withAsterisk
            data={headOptions.map((user) => ({
              value: `${user.firstName} ${user.lastName} (${user.username}): ${user.email}`,
            }))}
            limit={10}
            onOptionSubmit={(val) => {
              const selected = headOptions.find(
                (u) => `${u.firstName} ${u.lastName} (${u.username}): ${u.email}` === val,
              )
              if (selected) {
                form.setFieldValue('headId', selected.username)
                setHeadSearchKey(val)
              }
            }}
          />

          {form.values.headId && (
            <Text size='xs' c='dimmed'>
              Selected Head ID: {form.values.headId}
            </Text>
          )}

          <Button type='submit' fullWidth mt='md'>
            Create Research Group
          </Button>
        </Stack>
      </form>
    </Modal>
  )
}

export default CreateResearchGroupModal
