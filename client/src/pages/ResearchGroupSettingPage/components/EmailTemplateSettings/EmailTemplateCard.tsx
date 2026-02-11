import { Badge, Card, Text, Stack, Title, Group, Button, Flex } from '@mantine/core'
import { IEmailTemplate } from '../../../../requests/responses/emailtemplate'
import { useState } from 'react'
import EmailTemplateModal from './EmailTemplateModal'
import { EyeIcon, NotePencilIcon } from '@phosphor-icons/react'

interface IEmailTemplatesOverviewProps {
  emailTemplate: {
    default: IEmailTemplate | null
    researchGroupTemplate: IEmailTemplate | null
  }
  updateTemplate?: (template: IEmailTemplate) => void
}

const EmailTemplateCard = ({ emailTemplate, updateTemplate }: IEmailTemplatesOverviewProps) => {
  const [templateModalOpened, setTemplateModalOpened] = useState(false)

  const [editingTemplate, setEditingTemplate] = useState<IEmailTemplate | null>(
    emailTemplate.researchGroupTemplate ?? emailTemplate.default ?? null,
  )

  return (
    <>
      <Card
        withBorder
        shadow={'xs'}
        radius='md'
        h='100%'
        w='100%'
        p={0}
        bg={emailTemplate.researchGroupTemplate ? 'lime.6' : 'gray.6'}
      >
        <Card radius='md' h='100%' w='100%' ml={5}>
          <Flex
            justify='space-between'
            h={'100%'}
            gap={{ base: 'md', sm: 'lg' }}
            direction={{ base: 'column', sm: 'row' }}
          >
            <Stack gap={'0.5rem'}>
              <Group align='center' wrap='nowrap' gap={'xs'}>
                <Title order={6}>
                  {emailTemplate.default ? emailTemplate.default.subject : 'Custom Template'}
                </Title>
                <Badge
                  color={emailTemplate.researchGroupTemplate ? 'lime.8' : 'gray.8'}
                  variant='light'
                  style={{ flexShrink: 0 }}
                  size='xs'
                >
                  {emailTemplate.researchGroupTemplate ? 'Customized' : 'Default'}
                </Badge>
              </Group>
              <Text size='xs' c='dimmed' lineClamp={3}>
                {emailTemplate.default ? emailTemplate.default.description : 'No content available'}
              </Text>
            </Stack>

            <Flex align='center' wrap='nowrap' gap={'xs'}>
              <Button
                variant='outline'
                size='xs'
                onClick={() => {
                  setTemplateModalOpened(true)
                }}
                fullWidth={false}
                leftSection={<EyeIcon size={16} />}
              >
                Preview
              </Button>
              <Button
                size='xs'
                onClick={() => {
                  setTemplateModalOpened(true)
                }}
                leftSection={<NotePencilIcon size={16} />}
              >
                Edit
              </Button>
            </Flex>
          </Flex>
        </Card>
      </Card>

      <EmailTemplateModal
        opened={templateModalOpened}
        onClose={() => setTemplateModalOpened(false)}
        defaultTemplate={emailTemplate.default ?? undefined}
        researchGroupTemplate={emailTemplate.researchGroupTemplate ?? undefined}
        editingTemplate={editingTemplate}
        setEditingTemplate={setEditingTemplate}
        updateTemplate={updateTemplate}
      />
    </>
  )
}

export default EmailTemplateCard
