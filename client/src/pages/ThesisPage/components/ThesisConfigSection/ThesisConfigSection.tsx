import { IThesis, ThesisState } from '../../../../requests/responses/thesis'
import { Accordion, Button, Group, Select, Stack, TagsInput, Text, TextInput } from '@mantine/core'
import { useEffect } from 'react'
import { isNotEmpty, useForm } from '@mantine/form'
import { DateInput, DateTimePicker, DateValue } from '@mantine/dates'
import UserMultiSelect from '../../../../components/UserMultiSelect/UserMultiSelect'
import { isNotEmptyUserList } from '../../../../utils/validation'
import { isThesisClosed } from '../../../../utils/thesis'
import { doRequest } from '../../../../requests/request'
import ConfirmationButton from '../../../../components/ConfirmationButton/ConfirmationButton'
import {
  useLoadedThesisContext,
  useThesisUpdateAction,
} from '../../../../providers/ThesisProvider/hooks'
import { GLOBAL_CONFIG } from '../../../../config/global'
import { ApiError } from '../../../../requests/handler'
import ThesisStateBadge from '../../../../components/ThesisStateBadge/ThesisStateBadge'
import ThesisVisibilitySelect from '../ThesisVisibilitySelect/ThesisVisibilitySelect'
import { formatThesisType } from '../../../../utils/format'
import LanguageSelect from '../../../../components/LanguageSelect/LanguageSelect'

interface IThesisConfigSectionFormValues {
  title: string
  type: string
  language: string
  visibility: string
  keywords: string[]
  startDate: DateValue | undefined
  endDate: DateValue | undefined
  students: string[]
  advisors: string[]
  supervisors: string[]
  states: Array<{ state: ThesisState; changedAt: DateValue }>
}

const thesisDatesValidator = (
  _value: DateValue | undefined,
  values: IThesisConfigSectionFormValues,
): string | undefined => {
  const startDate = values.startDate
  const endDate = values.endDate

  if (!startDate && !endDate) {
    return undefined
  }

  if (!startDate || !endDate) {
    return 'Both start and end date must be set'
  }

  if (startDate.getTime() > endDate.getTime()) {
    return 'Start date must be before end date'
  }
}

