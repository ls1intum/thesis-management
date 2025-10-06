import { Button, Card, Group, Stack, Textarea, Text, Tooltip } from '@mantine/core'
import { useState } from 'react'
import { useThesisCommentsContext } from '../../providers/ThesisCommentsProvider/hooks'
import { PaperclipIcon, XIcon } from '@phosphor-icons/react'
import UploadFileButton from '../UploadFileButton/UploadFileButton'
import { isThesisClosed } from '../../utils/thesis'
import FileElement from '../FileElement/FileElement'

const ThesisCommentsForm = () => {
  const { postComment, posting, thesis } = useThesisCommentsContext()

  const [message, setMessage] = useState('')
  const [file, setFile] = useState<File>()

  if (isThesisClosed(thesis)) {
    return null
  }

  return (
    <Card withBorder radius={'md'} p={'sm'}>
      <Stack p={0} gap={'1rem'}>
        <Stack gap={'0.5rem'}>
          <Textarea
            placeholder='Add a comment or file...'
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            autosize
            minRows={1}
            maxRows={15}
            variant='unstyled'
            mx={5}
            size='md'
          />

          {file && (
            <FileElement
              file={file}
              rightSide={
                <Button
                  variant='subtle'
                  color='gray'
                  size='xs'
                  onClick={() => setFile(undefined)}
                  p={5}
                >
                  <XIcon size={16} />
                </Button>
              }
            />
          )}
        </Stack>

        <Group p={0} justify='space-between' align='center'>
          <UploadFileButton
            onUpload={setFile}
            maxSize={25 * 1024 * 1024}
            accept='any'
            size='xs'
            variant='subtle'
            color='gray'
            p={5}
            disabled={!!file}
          >
            <Tooltip
              label='Only one file per comment'
              disabled={!file}
              withArrow
              position='top'
              openDelay={500}
            >
              <Group justify='center' align='center' gap={5}>
                <PaperclipIcon size={18} />
                <Text size='sm' variant='dimmed'>
                  Attach File
                </Text>
              </Group>
            </Tooltip>
          </UploadFileButton>

          <Button
            disabled={!message && !file}
            loading={posting}
            onClick={() => {
              postComment(message, file)

              setMessage('')
              setFile(undefined)
            }}
            size='xs'
          >
            Post Comment
          </Button>
        </Group>
      </Stack>
    </Card>
  )
}

export default ThesisCommentsForm
