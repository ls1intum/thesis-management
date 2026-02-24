import { Group, Stack, Switch, Text } from '@mantine/core'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { doRequest } from '../../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { IResearchGroupSettings } from '../../../requests/responses/researchGroupSettings'

interface ApplicationEmailContentSettingsCardProps {
  includeApplicationDataInEmail: boolean
  setIncludeApplicationDataInEmail: (value: boolean) => void
}

const ApplicationEmailContentSettingsCard = ({
  includeApplicationDataInEmail,
  setIncludeApplicationDataInEmail,
}: ApplicationEmailContentSettingsCardProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const handleChange = (value: boolean) => {
    doRequest<IResearchGroupSettings>(
      `/v2/research-group-settings/${researchGroupId}`,
      {
        method: 'POST',
        requiresAuth: true,
        data: {
          applicationEmailSettings: {
            includeApplicationDataInEmail: value,
          },
        },
      },
      (res) => {
        if (res.ok) {
          if (
            res.data.applicationEmailSettings.includeApplicationDataInEmail !==
            includeApplicationDataInEmail
          ) {
            setIncludeApplicationDataInEmail(
              res.data.applicationEmailSettings.includeApplicationDataInEmail,
            )
          }
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }

  return (
    <ResearchGroupSettingsCard
      title='Application Email Content'
      subtle='Configure what information is included in application notification emails sent to supervisors and examiners.'
    >
      <Stack>
        <Group justify='space-between' align='center' wrap='nowrap'>
          <Stack gap={2}>
            <Text size='sm' fw={500}>
              Include Personal Details and Attachments
            </Text>
            <Text size='xs' c='dimmed'>
              When enabled, application notification emails will include the applicant&apos;s
              personal details (motivation, skills, interests, study info) and file attachments (CV,
              examination report, degree report). When disabled, emails only contain the student
              name, thesis topic, and a link to the application in the system.
            </Text>
          </Stack>
          <Switch
            checked={includeApplicationDataInEmail}
            onChange={(event) => {
              setIncludeApplicationDataInEmail(event.currentTarget.checked)
              handleChange(event.currentTarget.checked)
            }}
          />
        </Group>
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default ApplicationEmailContentSettingsCard
