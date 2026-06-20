import type { ReactNode } from 'react'
import { useEffect, useState } from 'react'
import { Button, Center, Group, Stack, type BoxProps } from '@mantine/core'
import { downloadFile } from '@/core/utils/blob'
import FilePreview from '@/core/components/FilePreview/FilePreview'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import { getApiResponseErrorMessage } from '@/core/requests/handler'
import type { UploadFileType } from '@/core/config/types'
import { getAdjustedFileType } from '@/core/utils/file'

interface IAuthenticatedFilePreviewProps extends BoxProps {
  url: string
  filename: string
  type: UploadFileType
  actionButton?: ReactNode
  aspectRatio?: number
  allowDownload?: boolean
  requiresAuth?: boolean
}

export const AuthenticatedFilePreview = (props: IAuthenticatedFilePreviewProps) => {
  const {
    url,
    filename,
    type,
    actionButton,
    aspectRatio = 16 / 9,
    allowDownload = true,
    requiresAuth = true,
  } = props

  const [file, setFile] = useState<File>()

  useEffect(() => {
    setFile(undefined)

    if (url) {
      return doRequest<Blob>(
        url,
        {
          method: 'GET',
          requiresAuth: requiresAuth,
          responseType: 'blob',
        },
        (response) => {
          if (response.ok) {
            const mimeType: Record<UploadFileType, string> = {
              pdf: 'application/pdf',
              // image does not need to be a png but it will work in browser anyway.
              image: 'image/png',
              any: 'application/octet-stream',
            }

            setFile(
              new File([response.data], filename, {
                type: mimeType[getAdjustedFileType(filename, type)],
              }),
            )
          } else {
            showSimpleError(getApiResponseErrorMessage(response))
          }
        },
      )
    }
  }, [url, filename, type, requiresAuth])

  return (
    <Stack>
      {file && <FilePreview aspectRatio={aspectRatio} file={file} type={type} />}
      {(allowDownload || actionButton) && (
        <Center>
          <Group>
            {actionButton}
            {allowDownload && (
              <Button variant='outline' onClick={() => file && downloadFile(file)}>
                Download
              </Button>
            )}
          </Group>
        </Center>
      )}
    </Stack>
  )
}
