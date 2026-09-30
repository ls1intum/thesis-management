import { useEffect, useState } from 'react'
import { Button, Checkbox, Modal, Select, Stack, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { doRequest } from '@/core/requests/request'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import type { ISchool, IStudyProgram } from '@/core/organization/requests/responses/organization'

interface IStudyProgramFormModalProps {
  opened: boolean
  studyProgram?: IStudyProgram
  schools: ISchool[]
  onClose: () => void
  onSaved: () => void
}

const emptyValues = { name: '', key: '', schoolId: null as string | null, active: true }

const StudyProgramFormModal = ({
  opened,
  studyProgram,
  schools,
  onClose,
  onSaved,
}: IStudyProgramFormModalProps) => {
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
        studyProgram
          ? {
              name: studyProgram.name,
              key: studyProgram.key,
              schoolId: studyProgram.school?.id ?? null,
              active: studyProgram.active !== false,
            }
          : emptyValues,
      )
      form.resetDirty()
      form.clearErrors()
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- form is stable; only re-seed when the modal opens for another study program
  }, [opened, studyProgram])

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={studyProgram ? 'Edit Study Program' : 'Add Study Program'}
    >
      <form
        onSubmit={form.onSubmit(async (values) => {
          setLoading(true)

          try {
            const response = await doRequest<IStudyProgram>(
              studyProgram ? `/v2/study-programs/${studyProgram.id}` : '/v2/study-programs',
              {
                method: studyProgram ? 'PUT' : 'POST',
                requiresAuth: true,
                data: {
                  name: values.name,
                  key: values.key || null,
                  schoolId: values.schoolId,
                  active: values.active,
                },
              },
            )

            if (response.ok) {
              showSimpleSuccess(studyProgram ? 'Study program updated' : 'Study program created')
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
            placeholder='e.g. Informatics'
            required
            {...form.getInputProps('name')}
          />
          <Select
            label='School'
            description='The school decides, for example, which portal students submit their thesis to.'
            placeholder='No school'
            clearable
            searchable
            data={schools
              .filter((school) => school.active !== false || school.id === form.values.schoolId)
              .map((school) => ({
                value: school.id,
                label: `${school.name} (${school.abbreviation})`,
              }))}
            {...form.getInputProps('schoolId')}
          />
          <TextInput
            label='Key'
            description='Stable identifier. Generated from the name when left empty.'
            placeholder='e.g. COMPUTER_SCIENCE'
            {...form.getInputProps('key')}
          />
          <Checkbox
            label='Active'
            description='Inactive study programs cannot be selected anymore, but existing assignments are kept.'
            {...form.getInputProps('active', { type: 'checkbox' })}
          />
          <Button type='submit' loading={loading}>
            {studyProgram ? 'Save' : 'Create'}
          </Button>
        </Stack>
      </form>
    </Modal>
  )
}

export default StudyProgramFormModal
