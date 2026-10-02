import { CustomAvatar } from '@/core/components/CustomAvatar/CustomAvatar'
import { useAuthenticationContext, useLoggedInUser } from '@/core/hooks/authentication'
import { Avatar, Button, Group, Input, Stack, Text, Tooltip } from '@mantine/core'
import { Dropzone, IMAGE_MIME_TYPE } from '@mantine/dropzone'
import { useEffect, useMemo, useState } from 'react'
import { doRequest } from '@/core/requests/request'
import { showSimpleError } from '@/core/utils/notification'
import type { IUser } from '@/core/user/requests/responses/user'
import AvatarCropModal from '@/core/user/components/AvatarCropModal/AvatarCropModal'

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

  const { updateUser } = useAuthenticationContext()
  const user = useLoggedInUser()

  // Preview of a picture that was chosen but not saved yet.
  const previewUrl = useMemo(() => (value ? URL.createObjectURL(value) : undefined), [value])

  useEffect(
    () => () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    },
    [previewUrl],
  )

  const [file, setFile] = useState<File>()
  const [importLoading, setImportLoading] = useState(false)

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
          {previewUrl ? (
            <Avatar
              src={previewUrl}
              name={`${user.firstName} ${user.lastName}`}
              color='initials'
              size='xl'
            />
          ) : (
            // The saved picture is only served to signed-in users (and publicly listed people), so it has to
            // be loaded with the login token; a plain image URL shows initials for most users.
            <CustomAvatar user={user} size='xl' />
          )}
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
              onClick={() => {
                void importProfilePicture()
              }}
              loading={importLoading}
            >
              Import from Gravatar
            </Button>
          </Tooltip>
        </Group>
      )}
      <AvatarCropModal
        file={file}
        onClose={() => setFile(undefined)}
        onSave={(cropped) => {
          onChange(cropped)
          setFile(undefined)
        }}
      />
    </Input.Wrapper>
  )
}

export default AvatarInput
