import { Divider, Group, NumberInput, Stack, Switch, Text } from '@mantine/core'
import { ResearchGroupSettingsCard } from './ResearchGroupSettingsCard'
import { useState } from 'react'

const AutomaticRejectionCard = () => {
  const [automaticRejectionEnabled, setAutomaticRejectionEnabled] = useState(false)

  return (
    <ResearchGroupSettingsCard
      title='Automatic Rejects'
      subtle='Configure automatic rejection settings for applications.'
    >
      <Stack>
        <Group justify='space-between' align='center' wrap='nowrap'>
          <Stack gap={2}>
            <Text size='sm' fw={500}>
              Enable automatic rejection
            </Text>
            <Text size='xs' c='dimmed'>
              Automatically reject applications after a specified time period.
            </Text>
          </Stack>
          <Switch
            checked={automaticRejectionEnabled}
            onChange={(event) => setAutomaticRejectionEnabled(event.currentTarget.checked)}
          />
        </Group>
        {automaticRejectionEnabled && (
          <Group ml={'1rem'} wrap='nowrap'>
            <Divider orientation='vertical' />
            <Stack gap={2}>
              <Text size='sm' fw={500}>
                Rejection Time Period
              </Text>
              <Text size='xs' c='dimmed'>
                Automatically reject applications after the selected number of weeks. The period
                starts after the application deadline, or if none is set, after the intended start
                date, otherwise from the application creation date. Applicants are never rejected
                earlier than two weeks after applying.
              </Text>
              <NumberInput
                placeholder="Don't enter less than 2 weeks"
                min={2}
                suffix=' weeks'
                defaultValue={8}
                pt={6}
              />
            </Stack>
          </Group>
        )}
      </Stack>
    </ResearchGroupSettingsCard>
  )
}

export default AutomaticRejectionCard
