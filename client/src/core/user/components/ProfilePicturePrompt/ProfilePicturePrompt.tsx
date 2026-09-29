import { useState } from 'react'
import { useLocation } from 'react-router'
import { Avatar, Button, Group, Modal, Paper, Stack, Text, ThemeIcon, Title } from '@mantine/core'
import { Dropzone, IMAGE_MIME_TYPE } from '@mantine/dropzone'
import { ChatsCircleIcon, ClockIcon, UsersThreeIcon } from '@phosphor-icons/react'
import type { Icon } from '@phosphor-icons/react'
import { useAuthenticationContext } from '@/core/hooks/authentication'
import { usePromptSlot } from '@/core/hooks/prompt-slot'
import { doRequest } from '@/core/requests/request'
import { showSimpleError, showSimpleSuccess } from '@/core/utils/notification'
import type { IUser } from '@/core/user/requests/responses/user'
import AvatarCropModal from '@/core/user/components/AvatarCropModal/AvatarCropModal'

const DISABLE_PROMPT_STORAGE_KEY = 'profile_picture_prompt_disabled'

const isPromptDisabled = () => {
  try {
    return localStorage.getItem(DISABLE_PROMPT_STORAGE_KEY) === 'true'
  } catch {
    return false
  }
}

const BENEFITS: Array<{ icon: Icon; title: string; text: string }> = [
  {
    icon: UsersThreeIcon,
    title: 'Get recognized',
    text: 'Advisors, supervisors and fellow students see who they are talking to in interviews, presentations and thesis discussions.',
  },
  {
    icon: ChatsCircleIcon,
    title: 'Make communication personal',
    text: 'A friendly face makes feedback rounds and questions feel less anonymous and easier to start.',
  },
  {
    icon: ClockIcon,
    title: 'Takes ten seconds',
    text: 'Upload a photo or import it from Gravatar. It is optional, and you can change or remove it any time in your settings.',
  },
]

const ProfilePicturePrompt = () => {
  const { user, updateUser } = useAuthenticationContext()
  const location = useLocation()

  const [dismissedForUserId, setDismissedForUserId] = useState<string>()
  const [file, setFile] = useState<File>()
  const [isUploading, setIsUploading] = useState(false)
  const [isImporting, setIsImporting] = useState(false)

  const userId = user?.userId
  const isBusy = isUploading || isImporting

  const shouldPrompt =
    Boolean(user) &&
    !user?.avatar &&
    !user?.avatarPromptDismissed &&
    dismissedForUserId !== userId &&
    location.pathname !== '/logout' &&
    !isPromptDisabled()

  const hasPromptSlot = usePromptSlot('profile-picture', shouldPrompt)

  const dismiss = async () => {
    // Close right away; the server remembers the decision so the user is never asked again.
    setDismissedForUserId(userId)

    try {
      const response = await doRequest<IUser>('/v2/user-info/dismiss-avatar-prompt', {
        method: 'POST',
        requiresAuth: true,
      })

      if (response.ok) {
        updateUser(response.data)
      }
    } catch (error) {
      console.error('Failed to store that the profile picture prompt was dismissed', error)
    }
  }

  const upload = async (avatar: File) => {
    setFile(undefined)
    setIsUploading(true)

    try {
      const formData = new FormData()
      formData.append('avatar', avatar)

      const response = await doRequest<IUser>('/v2/user-info/avatar', {
        method: 'POST',
        requiresAuth: true,
        formData,
      })

      if (!response.ok) {
        throw new Error('Could not save your profile picture. Please try another image.')
      }

      updateUser(response.data)
      showSimpleSuccess('Profile picture saved')
    } catch (error) {
      showSimpleError(
        error instanceof Error ? error.message : 'Could not save your profile picture.',
      )
    } finally {
      setIsUploading(false)
    }
  }

  const importFromGravatar = async () => {
    setIsImporting(true)

    try {
      const response = await doRequest<IUser>('/v2/user-info/import-profile-picture', {
        method: 'POST',
        requiresAuth: true,
      })

      if (!response.ok) {
        throw new Error('No profile picture found for your email address.')
      }

      updateUser(response.data)
      showSimpleSuccess('Profile picture imported')
    } catch (error) {
      showSimpleError(
        error instanceof Error ? error.message : 'No profile picture found for your email address.',
      )
    } finally {
      setIsImporting(false)
    }
  }

  if (!user) {
    return null
  }

  return (
    <>
      <Modal
        opened={hasPromptSlot && !file}
        onClose={() => void dismiss()}
        title='Add a profile picture'
        size='lg'
        centered
        closeOnClickOutside={!isBusy}
        closeOnEscape={!isBusy}
        withCloseButton={!isBusy}
      >
        <Stack gap='lg'>
          <Group align='center' wrap='nowrap' gap='md'>
            <Avatar
              size={72}
              name={`${user.firstName ?? ''} ${user.lastName ?? ''}`}
              color='initials'
            />
            <Stack gap={4}>
              <Title order={3} size='h3' lh={1.2}>
                Put a face to your name
              </Title>
              <Text size='sm' c='dimmed'>
                Welcome, {user.firstName ?? 'there'}! Here is why a profile picture helps.
              </Text>
            </Stack>
          </Group>

          <Paper withBorder radius='md' p='md' bg='var(--mantine-color-blue-light)'>
            <Stack gap='sm'>
              {BENEFITS.map((benefit) => (
                <Group key={benefit.title} gap='sm' wrap='nowrap' align='flex-start'>
                  <ThemeIcon
                    variant='light'
                    color='blue'
                    size='md'
                    radius='xl'
                    style={{ flexShrink: 0 }}
                  >
                    <benefit.icon size={16} weight='bold' />
                  </ThemeIcon>
                  <Text size='sm' style={{ minWidth: 0 }}>
                    <Text span fw={600}>
                      {benefit.title}:
                    </Text>{' '}
                    {benefit.text}
                  </Text>
                </Group>
              ))}
            </Stack>
          </Paper>

          <Dropzone
            onDrop={(files) => setFile(files[0])}
            accept={IMAGE_MIME_TYPE}
            multiple={false}
            loading={isUploading}
            disabled={isBusy}
          >
            <Text ta='center'>Drag a picture here or click to select a file</Text>
          </Dropzone>

          <Group justify='space-between'>
            <Button
              variant='subtle'
              onClick={() => void importFromGravatar()}
              loading={isImporting}
              disabled={isUploading || !user.email}
            >
              Import from Gravatar
            </Button>
            <Button variant='default' onClick={() => void dismiss()} disabled={isBusy}>
              Maybe not
            </Button>
          </Group>
          <Text size='xs' c='dimmed' ta='center'>
            We will not ask again. You can add a picture at any time in Settings.
          </Text>
        </Stack>
      </Modal>
      <AvatarCropModal
        file={file}
        onClose={() => setFile(undefined)}
        onSave={(f) => void upload(f)}
      />
    </>
  )
}

export default ProfilePicturePrompt
