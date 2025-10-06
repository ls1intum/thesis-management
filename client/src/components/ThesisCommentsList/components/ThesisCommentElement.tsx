import { Divider, Group, Stack, Text } from '@mantine/core'
import { IThesisComment } from '../../../requests/responses/thesis'
import CustomAvatar from '../../CustomAvatar/CustomAvatar'
import { formatDate } from '../../../utils/format'
import FileElement from '../../FileElement/FileElement'
import AuthenticatedFilePreviewButton from '../../AuthenticatedFilePreviewButton/AuthenticatedFilePreviewButton'
import AuthenticatedFileDownloadButton from '../../AuthenticatedFileDownloadButton/AuthenticatedFileDownloadButton'
import { DownloadIcon, EyeIcon } from '@phosphor-icons/react'
import { useUser } from '../../../hooks/authentication'
import { useHover } from '@mantine/hooks'
import DeleteButton from '../../DeleteButton/DeleteButton'

interface IThesisCommentElementProps {
  comment: IThesisComment
  thesisId: string
  deleteComment: (comment: IThesisComment) => void
}

// Utility function to get MIME type from file extension
const getFileTypeFromName = (fileName: string): string => {
  const extension = fileName.split('.').pop()?.toLowerCase()

  const mimeTypes: Record<string, string> = {
    // Documents
    pdf: 'application/pdf',

    // Images
    jpg: 'image/jpeg',
    jpeg: 'image/jpeg',
    png: 'image/png',
    gif: 'image/gif',
    bmp: 'image/bmp',
    svg: 'image/svg+xml',
    webp: 'image/webp',
    tiff: 'image/tiff',
    ico: 'image/x-icon',

    // Audio
    mp3: 'audio/mpeg',
    wav: 'audio/wav',
    ogg: 'audio/ogg',
    m4a: 'audio/mp4',
    aac: 'audio/aac',
    flac: 'audio/flac',

    // Video
    mp4: 'video/mp4',
    avi: 'video/x-msvideo',
    mov: 'video/quicktime',
    wmv: 'video/x-ms-wmv',
    flv: 'video/x-flv',
    webm: 'video/webm',
    mkv: 'video/x-matroska',
  }

  return extension ? mimeTypes[extension] || '' : ''
}

const ThesisCommentElement = ({ comment, thesisId, deleteComment }: IThesisCommentElementProps) => {
  const user = useUser()
  const { hovered, ref } = useHover()

  return (
    <Stack ref={ref} gap={'1rem'}>
      <Group justify='space-between'>
        <Group>
          <CustomAvatar user={comment.createdBy} />
          <div>
            <Text size='sm'> {`${comment.createdBy.firstName} ${comment.createdBy.lastName}`}</Text>
            <Text size='xs' c='dimmed'>
              {formatDate(comment.createdAt, { withTime: true })}
            </Text>
          </div>
        </Group>
        {user?.userId === comment.createdBy.userId && hovered && (
          <DeleteButton onClick={() => deleteComment(comment)} />
        )}
      </Group>
      <Stack pl={54} gap={'0.5rem'}>
        {comment.message && comment.message.trim() !== '' && (
          <Text size='sm'>{comment.message}</Text>
        )}
        {comment.uploadName && (
          <FileElement
            file={
              new File([], comment.uploadName, {
                type: getFileTypeFromName(comment.uploadName),
              })
            }
            fullWidth
            iconSize={24}
            withFileSize={false}
            sizeFileName='sm'
            rightSide={
              <Group gap='xs' ml='auto'>
                <AuthenticatedFilePreviewButton
                  url={`/v2/theses/${thesisId}/comments/${comment.commentId}/file`}
                  filename={comment.uploadName}
                  type='any'
                  size='xs'
                  variant='subtle'
                  color='gray'
                  p={5}
                >
                  <EyeIcon size={16} />
                </AuthenticatedFilePreviewButton>
                <AuthenticatedFileDownloadButton
                  url={`/v2/theses/${thesisId}/comments/${comment.commentId}/file`}
                  filename={comment.uploadName}
                  size='xs'
                  variant='subtle'
                  color='gray'
                  p={5}
                >
                  <DownloadIcon size={16} />
                </AuthenticatedFileDownloadButton>
              </Group>
            }
          />
        )}
      </Stack>
    </Stack>
  )
}

export default ThesisCommentElement
