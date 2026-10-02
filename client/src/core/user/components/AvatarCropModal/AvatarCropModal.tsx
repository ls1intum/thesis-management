import { useRef, useState } from 'react'
import AvatarEditor, { type AvatarEditorRef } from 'react-avatar-editor'
import { Button, Center, Modal, Slider, Stack } from '@mantine/core'
import { showSimpleError } from '@/core/utils/notification'

interface IAvatarCropModalProps {
  file: File | undefined
  onClose: () => unknown
  onSave: (file: File) => unknown
}

const AvatarCropModal = (props: IAvatarCropModalProps) => {
  const { file, onClose, onSave } = props

  const editorRef = useRef<AvatarEditorRef | null>(null)
  const [scale, setScale] = useState(1)

  const save = async () => {
    const canvas = editorRef.current?.getImageScaledToCanvas().toDataURL()

    if (!canvas) {
      return
    }

    const data = await fetch(canvas).then((res) => res.blob())

    // Browsers return an empty image for pictures that are too large for a canvas. Saving that would replace the
    // current picture with nothing, so ask for another picture instead.
    if (data.size === 0) {
      showSimpleError('This image could not be processed. Please choose a smaller image.')
      return
    }

    onSave(new File([data], 'avatar.png'))
    setScale(1)
  }

  return (
    <Modal
      opened={Boolean(file)}
      onClose={() => {
        setScale(1)
        onClose()
      }}
    >
      {file && (
        <Stack>
          <Center>
            <AvatarEditor
              ref={editorRef}
              image={file}
              width={300}
              height={300}
              border={20}
              scale={scale}
              color={[255, 255, 255, 0.6]}
              rotate={0}
            />
          </Center>
          <Slider value={scale} onChange={(x) => setScale(x)} min={1} max={3} step={0.1} />
          <Button
            onClick={() => {
              void save()
            }}
            fullWidth
          >
            Save Avatar
          </Button>
        </Stack>
      )}
    </Modal>
  )
}

export default AvatarCropModal
