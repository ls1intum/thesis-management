import { Box, Button, Group, Modal, Paper, Stack, Text, Title } from '@mantine/core'
import { useEffect, useMemo, useState } from 'react'
import DOMPurify from 'dompurify'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { doRequest } from '../../../../requests/request'
import { IEmailTemplate, IMailVariableDto } from '../../../../requests/responses/emailtemplate'
import { showSimpleError } from '../../../../utils/notification'

interface IEmailTemplatePreviewModalProps {
  opened: boolean
  onClose: () => void
  template?: IEmailTemplate | null
  onEdit: () => void
}

const EmailTemplatePreviewModal = ({
  opened,
  onClose,
  template,
  onEdit,
}: IEmailTemplatePreviewModalProps) => {
  const [templateVariables, setTemplateVariables] = useState<IMailVariableDto[]>([])

  useEffect(() => {
    if (!opened || !template?.id) return

    doRequest<IMailVariableDto[]>(
      `/v2/email-templates/${template.id}/variables`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setTemplateVariables(res.data)
        } else {
          setTemplateVariables([])
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [opened, template?.id])

  const previewHtml = useMemo(() => {
    let html = template?.bodyHtml ?? ''
    for (const variable of templateVariables) {
      html = html.replaceAll(variable.templateVariable, variable.example)
    }
    return html
  }, [template?.bodyHtml, templateVariables])

  return (
    <Modal opened={opened} onClose={onClose} size='xl' centered title='Email Template Preview'>
      <Stack>
        <Stack gap={2}>
          <Title order={4}>{template?.subject ?? 'Untitled template'}</Title>
          <Text size='sm' c='dimmed'>
            {template?.description ?? ''}
          </Text>
        </Stack>

        <Paper withBorder radius='sm'>
          <Box p='md'>
            <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(previewHtml) }} />
          </Box>
        </Paper>

        <Group justify='flex-end'>
          <Button variant='default' onClick={onClose}>
            Close
          </Button>
          <Button onClick={onEdit}>Edit template</Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export default EmailTemplatePreviewModal