const ThesisConfigSection = () => {
  const { thesis, access } = useLoadedThesisContext()

  const form = useForm<IThesisConfigSectionFormValues>({
    mode: 'controlled',
    initialValues: {
      title: thesis.title,
      type: thesis.type,
      language: thesis.language,
      visibility: thesis.visibility,
      keywords: thesis.keywords,
      startDate: thesis.startDate ? new Date(thesis.startDate) : undefined,
      endDate: thesis.endDate ? new Date(thesis.endDate) : undefined,
      students: thesis.students.map((student) => student.userId),
      advisors: thesis.advisors.map((advisor) => advisor.userId),
      supervisors: thesis.supervisors.map((supervisor) => supervisor.userId),
      states: thesis.states.map((state) => ({
        state: state.state,
        changedAt: new Date(state.startedAt),
      })),
    },
    validateInputOnBlur: true,
    validate: {
      title: isNotEmpty('Title must not be empty'),
      type: isNotEmpty('Type must not be empty'),
      language: isNotEmpty('Language must not be empty'),
      visibility: isNotEmpty('Visibility must not be empty'),
      keywords: (value) => {
        if (value && value.length > 2) {
          return 'You cannot add more than 2 keywords'
        }
      },
      students: isNotEmptyUserList('student'),
      advisors: isNotEmptyUserList('advisor'),
      supervisors: isNotEmptyUserList('supervisor'),
      startDate: thesisDatesValidator,
      endDate: thesisDatesValidator,
      states: (value) => {
        let lastTimestamp = 0

        for (const state of value) {
          if (!state.changedAt) {
            return 'State must have a changed date'
          }

          if (state.changedAt.getTime() <= lastTimestamp) {
            return 'States must be in chronological order'
          }

          lastTimestamp = state.changedAt.getTime()
        }
      },
    },
  })

  useEffect(() => {
    form.validate()
  }, [form.values.startDate, form.values.endDate, form.values.states])

  useEffect(() => {
    form.setInitialValues({
      title: thesis.title,
      type: thesis.type,
      language: thesis.language,
      visibility: thesis.visibility,
      keywords: thesis.keywords,
      startDate: thesis.startDate ? new Date(thesis.startDate) : undefined,
      endDate: thesis.endDate ? new Date(thesis.endDate) : undefined,
      students: thesis.students.map((student) => student.userId),
      advisors: thesis.advisors.map((advisor) => advisor.userId),
      supervisors: thesis.supervisors.map((supervisor) => supervisor.userId),
      states: thesis.states.map((state) => ({
        state: state.state,
        changedAt: new Date(state.startedAt),
      })),
    })

    form.reset()
  }, [thesis])

  const [closing, onClose] = useThesisUpdateAction(async () => {
    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}`, {
      method: 'DELETE',
      requiresAuth: true,
    })

    if (response.ok) {
      return response.data
    } else {
      throw new Error(`Failed to close thesis ${response.status}`)
    }
  }, 'Thesis closed successfully')

  const [updating, onSave] = useThesisUpdateAction(async () => {
    const values = form.values

    const response = await doRequest<IThesis>(`/v2/theses/${thesis.thesisId}`, {
      method: 'PUT',
      requiresAuth: true,
      data: {
        thesisTitle: values.title,
        thesisType: values.type,
        language: values.language,
        visibility: values.visibility,
        keywords: values.keywords,
        startDate: values.startDate,
        endDate: values.endDate,
        studentIds: values.students,
        advisorIds: values.advisors,
        supervisorIds: values.supervisors,
        states: values.states.map((state) => ({
          state: state.state,
          changedAt: state.changedAt,
        })),
      },
    })

    if (response.ok) {
      return response.data
    } else {
      throw new ApiError(response)
    }
  }, 'Thesis updated successfully')

  return (
    <Accordion variant='separated' defaultValue=''>
      <Accordion.Item value='open'>
        <Accordion.Control>Configuration</Accordion.Control>
        <Accordion.Panel>
          <form onSubmit={form.onSubmit(() => void onSave())}>
            <Stack gap='md'>
              <TextInput
                label='Thesis Title'
                required={true}
                disabled={!access.advisor}
                {...form.getInputProps('title')}
              />
              <Select
                label='Thesis Type'
                required={true}
                disabled={!access.advisor}
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
              <ThesisVisibilitySelect
                label='Visibility'
                required={true}
                disabled={!access.advisor}
                {...form.getInputProps('visibility')}
              />
              <TagsInput
                label='Keywords'
                disabled={!access.advisor}
                data={form.values.keywords}
                {...form.getInputProps('keywords')}
              />
              <Group grow>
                <DateInput
                  label='Start Date'
                  disabled={!access.advisor}
                  {...form.getInputProps('startDate')}
                />
                <DateInput
                  label='End Date'
                  disabled={!access.advisor}
                  {...form.getInputProps('endDate')}
                />
              </Group>
              <UserMultiSelect
                required={true}
                disabled={!access.advisor}
                label='Student(s)'
                groups={[]}
                initialUsers={thesis.students}
                {...form.getInputProps('students')}
              />
              <UserMultiSelect
                required={true}
                disabled={!access.advisor}
                label='Advisor(s)'
                groups={['advisor', 'supervisor']}
                initialUsers={thesis.advisors}
                {...form.getInputProps('advisors')}
              />
              <UserMultiSelect
                required={true}
                disabled={!access.advisor}
                label='Supervisor'
                groups={['supervisor']}
                initialUsers={thesis.supervisors}
                maxValues={1}
                {...form.getInputProps('supervisors')}
              />
              {form.values.states.map((item, index) => (
                <Group key={item.state} grow>
                  <Group justify='center'>
                    <Text ta='center' fw='bold'>
                      State changed to
                    </Text>
                    <ThesisStateBadge state={item.state} />
                    <Text ta='center' fw='bold'>
                      at
                    </Text>
                  </Group>
                  <DateTimePicker
                    required={true}
                    disabled={!access.advisor}
                    value={item.changedAt}
                    error={form.errors.states}
                    onChange={(value) => {
                      form.values.states[index] = { state: item.state, changedAt: value }
                      form.setFieldValue('states', [...form.values.states])
                    }}
                  />
                </Group>
              ))}
              {access.advisor && (
                <Group>
                  {!isThesisClosed(thesis) && (
                    <ConfirmationButton
                      confirmationText='Are you sure you want to close the thesis? This will set the thesis state to DROPPED OUT and cannot be undone.'
                      confirmationTitle='Close Thesis'
                      variant='outline'
                      color='red'
                      loading={closing}
                      onClick={onClose}
                    >
                      Close Thesis
                    </ConfirmationButton>
                  )}
                  <Button type='submit' ml='auto' loading={updating} disabled={!form.isValid()}>
                    Update
                  </Button>
                </Group>
              )}
            </Stack>
          </form>
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  )
}

export default ThesisConfigSection
