import { Button, Modal, Select, Stack, TextInput } from '@mantine/core'
import { isNotEmpty, useForm } from '@mantine/form'
import { GLOBAL_CONFIG } from '../../../../config/global'
import React, { useState } from 'react'
import UserMultiSelect from '../../../../components/UserMultiSelect/UserMultiSelect'
import { useNavigate } from 'react-router'
import { doRequest } from '../../../../requests/request'
import { IThesis } from '../../../../requests/responses/thesis'
import { isNotEmptyUserList } from '../../../../utils/validation'
import { showSimpleError } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { formatThesisType, getDefaultLanguage } from '../../../../utils/format'
import LanguageSelect from '../../../../components/LanguageSelect/LanguageSelect'

interface ICreateThesisModalProps {
  opened: boolean
  onClose: () => unknown
}

const CreateThesisModal = (props: ICreateThesisModalProps) => {
  const { opened, onClose } = props

  const navigate = useNavigate()

  const form = useForm<{
    title: string
    type: string | null
    language: string | null
    students: string[]
    advisors: string[]
    supervisors: string[]
  }>({
    mode: 'controlled',
    initialValues: {
      title: '',
      type: null,
      language: getDefaultLanguage(),
      students: [],
      advisors: [],
      supervisors: GLOBAL_CONFIG.default_supervisors,
    },
    validateInputOnBlur: true,
    validate: {
      title: isNotEmpty('Thesis title must not be empty'),
      type: isNotEmpty('Thesis type must not be empty'),
      language: isNotEmpty('Thesis language must not be empty'),
      students: isNotEmptyUserList('student'),
      advisors: isNotEmptyUserList('advisor'),
      supervisors: isNotEmptyUserList('supervisor'),
    },
  })

  const [loading, setLoading] = useState(false)

  return (
    <Modal opened={opened} onClose={onClose} title='Create Thesis'>
      <form
        onSubmit={form.onSubmit(async (values) => {
          setLoading(true)

          try {
            const response = await doRequest<IThesis>('/v2/theses', {
              method: 'POST',
              requiresAuth: true,
              data: {
                thesisTitle: values.title,
                thesisType: values.type,
                language: values.language,
                studentIds: values.students,
                advisorIds: values.advisors,
                supervisorIds: values.supervisors,
              },
            })

            if (response.ok) {
              navigate(`/theses/${response.data.thesisId}`)
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
            type='text'
            required={true}
            placeholder='Thesis Title'
            label='Thesis Title'
            {...form.getInputProps('title')}
          />
          <Select
            label='Thesis Type'
            required={true}
            data={Object.keys(GLOBAL_CONFIG.thesis_types).map((key) => ({
              value: key,
              label: formatThesisType(key),
            }))}
            {...form.getInputProps('type')}
          />
          <LanguageSelect
            label='Thesis Language'
            required={true}
            {...form.getInputProps('language')}
          />
          <UserMultiSelect
            label='Student(s)'
            required={true}
            groups={[]}
            {...form.getInputProps('students')}
          />
          <UserMultiSelect
            label='Advisor(s)'
            required={true}
            groups={['advisor', 'supervisor']}
            {...form.getInputProps('advisors')}
          />
          <UserMultiSelect
            label='Supervisor'
            required={true}
            groups={['supervisor']}
            maxValues={1}
            {...form.getInputProps('supervisors')}
          />
          <Button type='submit' loading={loading} disabled={!form.isValid()}>
            Create Thesis
          </Button>
        </Stack>
      </form>
    </Modal>
  )
}

export default CreateThesisModal
