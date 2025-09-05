import { Badge, Card, Text, Stack, Title, Group, Button, Flex, Modal } from '@mantine/core'
import { IEmailTemplate } from '../../../../requests/responses/emailtemplate'
import { Eye, NotePencil } from 'phosphor-react'
import { useState } from 'react'
import EmailTemplateModal from './EmailTemplateModal'

interface IEmailTemplatesOverviewProps {
  emailTemplate: {
    default: IEmailTemplate | null
    researchGroupTemplate: IEmailTemplate | null
  }
}

const EmailTemplateCard = ({ emailTemplate }: IEmailTemplatesOverviewProps) => {
  const [templateModalOpened, setTemplateModalOpened] = useState(false)

  //Create modal states here so they don't get reset if you close accidentally
  const [mode, setMode] = useState<'preview' | 'edit'>(`edit`)
  const [editingTemplate, setEditingTemplate] = useState<IEmailTemplate | null>(
    emailTemplate.researchGroupTemplate ?? emailTemplate.default ?? null,
  )

  return (
    <>
      <Card withBorder shadow={'xs'} radius='md' h='100%' w='100%'>
        <Stack justify='space-between' h={'100%'} gap='md'>
          <Stack gap={'0.5rem'}>
            <Group justify='space-between' align='top' wrap='nowrap' gap={'md'}>
              <Title order={5}>
                {emailTemplate.default ? emailTemplate.default.subject : 'Custome Template'}
              </Title>
              <Badge
                color={emailTemplate.researchGroupTemplate ? 'green' : 'gray'}
                variant='light'
                style={{ flexShrink: 0 }}
                size='sm'
              >
                {emailTemplate.researchGroupTemplate ? 'Customized' : 'Default'}
              </Badge>
            </Group>
            <Text size='sm' c='dimmed' lineClamp={3}>
              {emailTemplate.default ? emailTemplate.default.description : 'No content available'}
            </Text>
          </Stack>

          <Flex align='center' wrap='nowrap' gap={'xs'}>
            <Button
              fullWidth
              variant='default'
              size='xs'
              onClick={() => {
                setTemplateModalOpened(true)
                setMode('preview')
              }}
            >
              <Group align='center' wrap='nowrap' gap={'xs'}>
                <Eye size={16} />
                <Text size='sm'>Preview</Text>
              </Group>
            </Button>
            <Button
              fullWidth
              size='xs'
              onClick={() => {
                setTemplateModalOpened(true)
                setMode('edit')
              }}
            >
              <Group align='center' wrap='nowrap' gap={'xs'}>
                <NotePencil size={16} />
                <Text size='sm'>Edit</Text>
              </Group>
            </Button>
          </Flex>
        </Stack>
      </Card>

      <EmailTemplateModal
        opened={templateModalOpened}
        onClose={() => setTemplateModalOpened(false)}
        mode={mode}
      />
    </>
  )
}

export default EmailTemplateCard
