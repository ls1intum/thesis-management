import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { IResearchGroup } from '../../requests/responses/researchGroup'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { Button, Loader, Select, Stack, Textarea, TextInput, Title, Text } from '@mantine/core'
import KeycloakUserAutocomplete from '../../components/KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'
import { useForm } from '@mantine/form'
import { showNotification } from '@mantine/notifications'

const ResearchGroupSettingPage = () => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const [loading, setLoading] = useState(true)

  const form = useForm({
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

  // Fetch research group data on load
  useEffect(() => {
    if (!researchGroupId) return

    setLoading(true)

    doRequest<IResearchGroup>(
      `/v2/research-groups/${researchGroupId}`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          if (res.ok) {
            form.setValues({
              name: res.data.name,
              abbreviation: res.data.abbreviation,
              campus: res.data.campus,
              description: res.data.description,
              websiteUrl: res.data.websiteUrl,
              headUsername: res.data.head.universityId,
            })
            setHeadDisplayLabel(`${res.data.head.firstName} ${res.data.head.lastName}`)
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setLoading(false)
      },
    )
  }, [researchGroupId])

  const handleSubmit = (values: typeof form.values) => {
    if (!researchGroupId) return

    doRequest(
      `/v2/research-groups/${researchGroupId}`,
      {
        method: 'PUT',
        requiresAuth: true,
        data: {
          headUsername: values.headUsername,
          name: values.name,
          abbreviation: values.abbreviation,
          campus: values.campus,
          description: values.description,
          websiteUrl: values.websiteUrl,
        },
      },
      (res) => {
        if (res.ok) {
          showNotification({
            title: 'Success',
            message: 'Research group updated successfully.',
            color: 'green',
          })
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  if (loading) return <Loader />

  return (
    <Stack>
      <Title>Research Group Settings</Title>

      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Stack>
          <TextInput label='Name' withAsterisk {...form.getInputProps('name')} />

          <KeycloakUserAutocomplete
            username={form.values.headUsername}
            selectedLabel={headDisplayLabel}
            onSelect={(username, label) => {
              form.setFieldValue('headUsername', username)
              setHeadDisplayLabel(label)
            }}
            label='Group Head'
            placeholder='Search by name or email...'
            withAsterisk
          />

          <TextInput label='Abbreviation' {...form.getInputProps('abbreviation')} />

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
            Save Changes
          </Button>
        </Stack>
      </form>
    </Stack>
  )
}

export default ResearchGroupSettingPage
