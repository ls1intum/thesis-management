import { useEffect, useState } from 'react'
import { Button, Checkbox, Modal, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { doRequest } from '@/core/requests/request'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import type { IDepartment, ISchool } from '@/core/organization/requests/responses/organization'

interface IDepartmentFormModalProps {
  opened: boolean
  school?: ISchool
  department?: IDepartment
  onClose: () => void
  onSaved: () => void
}

const emptyValues = { name: '', abbreviation: '', active: true }

const DepartmentFormModal = ({
  opened,
  school,
  department,
  onClose,
  onSaved,
}: IDepartmentFormModalProps) => {
  const [loading, setLoading] = useState(false)

  const form = useForm({
    initialValues: emptyValues,
    validate: {
      name: (value) => (value.trim() ? null : 'Please enter a name'),
    },
  })

  useEffect(() => {
    if (opened) {
      form.setValues(
        department
          ? {
              name: department.name,
              abbreviation: department.abbreviation ?? '',
              active: department.active !== false,
            }
          : emptyValues,
      )
      form.resetDirty()
      form.clearErrors()
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- form is stable; only re-seed when the modal opens for another department
  }, [opened, department])

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={department ? 'Edit Department' : `Add Department to ${school?.name ?? 'School'}`}
    >
      <form
        onSubmit={form.onSubmit(async (values) => {
          if (!school) {
            return
          }

          setLoading(true)

          try {
            const response = await doRequest<IDepartment>(
              department ? `/v2/departments/${department.id}` : '/v2/departments',
              {
                method: department ? 'PUT' : 'POST',
                requiresAuth: true,
                data: {
                  schoolId: school.id,
                  name: values.name,
                  abbreviation: values.abbreviation || null,
                  active: values.active,
                },
              },
            )

            if (response.ok) {
              showSimpleSuccess(department ? 'Department updated' : 'Department created')
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
            placeholder='e.g. Computer Science'
            required
            {...form.getInputProps('name')}
          />
          <TextInput
            label='Abbreviation'
            placeholder='e.g. CS'
            {...form.getInputProps('abbreviation')}
          />
          <Checkbox
            label='Active'
            description='Inactive departments cannot be selected anymore, but existing assignments are kept.'
            {...form.getInputProps('active', { type: 'checkbox' })}
          />
          <Button type='submit' loading={loading}>
            {department ? 'Save' : 'Create'}
          </Button>
        </Stack>
      </form>
    </Modal>
  )
}

export default DepartmentFormModal
