import { getAvatar } from '../../../../utils/user'
import AvatarEditor from 'react-avatar-editor'
import { useAuthenticationContext, useLoggedInUser } from '../../../../hooks/authentication'
import {
  Avatar,
  Button,
  Center,
  Group,
  Input,
  Modal,
  Slider,
  Stack,
  Text,
  Tooltip,
} from '@mantine/core'
import { Dropzone, IMAGE_MIME_TYPE } from '@mantine/dropzone'
import { useMemo, useRef, useState } from 'react'
import { doRequest } from '../../../../requests/request'
import { showSimpleError } from '../../../../utils/notification'
import { IUser } from '../../../../requests/responses/user'

const IMPORT_TOOLTIP =
  'Imports your profile picture from Gravatar (gravatar.com), a US-based service.' +
  ' Your email hash is sent from the server, so your IP address is not exposed to the external service.' +
  ' The image is only fetched once and stored locally.'

interface IAvatarInputProps {
  value: File | undefined
  onChange: (file: File | undefined) => unknown
  label?: string
  required?: boolean
}

const AvatarInput = (props: IAvatarInputProps) => {
  const { value, onChange, label, required } = props

  const editorRef = useRef<AvatarEditor | null>(null)
  const { updateUser } = useAuthenticationContext()
  const user = useLoggedInUser()

  const avatarUrl = useMemo(() => {
    return value ? URL.createObjectURL(value) : getAvatar(user)
  }, [user, value])

  const [file, setFile] = useState<File>()
  const [scale, setScale] = useState(1)
  const [importLoading, setImportLoading] = useState(false)

  const onSave = async () => {
    const canvas = editorRef.current?.getImageScaledToCanvas().toDataURL()

    if (!canvas) {
      return
    }

    const data = await fetch(canvas).then((res) => res.blob())

    onChange(new File([data], 'avatar.png'))
    setFile(undefined)
    setScale(1)
  }

  const importProfilePicture = async () => {
    setImportLoading(true)

    try {
      const response = await doRequest<IUser>('/v2/user-info/import-profile-picture', {
        method: 'POST',
        requiresAuth: true,
      })

      if (!response.ok) {
        throw new Error('No profile picture found for your email address.')
      }

      updateUser(response.data)
    } catch (e: unknown) {
      const message =
        e instanceof Error ? e.message : 'No profile picture found for your email address.'
      showSimpleError(message)
    } finally {
      setImportLoading(false)
    }
  }

  return (
    <Input.Wrapper label={label} required={required}>
      <Dropzone
        onDrop={(files) => {
          setFile(files[0])
        }}
        accept={IMAGE_MIME_TYPE}
      >
        <Group>
          <Avatar
            src={avatarUrl}
            name={`${user.firstName} ${user.lastName}`}
            color='initials'
            size='xl'
          />
          <Stack>
            <Text size='xl' inline>
              Drag the file here or click to select file
            </Text>
          </Stack>
        </Group>
      </Dropzone>
      {user.email && (
        <Group gap='xs' mt='xs'>
          <Tooltip label={IMPORT_TOOLTIP} multiline w={300} withArrow>
            <Button
              variant='subtle'
              size='xs'
              onClick={importProfilePicture}
              loading={importLoading}
            >
              Import from Gravatar
            </Button>
          </Tooltip>
        </Group>
      )}
      <Modal opened={!!file} onClose={() => setFile(undefined)}>
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
            <Button onClick={onSave} fullWidth>
              Save Avatar
            </Button>
          </Stack>
        )}
      </Modal>
    </Input.Wrapper>
  )
}

export default AvatarInput
