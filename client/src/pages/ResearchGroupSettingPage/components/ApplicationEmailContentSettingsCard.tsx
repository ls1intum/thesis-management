import { ActionIcon, Group, Stack, Switch, Text, Tooltip } from '@mantine/core'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { doRequest } from '../../../requests/request'
import { useParams } from 'react-router'
import { showSimpleError } from '../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../requests/handler'
import { IResearchGroupSettings } from '../../../requests/responses/researchGroupSettings'
import { Info } from '@phosphor-icons/react'

interface ApplicationEmailContentSettingsCardProps {
  includeApplicationDataInEmail: boolean
  setIncludeApplicationDataInEmail: (value: boolean) => void
}

const ApplicationEmailContentSettingsCard = ({
  includeApplicationDataInEmail,
  setIncludeApplicationDataInEmail,
}: ApplicationEmailContentSettingsCardProps) => {
  const { researchGroupId } = useParams<{ researchGroupId: string }>()

  const handleChange = (value: boolean, previousValue: boolean) => {
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
          setIncludeApplicationDataInEmail(
            res.data.applicationEmailSettings.includeApplicationDataInEmail,
          )
        } else {
          setIncludeApplicationDataInEmail(previousValue)
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
            <Group gap={6} align='center'>
              <Text size='sm' fw={500}>
                Include Personal Details and Attachments
              </Text>
              <Tooltip
                label={
                  'When disabled, the custom email template for new application notifications ' +
                  'is not used. Instead, a minimal email with only the student name, thesis ' +
                  'topic, and a link is sent.'
                }
                multiline
                w={300}
                events={{ hover: true, focus: true, touch: false }}
              >
                <ActionIcon
                  variant='subtle'
                  color='gray'
                  size='sm'
                  aria-label='Information about email template behavior'
                >
                  <Info size={16} />
                </ActionIcon>
              </Tooltip>
            </Group>
            <Text size='xs' c='dimmed'>
              When enabled, application notification emails will include the applicant&apos;s
              personal details (motivation, skills, interests, study info) and file attachments (CV,
              examination report, degree report). When disabled, supervisors and examiners receive a
              minimal notification with only the student name, thesis topic, and a link to the
              application.
            </Text>
          </Stack>
          <Switch
            checked={includeApplicationDataInEmail}
            onChange={(event) => {
              const previousValue = includeApplicationDataInEmail
              setIncludeApplicationDataInEmail(event.currentTarget.checked)
              handleChange(event.currentTarget.checked, previousValue)
            }}
          />
        </Group>
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default ApplicationEmailContentSettingsCard
