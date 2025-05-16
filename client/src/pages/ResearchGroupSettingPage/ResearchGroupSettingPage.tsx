import { useEffect, useState } from 'react'
import { useParams } from 'react-router'
import { IResearchGroup } from '../../requests/responses/researchGroup'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import {
  Button,
  Loader,
  Select,
  Stack,
  Textarea,
  TextInput,
  Title,
  Text,
  Card,
  Grid,
} from '@mantine/core'
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
      <Card
        bg='transparent'
        withBorder
        shadow='sm'
        radius='md'
        w='100%'
        style={{ display: 'flex', flexDirection: 'column' }}
      >
        <Stack>
          <Stack gap={5}>
            <Title order={3}>Group Information</Title>
            <Text size='sm' c='dimmed'>
              Edit the basic information about your research group.
            </Text>
          </Stack>

          <form onSubmit={form.onSubmit(handleSubmit)}>
            <Grid gutter='md'>
              <Grid.Col span={{ base: 12, md: 6 }}>
                <TextInput label='Name' withAsterisk {...form.getInputProps('name')} />
              </Grid.Col>

              <Grid.Col span={{ base: 12, md: 6 }}>
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
              </Grid.Col>

              <Grid.Col span={{ base: 12, md: 6 }}>
                <TextInput label='Abbreviation' {...form.getInputProps('abbreviation')} />
              </Grid.Col>

              <Grid.Col span={{ base: 12, md: 6 }}>
                <Select
                  label='Campus'
                  placeholder='Select a campus'
                  data={['Garching', 'Munich', 'Heilbronn', 'Weihenstephan']}
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
                  Save Changes
                </Button>
              </Grid.Col>
            </Grid>
          </form>
        </Stack>
      </Card>

      <Card
        bg='transparent'
        withBorder
        shadow='sm'
        radius='md'
        w='100%'
        style={{ display: 'flex', flexDirection: 'column' }}
      >
        <Stack>
          <Stack gap={5}>
            <Title order={3}>Group Members</Title>
            <Text size='sm' c='dimmed'>
              Manage the members of your research group.
            </Text>
          </Stack>
        </Stack>
      </Card>
    </Stack>
  )
}

export default ResearchGroupSettingPage
