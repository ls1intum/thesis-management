import { Button, Modal, Select, Stack, TextInput } from '@mantine/core'
import { isNotEmpty, useForm } from '@mantine/form'
import { GLOBAL_CONFIG } from '../../../../config/global'
import React, { useEffect, useState } from 'react'
import UserMultiSelect from '../../../../components/UserMultiSelect/UserMultiSelect'
import { useNavigate } from 'react-router'
import { doRequest } from '../../../../requests/request'
import { IThesis } from '../../../../requests/responses/thesis'
import { isNotEmptyUserList } from '../../../../utils/validation'
import { showSimpleError } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { formatThesisType, getDefaultLanguage } from '../../../../utils/format'
import LanguageSelect from '../../../../components/LanguageSelect/LanguageSelect'
import { PaginationResponse } from '../../../../requests/responses/pagination'
import { ILightResearchGroup } from '../../../../requests/responses/researchGroup'
import { useHasGroupAccess } from '../../../../hooks/authentication'

interface ICreateThesisModalProps {
  opened: boolean
  onClose: () => unknown
}

const CreateThesisModal = (props: ICreateThesisModalProps) => {
  const { opened, onClose } = props

  const navigate = useNavigate()

  const [loading, setLoading] = useState(false)
  const [researchGroups, setResearchGroups] = useState<PaginationResponse<ILightResearchGroup>>()
  const hasAdminAccess = useHasGroupAccess('admin')

  const form = useForm<{
    title: string
    type: string | null
    language: string | null
    students: string[]
    advisors: string[]
    supervisors: string[]
    researchGroupId: string
  }>({
    mode: 'controlled',
    initialValues: {
      title: '',
      type: null,
      language: getDefaultLanguage(),
      students: [],
      advisors: [],
      supervisors: [],
      researchGroupId: '',
    },
    validateInputOnBlur: true,
    validate: {
      title: isNotEmpty('Thesis title must not be empty'),
      type: isNotEmpty('Thesis type must not be empty'),
      language: isNotEmpty('Thesis language must not be empty'),
      students: isNotEmptyUserList('student'),
      advisors: isNotEmptyUserList('advisor'),
      supervisors: isNotEmptyUserList('supervisor'),
      researchGroupId: isNotEmpty('Research group must not be empty'),
    },
  })

  useEffect(() => {
    setLoading(true)
    return doRequest<PaginationResponse<ILightResearchGroup>>(
      '/v2/research-groups',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          page: 0,
          limit: -1,
        },
      },
      (res) => {
        if (res.ok) {
          setResearchGroups({
            ...res.data,
            content: res.data.content,
          })

          if (res.data.content.length === 1) {
            form.setValues({
              researchGroupId: res.data.content[0].id,
              supervisors: [res.data.content[0].head.userId],
            })
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))

          setResearchGroups({
            content: [],
            totalPages: 0,
            totalElements: 0,
            last: true,
            pageNumber: 0,
            pageSize: -1,
          })
        }
        setLoading(false)
      },
    )
  }, [opened])

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
                researchGroupId: values.researchGroupId,
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
          <Select
            label='Research Group'
            required
            nothingFoundMessage={!loading ? 'Nothing found...' : 'Loading...'}
            disabled={loading || !researchGroups || !hasAdminAccess}
            data={researchGroups?.content.map((researchGroup: ILightResearchGroup) => ({
              label: researchGroup.name,
              value: researchGroup.id,
            }))}
            {...form.getInputProps('researchGroupId')}
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
