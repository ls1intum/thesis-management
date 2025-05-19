import { useForm } from '@mantine/form'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { doRequest } from '../../../requests/request'
import { showNotification } from '@mantine/notifications'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { useParams } from 'react-router'
import { Button, Grid, Select, Textarea, TextInput, Text, Loader } from '@mantine/core'
import KeycloakUserAutocomplete from '../../../components/KeycloakUserAutocomplete.tsx/KeycloakUserAutocomplete'
import { useState } from 'react'
import { IResearchGroup } from '../../../requests/responses/researchGroup'

interface IGeneralResearchGroupSettingsProps {
  researchGroupData: IResearchGroup | undefined
  setResearchGroupData: (data: IResearchGroup) => void
}

const GeneralResearchGroupSettings = ({
  researchGroupData,
  setResearchGroupData,
}: IGeneralResearchGroupSettingsProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const [headDisplayLabel, setHeadDisplayLabel] = useState(
    `${researchGroupData?.head.firstName} ${researchGroupData?.head.lastName}`,
  )

  const form = useForm({
    initialValues: {
      name: researchGroupData?.name || '',
      abbreviation: researchGroupData?.abbreviation || '',
      campus: researchGroupData?.campus || '',
      description: researchGroupData?.description || '',
      websiteUrl: researchGroupData?.websiteUrl || '',
      headUsername: researchGroupData?.head.universityId || '',
    },
    validateInputOnChange: true,
    validate: {
      name: (value) => (value.length < 2 ? 'Name must be at least 2 characters' : null),
      headUsername: (value) => (!value ? 'Please select a group head' : null),
    },
  })

  const handleSubmit = (values: typeof form.values) => {
    if (!researchGroupId) return

    doRequest<IResearchGroup>(
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
          setResearchGroupData(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <ResearchGroupSettingsCard
      title='Group Information'
      subtle='Edit the basic information about your research group.'
    >
      <form onSubmit={form.onSubmit(handleSubmit)}>
        <Grid gutter='md'>
          <Grid.Col span={{ base: 12, md: 6 }}>
            <TextInput label='Name' withAsterisk {...form.getInputProps('name')} />
          </Grid.Col>

          <Grid.Col span={{ base: 12, md: 6 }}>
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
    </ResearchGroupSettingsCard>
  )
}

export default GeneralResearchGroupSettings
