import { isEmail, isNotEmpty, useForm } from '@mantine/form'
import { IUpdateUserInformationPayload } from '../../requests/payloads/user'
import {
  Anchor,
  Button,
  Checkbox,
  Group,
  NumberInput,
  Select,
  Stack,
  TextInput,
} from '@mantine/core'
import { useAuthenticationContext, useLoggedInUser } from '../../hooks/authentication'
import { GLOBAL_CONFIG } from '../../config/global'
import { AVAILABLE_COUNTRIES } from '../../config/countries'
import UploadArea from '../UploadArea/UploadArea'
import { useEffect, useState } from 'react'
import DocumentEditor from '../DocumentEditor/DocumentEditor'
import { useApiPdfFile } from '../../hooks/fetcher'
import { showSimpleError } from '../../utils/notification'
import { getHtmlTextLength } from '../../utils/validation'
import { enrollmentDateToSemester, semesterToEnrollmentDate } from '../../utils/converter'
import { Link } from 'react-router'
import AvatarInput from './components/AvatarInput/AvatarInput'

interface IUserInformationFormProps {
  requireCompletion: boolean
  includeAvatar?: boolean
  onComplete?: () => unknown
}

const UserInformationForm = (props: IUserInformationFormProps) => {
  const { requireCompletion, includeAvatar, onComplete } = props

  const { updateInformation } = useAuthenticationContext()
  const user = useLoggedInUser()

  const form = useForm<
    Omit<IUpdateUserInformationPayload, 'enrolledAt'> & {
      semester: string
      customData: Record<string, string>
      declarationOfConsentAccepted: boolean
      avatar: File | undefined
      examinationReport: File | undefined
      cv: File | undefined
      degreeReport: File | undefined
    }
  >({
    mode: 'controlled',
    initialValues: {
      matriculationNumber: '',
      email: '',
      firstName: '',
      lastName: '',
      gender: '',
      nationality: '',
      studyDegree: '',
      studyProgram: '',
      semester: '',
      specialSkills: '',
      projects: '',
      interests: '',
      declarationOfConsentAccepted: localStorage.getItem('declarationOfConsentAccepted') === 'true',
      avatar: undefined,
      examinationReport: undefined,
      cv: undefined,
      degreeReport: undefined,
      customData: Object.fromEntries(
        Object.keys(GLOBAL_CONFIG.custom_data).map((key) => [key, '']),
      ),
    },
    validateInputOnBlur: true,
    validate: {
      matriculationNumber: requireCompletion
        ? isNotEmpty('Please state your matriculation number')
        : undefined,
      firstName: requireCompletion ? isNotEmpty('Please state your first name') : undefined,
      lastName: requireCompletion ? isNotEmpty('Please state your last name') : undefined,
      email: requireCompletion ? isEmail('Invalid email') : undefined,
      gender: requireCompletion ? isNotEmpty('Please state your gender') : undefined,
      nationality: requireCompletion ? isNotEmpty('Please state your nationality') : undefined,
      studyDegree: requireCompletion ? isNotEmpty('Please state your study degree') : undefined,
      studyProgram: requireCompletion ? isNotEmpty('Please state your study program') : undefined,
      semester: requireCompletion ? isNotEmpty('Please state your semester date') : undefined,
      specialSkills: (value) => {
        if (!value && requireCompletion) {
          return 'Please state your special skills.'
        } else if (getHtmlTextLength(value) > 1000) {
          return 'The maximum allowed number of characters is 1000'
        }
      },
      interests: (value) => {
        if (!value && requireCompletion) {
          return 'Please state your interests.'
        } else if (getHtmlTextLength(value) > 1000) {
          return 'The maximum allowed number of characters is 1000'
        }
      },
      projects: (value) => {
        if (!value && requireCompletion) {
          return 'Please state your projects.'
        } else if (getHtmlTextLength(value) > 1000) {
          return 'The maximum allowed number of characters is 1000'
        }
      },
      declarationOfConsentAccepted: (value) => !value,
      examinationReport: (value) => {
        if (!value && requireCompletion) {
          return 'Please upload your examination report'
        } else if (value && value.size > 2 * 1024 ** 3) {
          return 'The examination report should not exceed 2mb'
        }
      },
      cv: (value) => {
        if (!value && requireCompletion) {
          return 'Please upload your CV.'
        } else if (value && value.size > 2 * 1024 ** 3) {
          return 'The CV should not exceed 2mb'
        }
      },
      degreeReport: (value) => {
        if (value && value.size > 2 * 1024 ** 3) {
          return 'The bachelor report should not exceed 2mb'
        }
      },
      ...Object.fromEntries(
        Object.entries(GLOBAL_CONFIG.custom_data)
          .filter(([, value]) => value.required)
          .map(([key, value]) => [
            `customData.${key}`,
            requireCompletion ? isNotEmpty(`Please state your ${value.label}`) : undefined,
          ]),
      ),
    },
  })

  const [loading, setLoading] = useState(false)

  useEffect(() => {
    form.setValues({
      email: user?.email || '',
      matriculationNumber: user?.matriculationNumber || '',
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      gender: user?.gender || '',
      nationality: user?.nationality || '',
      studyDegree: user?.studyDegree || '',
      studyProgram: user?.studyProgram || '',
      semester: user?.enrolledAt ? enrollmentDateToSemester(user.enrolledAt).toString() : '',
      specialSkills: user?.specialSkills || '',
      interests: user?.interests || '',
      projects: user?.projects || '',
      customData: Object.fromEntries(
        Object.keys(GLOBAL_CONFIG.custom_data).map((key) => [key, user.customData?.[key] || '']),
      ),
    })
  }, [user])

  useApiPdfFile(
    user.hasCv ? `/v2/users/${user.userId}/cv` : undefined,
    `cv-${user.userId}.pdf`,
    (file) => form.setFieldValue('cv', file),
  )
  useApiPdfFile(
    user.hasExaminationReport ? `/v2/users/${user.userId}/examination-report` : undefined,
    `examination-report-${user.userId}.pdf`,
    (file) => form.setFieldValue('examinationReport', file),
  )
  useApiPdfFile(
    user.hasDegreeReport ? `/v2/users/${user.userId}/degree-report` : undefined,
    `degree-report-${user.userId}.pdf`,
    (file) => form.setFieldValue('degreeReport', file),
  )

  return (
    <form
      onSubmit={form.onSubmit(async (values) => {
        setLoading(true)

        localStorage.setItem('declarationOfConsentAccepted', 'true')

        try {
          await updateInformation(
            {
              matriculationNumber: values.matriculationNumber || null,
              firstName: values.firstName || null,
              lastName: values.lastName || null,
              gender: values.gender || null,
              nationality: values.nationality || null,
              email: values.email || null,
              studyDegree: values.studyDegree || null,
              studyProgram: values.studyProgram || null,
              enrolledAt: semesterToEnrollmentDate(values.semester),
              specialSkills: values.specialSkills || null,
              interests: values.interests || null,
              projects: values.projects || null,
              customData: values.customData,
            },
            values.avatar,
            values.examinationReport,
            values.cv,
            values.degreeReport,
          )
            .then(onComplete)
            .catch((e) => {
              if (e instanceof Error) {
                showSimpleError(e.message)
              }
            })
        } finally {
          setLoading(false)
        }
      })}
    >
      <Stack gap='md'>
        {includeAvatar && (
          <AvatarInput
            label='Avatar'
            required={requireCompletion}
            value={form.values.avatar}
            onChange={(file) => form.setFieldValue('avatar', file)}
          />
        )}
        <Group grow align='flex-start'>
          <TextInput
            type='email'
            required={requireCompletion}
            placeholder='your@email.com'
            label='Email'
            {...form.getInputProps('email')}
          />
          <TextInput
            type='text'
            required={requireCompletion}
            placeholder='Matriculation Number'
            label='Matriculation Number'
            {...form.getInputProps('matriculationNumber')}
          />
        </Group>
        <Group grow align='flex-start'>
          <TextInput
            type='text'
            required={requireCompletion}
            placeholder='First Name'
            label='First Name'
            {...form.getInputProps('firstName')}
          />
          <TextInput
            type='text'
            required={requireCompletion}
            placeholder='Last Name'
            label='Last Name'
            {...form.getInputProps('lastName')}
          />
        </Group>
        <Group grow align='flex-start'>
          <Select
            label='Gender'
            placeholder='Gender'
            data={Object.keys(GLOBAL_CONFIG.genders).map((key) => {
              return {
                label: GLOBAL_CONFIG.genders[key],
                value: key,
              }
            })}
            required={requireCompletion}
            searchable={true}
            {...form.getInputProps('gender')}
          />
          <Select
            label='Nationality'
            placeholder='Nationality'
            data={Object.entries(AVAILABLE_COUNTRIES).map(([key, value]) => {
              return {
                label: value,
                value: key,
              }
            })}
            required={requireCompletion}
            searchable={true}
            {...form.getInputProps('nationality')}
          />
        </Group>
        <Group grow align='flex-start'>
          <Select
            label='Current Study Degree'
            placeholder='Study Degree'
            data={Object.keys(GLOBAL_CONFIG.study_degrees).map((key) => {
              return {
                label: GLOBAL_CONFIG.study_degrees[key],
                value: key,
              }
            })}
            required={requireCompletion}
            searchable={true}
            {...form.getInputProps('studyDegree')}
          />
          <Select
            label='Study Program'
            placeholder='Study Program'
            data={Object.keys(GLOBAL_CONFIG.study_programs).map((key) => {
              return {
                label: GLOBAL_CONFIG.study_programs[key],
                value: key,
              }
            })}
            required={requireCompletion}
            searchable={true}
            {...form.getInputProps('studyProgram')}
          />
          <NumberInput
            required={requireCompletion}
            label='Semester in Current Study Program'
            {...form.getInputProps('semester')}
          />
        </Group>
        {Object.entries(GLOBAL_CONFIG.custom_data).map(([key, value]) => (
          <TextInput
            key={key}
            label={value.label}
            required={value.required && requireCompletion}
            {...form.getInputProps(`customData.${key}`)}
          />
        ))}
        <DocumentEditor
          label='Interests (What are you interested in?)'
          maxLength={1000}
          required={requireCompletion}
          editMode={true}
          {...form.getInputProps('interests')}
        />
        <DocumentEditor
          label='Projects (What projects have you worked on?)'
          maxLength={1000}
          required={requireCompletion}
          editMode={true}
          {...form.getInputProps('projects')}
        />
        <DocumentEditor
          label='Special Skills (Programming languages, certificates, etc.)'
          maxLength={1000}
          required={requireCompletion}
          editMode={true}
          {...form.getInputProps('specialSkills')}
        />
        <UploadArea
          label='Examination Report'
          required={requireCompletion}
          value={form.values.examinationReport}
          onChange={(file) => form.setFieldValue('examinationReport', file)}
          maxSize={2 * 1024 * 1024}
          accept='pdf'
        />
        <UploadArea
          label='CV'
          required={requireCompletion}
          value={form.values.cv}
          onChange={(file) => form.setFieldValue('cv', file)}
          maxSize={2 * 1024 * 1024}
          accept='pdf'
        />
        {form.values.studyDegree !== 'BACHELOR' && (
          <UploadArea
            label='Bachelor Report'
            required={requireCompletion}
            value={form.values.degreeReport}
            onChange={(file) => form.setFieldValue('degreeReport', file)}
            maxSize={2 * 1024 * 1024}
            accept='pdf'
          />
        )}
        <Checkbox
          mt='md'
          label={
            <>
              I have read the{' '}
              <Anchor component={Link} to='/privacy'>
                privacy notice
              </Anchor>{' '}
              and agree to the processing of my data.
            </>
          }
          {...form.getInputProps('declarationOfConsentAccepted', { type: 'checkbox' })}
        />
        <Group>
          <Button type='submit' ml='auto' disabled={!form.isValid()} loading={loading}>
            Update Information
          </Button>
        </Group>
      </Stack>
    </form>
  )
}

export default UserInformationForm
