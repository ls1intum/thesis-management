import type { UploadFileType } from '../../config/types'
import type { PropsWithChildren } from 'react'
import { useEffect, useState } from 'react'
import { Button, Modal, Stack, type ButtonProps } from '@mantine/core'
import UploadArea from '../UploadArea/UploadArea'

interface IUploadFileButtonProps extends ButtonProps {
  onUpload: (file: File) => unknown
  maxSize: number
  accept: UploadFileType
}

export const UploadFileButton = (props: PropsWithChildren<IUploadFileButtonProps>) => {
  const { onUpload, maxSize, accept, children, ...buttonProps } = props

  const [file, setFile] = useState<File>()
  const [modal, setModal] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setFile(undefined)
    setLoading(false)
  }, [modal])

  const handleUpload = async () => {
    if (!file) return
    setLoading(true)
    try {
      await onUpload(file)
      setModal(false)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Button onClick={() => setModal(true)} {...buttonProps}>
      <Modal
        title='File Upload'
        opened={modal}
        onClose={() => setModal(false)}
        onClick={(e) => e.stopPropagation()}
      >
        <Stack>
          <UploadArea value={file} onChange={setFile} maxSize={maxSize} accept={accept} />
          <Button fullWidth disabled={!file} loading={loading} onClick={() => void handleUpload()}>
            Upload File
          </Button>
        </Stack>
      </Modal>
      {children}
    </Button>
  )
}
