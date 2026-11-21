import { useEffect, useState } from 'react'
import { Button, Checkbox, Modal, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { doRequest } from '@/core/requests/request'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import type { ISchool } from '@/core/organization/requests/responses/organization'
import { isHttpUrl } from '@/core/organization/utils'

interface ISchoolFormModalProps {
  opened: boolean
  school?: ISchool
  onClose: () => void
  onSaved: () => void
}

const emptyValues = {
  name: '',
  abbreviation: '',
  websiteUrl: '',
  thesisPortalUrl: '',
  active: true,
}

const SchoolFormModal = ({ opened, school, onClose, onSaved }: ISchoolFormModalProps) => {
  const [loading, setLoading] = useState(false)

  const form = useForm({
    initialValues: emptyValues,
    validate: {
      name: (value) => (value.trim() ? null : 'Please enter a name'),
      abbreviation: (value) =>
        value.trim()
          ? value.length > 50
            ? 'At most 50 characters'
            : null
          : 'Please enter an abbreviation',
      websiteUrl: (value) => (isHttpUrl(value) ? null : 'Please enter a valid http(s) URL'),
      thesisPortalUrl: (value) => (isHttpUrl(value) ? null : 'Please enter a valid http(s) URL'),
    },
  })

  useEffect(() => {
    if (opened) {
      form.setValues(
        school
          ? {
              name: school.name,
              abbreviation: school.abbreviation,
              websiteUrl: school.websiteUrl ?? '',
              thesisPortalUrl: school.thesisPortalUrl ?? '',
              active: school.active !== false,
            }
          : emptyValues,
      )
      form.resetDirty()
      form.clearErrors()
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- form is stable; only re-seed when the modal opens for another school
  }, [opened, school])

  return (
    <Modal opened={opened} onClose={onClose} title={school ? 'Edit School' : 'Add School'}>
      <form
        onSubmit={form.onSubmit(async (values) => {
          setLoading(true)

          try {
            const response = await doRequest<ISchool>(
              school ? `/v2/schools/${school.id}` : '/v2/schools',
              {
                method: school ? 'PUT' : 'POST',
                requiresAuth: true,
                data: {
                  name: values.name,
                  abbreviation: values.abbreviation,
                  websiteUrl: values.websiteUrl || null,
                  thesisPortalUrl: values.thesisPortalUrl || null,
                  active: values.active,
                },
              },
            )

            if (response.ok) {
              showSimpleSuccess(school ? 'School updated' : 'School created')
              onSaved()
              onClose()
            } else {
              showSimpleError(getApiResponseErrorMessage(response))
            }
          } finally {
            setLoading(false)
          }
        })}
      >
        <Stack gap='md'>
          <TextInput
            label='Name'
            placeholder='e.g. TUM School of Management'
            required
            {...form.getInputProps('name')}
          />
          <TextInput
            label='Abbreviation'
            placeholder='e.g. MGT'
            required
            {...form.getInputProps('abbreviation')}
          />
          <TextInput
            label='Website'
            placeholder='https://www.example.edu/'
            {...form.getInputProps('websiteUrl')}
          />
          <TextInput
            label='Thesis portal'
            description='Students of this school submit their final thesis here. Leave empty to use the default portal.'
            placeholder='https://portal.example.edu/'
            {...form.getInputProps('thesisPortalUrl')}
          />
          <Checkbox
            label='Active'
            description='Inactive schools cannot be selected anymore, but existing assignments are kept.'
            {...form.getInputProps('active', { type: 'checkbox' })}
          />
          <Button type='submit' loading={loading}>
            {school ? 'Save' : 'Create'}
          </Button>
        </Stack>
      </form>
    </Modal>
  )
}

export default SchoolFormModal
