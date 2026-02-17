import { Badge, Card, Text, Stack, Title, Group, Button, Flex } from '@mantine/core'
import { IEmailTemplate } from '../../../../requests/responses/emailtemplate'
import { useState } from 'react'
import EmailTemplatePreviewModal from './EmailTemplatePreviewModal'
import { EyeIcon, NotePencilIcon } from '@phosphor-icons/react'
import { useNavigate, useParams } from 'react-router'

interface IEmailTemplateCardProps {
  emailTemplate: {
    default: IEmailTemplate | null
    researchGroupTemplate: IEmailTemplate | null
  }
}

const EmailTemplateCard = ({ emailTemplate }: IEmailTemplateCardProps) => {
  const [templatePreviewOpened, setTemplatePreviewOpened] = useState(false)
  const navigate = useNavigate()
  const { researchGroupId } = useParams<{ researchGroupId: string }>()
  const activeTemplate = emailTemplate.researchGroupTemplate ?? emailTemplate.default ?? null

  const navigateToEditPage = () => {
    if (!researchGroupId || !activeTemplate) return
    navigate(
      `/research-groups/${researchGroupId}/email-templates/${activeTemplate.templateCase}/edit`,
    )
  }

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
                  setTemplatePreviewOpened(true)
                }}
                fullWidth={false}
                leftSection={<EyeIcon size={16} />}
              >
                Preview
              </Button>
              <Button
                size='xs'
                onClick={() => {
                  navigateToEditPage()
                }}
                leftSection={<NotePencilIcon size={16} />}
              >
                Edit
              </Button>
            </Flex>
          </Flex>
        </Card>
      </Card>

      <EmailTemplatePreviewModal
        opened={templatePreviewOpened}
        onClose={() => setTemplatePreviewOpened(false)}
        template={activeTemplate}
        onEdit={navigateToEditPage}
      />
    </>
  )
}

export default EmailTemplateCard
