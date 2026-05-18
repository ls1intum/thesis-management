import {
  Anchor,
  Badge,
  Button,
  Checkbox,
  Group,
  Modal,
  Paper,
  Stack,
  Text,
  ThemeIcon,
  Title,
  Tooltip,
} from '@mantine/core'
import { FingerprintIcon, LightningIcon, ShieldCheckIcon } from '@phosphor-icons/react'
import { useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router'
import { useAuthenticationContext } from '../../hooks/authentication'
import { getPasskeyErrorMessage, isPasskeyCredential } from '../../utils/passkey'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'
import { GLOBAL_CONFIG } from '../../config/global'

const NEVER_ASK_AGAIN_STORAGE_KEY = 'passkey_prompt_never_ask_again'
const MAYBE_LATER_STORAGE_KEY = 'passkey_prompt_maybe_later'
const DISABLE_PROMPT_STORAGE_KEY = 'passkey_prompt_disabled'

const ChairLink = () => (
  <Tooltip label={GLOBAL_CONFIG.chair_name} withArrow>
    <Anchor href={GLOBAL_CONFIG.chair_url} target='_blank' rel='noreferrer' inherit>
      {GLOBAL_CONFIG.chair_name}
    </Anchor>
  </Tooltip>
)

const getTodayStorageValue = () => {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')

  return `${now.getFullYear()}-${month}-${day}`
}

const shouldSkipPromptForStorageKeys = (
  perUserNeverAskAgainStorageKey: string | undefined,
  perUserMaybeLaterStorageKey: string | undefined,
) => {
  if (typeof window === 'undefined') {
    return false
  }

  if (localStorage.getItem(DISABLE_PROMPT_STORAGE_KEY) === 'true') {
    return true
  }

  if (!perUserNeverAskAgainStorageKey) {
    return false
  }

  if (localStorage.getItem(perUserNeverAskAgainStorageKey) === 'true') {
    return true
  }

  if (!perUserMaybeLaterStorageKey) {
    return false
  }

  return localStorage.getItem(perUserMaybeLaterStorageKey) === getTodayStorageValue()
}

const PasskeyRegistrationPrompt = () => {
  const auth = useAuthenticationContext()
  const { isAuthenticated, isPasskeySupported, listCredentials, registerPasskey, user } = auth
  const location = useLocation()
  const promptApps = GLOBAL_CONFIG.passkey_prompt_apps
  const [isOpen, setIsOpen] = useState(false)
  const [isRegistering, setIsRegistering] = useState(false)
  const [neverAskAgain, setNeverAskAgain] = useState(false)
  const checkedUserIdRef = useRef<string | undefined>(undefined)
  const checkingUserIdRef = useRef<string | undefined>(undefined)
  const listCredentialsRef = useRef(listCredentials)

  const userId = user?.userId
  const perUserNeverAskAgainStorageKey = userId
    ? `${NEVER_ASK_AGAIN_STORAGE_KEY}_${userId}`
    : undefined
  const perUserMaybeLaterStorageKey = userId ? `${MAYBE_LATER_STORAGE_KEY}_${userId}` : undefined
  const [shouldSkipPrompt, setShouldSkipPrompt] = useState(() =>
    shouldSkipPromptForStorageKeys(perUserNeverAskAgainStorageKey, perUserMaybeLaterStorageKey),
  )

  useEffect(() => {
    setShouldSkipPrompt(
      shouldSkipPromptForStorageKeys(perUserNeverAskAgainStorageKey, perUserMaybeLaterStorageKey),
    )
  }, [perUserMaybeLaterStorageKey, perUserNeverAskAgainStorageKey])

  useEffect(() => {
    listCredentialsRef.current = listCredentials
  }, [listCredentials])

  useEffect(() => {
    if (!isAuthenticated) {
      checkedUserIdRef.current = undefined
      checkingUserIdRef.current = undefined
      setIsOpen(false)
      setNeverAskAgain(false)
    }
  }, [isAuthenticated])

  useEffect(() => {
    if (!isAuthenticated || !userId || location.pathname === '/logout') {
      return
    }

    if (
      !isPasskeySupported ||
      shouldSkipPrompt ||
      checkedUserIdRef.current === userId ||
      checkingUserIdRef.current === userId
    ) {
      return
    }

    let isMounted = true
    checkingUserIdRef.current = userId

    const checkForPasskeys = async () => {
      try {
        const allCredentials = await listCredentialsRef.current()
        const hasPasskey = allCredentials.some(isPasskeyCredential)

        if (isMounted) {
          checkedUserIdRef.current = userId
        }

        if (!hasPasskey && isMounted) {
          setNeverAskAgain(false)
          setIsOpen(true)
        }
      } catch (error) {
        console.error('Failed to fetch passkey credentials for login prompt', error)
      } finally {
        if (checkingUserIdRef.current === userId) {
          checkingUserIdRef.current = undefined
        }
      }
    }

    void checkForPasskeys()

    return () => {
      isMounted = false
    }
  }, [isAuthenticated, isPasskeySupported, location.pathname, shouldSkipPrompt, userId])

  const persistPromptDismissal = () => {
    if (neverAskAgain && perUserNeverAskAgainStorageKey) {
      localStorage.setItem(perUserNeverAskAgainStorageKey, 'true')
      setShouldSkipPrompt(true)
      return
    }

    if (perUserMaybeLaterStorageKey) {
      localStorage.setItem(perUserMaybeLaterStorageKey, getTodayStorageValue())
      setShouldSkipPrompt(true)
    }
  }

  const closeModal = () => {
    persistPromptDismissal()
    setIsOpen(false)
  }

  const onRegisterPasskey = async () => {
    setIsRegistering(true)
    try {
      await registerPasskey()
      showSimpleSuccess('Passkey registered successfully')
      setIsOpen(false)
    } catch (error) {
      showSimpleError(await getPasskeyErrorMessage(error, undefined, 'register'))
    } finally {
      setIsRegistering(false)
    }
  }

  if (!isPasskeySupported) {
    return null
  }

  return (
    <Modal
      opened={isOpen}
      onClose={closeModal}
      title='Register a passkey'
      size='lg'
      centered
      closeOnClickOutside={!isRegistering}
      closeOnEscape={!isRegistering}
      withCloseButton={!isRegistering}
    >
      <Stack gap='lg'>
        <Group align='flex-start' wrap='nowrap' gap='md'>
          <ThemeIcon size={64} radius='md' variant='light' color='blue' style={{ flexShrink: 0 }}>
            <FingerprintIcon size={36} weight='duotone' />
          </ThemeIcon>
          <Stack gap={6} style={{ minWidth: 0 }}>
            <Title order={3} size='h3' lh={1.2}>
              Your fast track to <ChairLink /> {promptApps.length > 0 ? 'apps' : null}
            </Title>
            <Text size='sm'>
              {promptApps.length > 0
                ? 'One passkey for fast, secure sign-in across multiple apps.'
                : 'Use a passkey for fast, secure sign-in.'}
            </Text>
            {promptApps.length > 0 && (
              <Group gap='xs'>
                {promptApps.map((appName) => (
                  <Badge key={appName} variant='filled' color='blue' radius='sm'>
                    {appName}
                  </Badge>
                ))}
              </Group>
            )}
          </Stack>
        </Group>

        <Paper withBorder radius='md' p='md' bg='var(--mantine-color-blue-light)'>
          <Stack gap='sm'>
            <Group gap='sm' wrap='nowrap' align='flex-start'>
              <ThemeIcon
                variant='light'
                color='teal'
                size='sm'
                radius='xl'
                style={{ flexShrink: 0 }}
              >
                <LightningIcon size={14} weight='bold' />
              </ThemeIcon>
              <Text size='sm' style={{ minWidth: 0 }}>
                <Text span fw={600}>
                  Faster sign-in:
                </Text>{' '}
                skip repeated login screens after your device recognizes you.
              </Text>
            </Group>
            <Group gap='sm' wrap='nowrap' align='flex-start'>
              <ThemeIcon
                variant='light'
                color='green'
                size='sm'
                radius='xl'
                style={{ flexShrink: 0 }}
              >
                <ShieldCheckIcon size={14} weight='bold' />
              </ThemeIcon>
              <Text size='sm' style={{ minWidth: 0 }}>
                <Text span fw={600}>
                  More secure than passwords:
                </Text>{' '}
                phishing-resistant and bound to your device.
              </Text>
            </Group>
          </Stack>
        </Paper>

        <Checkbox
          label='Never ask again'
          checked={neverAskAgain}
          onChange={(event) => setNeverAskAgain(event.currentTarget.checked)}
          disabled={isRegistering}
        />
        <Group justify='flex-end'>
          <Button variant='default' onClick={closeModal} disabled={isRegistering}>
            Maybe later
          </Button>
          <Button onClick={() => void onRegisterPasskey()} loading={isRegistering}>
            Register Passkey
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export default PasskeyRegistrationPrompt
