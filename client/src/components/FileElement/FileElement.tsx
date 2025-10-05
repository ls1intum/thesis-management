import { Group, Paper, Stack, Text, useMantineColorScheme } from '@mantine/core'
import { FileAudioIcon, FileIcon, FileImageIcon, FilePdfIcon } from '@phosphor-icons/react'
import { FileVideoIcon } from '@phosphor-icons/react/dist/ssr'

interface FileElementProps {
  file: File
  rightSide?: React.ReactNode
  fullWidth?: boolean
  iconSize?: number
}

const FileElement = ({ file, rightSide, fullWidth, iconSize }: FileElementProps) => {
  function getFileIcon(fileType: string, size: number = 32) {
    fileType = fileType.toLowerCase()

    if (!fileType) {
      return <FileIcon size={size} />
    }

    if (fileType.startsWith('image/')) {
      return <FileImageIcon size={size} />
    }

    if (fileType.startsWith('audio/')) {
      return <FileAudioIcon size={size} />
    }

    if (fileType.startsWith('video/')) {
      return <FileVideoIcon size={size} />
    }

    if (fileType === 'application/pdf') {
      return <FilePdfIcon size={size} />
    }

    return <FileIcon size={size} />
  }

  const colorScheme = useMantineColorScheme()

  return (
    <Paper
      bg={colorScheme.colorScheme === 'dark' ? 'dark.7' : 'gray.0'}
      p='xs'
      radius='md'
      w={fullWidth ? '100%' : 'fit-content'}
    >
      <Group justify='space-between' align='center' gap='xs' wrap='nowrap'>
        <Group align='center' gap='xs' wrap='nowrap'>
          {getFileIcon(file.type, iconSize)}
          <Stack gap={0}>
            <Text fw={500}>{file.name}</Text>
            <Text size='xs' variant='dimmed'>
              {(file.size / (1024 * 1024)).toFixed(2)} MB
            </Text>
          </Stack>
        </Group>
        {rightSide}
      </Group>
    </Paper>
  )
}
export default FileElement
