import { Button, Checkbox, Group, Modal, Stack, Text } from '@mantine/core'
import { useEffect, useState } from 'react'
import { useLocation } from 'react-router'
import { useAuthenticationContext } from '../../hooks/authentication'
import { useLocalStorage } from '../../hooks/local-storage'
import { IKeycloakCredential } from '../../providers/AuthenticationContext/context'
import { showSimpleError, showSimpleSuccess } from '../../utils/notification'

const NEVER_ASK_AGAIN_STORAGE_KEY = 'passkey_prompt_never_ask_again'

const isPasskeyCredential = (credential: IKeycloakCredential) =>
  credential.type?.toLowerCase().includes('webauthn') ?? false

const getErrorMessage = (error: unknown) => {
  if (error instanceof DOMException && error.name === 'NotAllowedError') {
    return 'Passkey request was cancelled or timed out'
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }

  return 'Passkey operation failed'
}

const PasskeyRegistrationPrompt = () => {
  const auth = useAuthenticationContext()
  const location = useLocation()
  const [isOpen, setIsOpen] = useState(false)
  const [isRegistering, setIsRegistering] = useState(false)
  const [neverAskAgain, setNeverAskAgain] = useState(false)
  const [checkedUserId, setCheckedUserId] = useState<string>()
  const [neverAskAgainPreference, setNeverAskAgainPreference] = useLocalStorage<boolean>(
    NEVER_ASK_AGAIN_STORAGE_KEY,
    { usingJson: true },
  )

  const userId = auth.user?.userId
  const shouldSkipPrompt = neverAskAgainPreference === true

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
    if (!neverAskAgain) {
      return
    }

    setNeverAskAgainPreference(true)
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
      showSimpleError(getErrorMessage(error))
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
