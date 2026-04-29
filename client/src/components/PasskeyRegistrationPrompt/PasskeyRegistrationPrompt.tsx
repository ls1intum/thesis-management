import { Button, Checkbox, Group, Modal, Stack, Text } from '@mantine/core'
import { useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router'
import { useAuthenticationContext } from '../../hooks/authentication'
import { getPasskeyErrorMessage, isPasskeyCredential } from '../../utils/passkey'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'

const NEVER_ASK_AGAIN_STORAGE_KEY = 'passkey_prompt_never_ask_again'
const DISABLE_PROMPT_STORAGE_KEY = 'passkey_prompt_disabled'

const PasskeyRegistrationPrompt = () => {
  const auth = useAuthenticationContext()
  const location = useLocation()
  const [isOpen, setIsOpen] = useState(false)
  const [isRegistering, setIsRegistering] = useState(false)
  const [neverAskAgain, setNeverAskAgain] = useState(false)
  const [checkedUserId, setCheckedUserId] = useState<string>()

  const userId = auth.user?.userId
  const perUserNeverAskAgainStorageKey = userId
    ? `${NEVER_ASK_AGAIN_STORAGE_KEY}_${userId}`
    : undefined
  const shouldSkipPrompt = useMemo(() => {
    if (typeof window === 'undefined') {
      return false
    }

    if (localStorage.getItem(DISABLE_PROMPT_STORAGE_KEY) === 'true') {
      return true
    }

    if (!perUserNeverAskAgainStorageKey) {
      return false
    }

    return localStorage.getItem(perUserNeverAskAgainStorageKey) === 'true'
  }, [perUserNeverAskAgainStorageKey])

  useEffect(() => {
    if (!auth.isAuthenticated) {
      setCheckedUserId(undefined)
      setIsOpen(false)
      setNeverAskAgain(false)
    }
  }, [auth.isAuthenticated])

  useEffect(() => {
    if (!auth.isAuthenticated || !userId || location.pathname === '/logout') {
      return
    }

    if (!auth.isPasskeySupported || shouldSkipPrompt || checkedUserId === userId) {
      return
    }

    let isMounted = true

    const checkForPasskeys = async () => {
      try {
        const allCredentials = await auth.listCredentials()
        const hasPasskey = allCredentials.some(isPasskeyCredential)

        if (isMounted) {
          setCheckedUserId(userId)
        }

        if (!hasPasskey && isMounted) {
          setNeverAskAgain(false)
          setIsOpen(true)
        }
      } catch (error) {
        if (isMounted) {
          setCheckedUserId(userId)
        }
        console.error('Failed to fetch passkey credentials for login prompt', error)
      }
    }

    void checkForPasskeys()

    return () => {
      isMounted = false
    }
  }, [
    auth,
    auth.isAuthenticated,
    auth.isPasskeySupported,
    checkedUserId,
    location.pathname,
    shouldSkipPrompt,
    userId,
  ])

  const persistNeverAskAgain = () => {
    if (!neverAskAgain || !perUserNeverAskAgainStorageKey) {
      return
    }

    localStorage.setItem(perUserNeverAskAgainStorageKey, 'true')
  }

  const closeModal = () => {
    persistNeverAskAgain()
    setIsOpen(false)
  }

  const onRegisterPasskey = async () => {
    setIsRegistering(true)
    try {
      await auth.registerPasskey()
      showSimpleSuccess('Passkey registered successfully')
      setIsOpen(false)
    } catch (error) {
      showSimpleError(await getPasskeyErrorMessage(error))
    } finally {
      setIsRegistering(false)
    }
  }

  return (
    <Modal
      opened={isOpen}
      onClose={closeModal}
      title='Register a passkey'
      centered
      closeOnClickOutside={!isRegistering}
      closeOnEscape={!isRegistering}
      withCloseButton={!isRegistering}
    >
      <Stack>
        <Text size='sm' c='dimmed'>
          You do not have a passkey registered yet. Register one now to sign in faster next time.
        </Text>
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
            Register passkey
          </Button>
        </Group>
      </Stack>
    </Modal>
  )
}

export default PasskeyRegistrationPrompt
