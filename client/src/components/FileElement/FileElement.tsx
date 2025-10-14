import {
  Group,
  MantineColor,
  MantineFontSize,
  Paper,
  Stack,
  Text,
  useMantineColorScheme,
} from '@mantine/core'
import { FileAudioIcon, FileIcon, FileImageIcon, FilePdfIcon } from '@phosphor-icons/react'
import { FileVideoIcon } from '@phosphor-icons/react/dist/ssr'

interface FileElementProps {
  file: File
  rightSide?: React.ReactNode
  fullWidth?: boolean
  iconSize?: number
  sizeFileName?: MantineFontSize
  withFileSize?: boolean
  bg?: MantineColor
  textColor?: MantineColor
}

const FileElement = ({
  file,
  rightSide,
  fullWidth,
  iconSize,
  sizeFileName = 'md',
  withFileSize = true,
  bg,
  textColor = 'gray',
}: FileElementProps) => {
  function getFileIcon(fileType: string, size: number = 32) {
    fileType = fileType.toLowerCase()

    if (!fileType) {
      return <FileIcon size={size} color={textColor || undefined} />
    }

    if (fileType.startsWith('image/')) {
      return <FileImageIcon size={size} color={textColor || undefined} />
    }

    if (fileType.startsWith('audio/')) {
      return <FileAudioIcon size={size} />
    }

    if (fileType.startsWith('video/')) {
      return <FileVideoIcon size={size} color={textColor || undefined} />
    }

    if (fileType === 'application/pdf') {
      return <FilePdfIcon size={size} color={textColor || undefined} />
    }

    return <FileIcon size={size} />
  }

  const colorScheme = useMantineColorScheme()

  return (
    <Paper
      bg={bg || (colorScheme.colorScheme === 'dark' ? 'dark.6' : 'gray.1')}
      p='xs'
      radius='md'
      w={fullWidth ? '100%' : 'fit-content'}
    >
      <Group justify='space-between' align='center' gap='xs' wrap='nowrap'>
        <Group align='center' gap='xs' wrap='nowrap'>
          {getFileIcon(file.type, iconSize)}
          <Stack gap={0}>
            <Text fw={500} size={sizeFileName} c={textColor || undefined} lineClamp={1}>
              {file.name}
            </Text>
            {withFileSize && (
              <Text size='xs' variant='dimmed'>
                {(file.size / (1024 * 1024)).toFixed(2)} MB
              </Text>
            )}
          </Stack>
        </Group>
        {rightSide}
      </Group>
    </Paper>
  )
}
export default FileElement
