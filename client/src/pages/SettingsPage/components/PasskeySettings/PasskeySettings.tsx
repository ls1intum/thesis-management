import { Alert, Button, Group, Loader, Paper, Stack, Text, Title } from '@mantine/core'
import { useEffect, useState } from 'react'
import { useAuthenticationContext } from '../../../../hooks/authentication'
import { showSimpleError, showSimpleSuccess } from '../../../../utils/notification'
import { IKeycloakCredential } from '../../../../providers/AuthenticationContext/context'

const getErrorMessage = (error: unknown) => {
  if (error instanceof DOMException && error.name === 'NotAllowedError') {
    return 'Passkey request was cancelled or timed out'
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }

  return 'Passkey operation failed'
}

const isPasskeyCredential = (credential: IKeycloakCredential) =>
  credential.type?.toLowerCase().includes('webauthn') ?? false

const formatCreatedDate = (createdDate?: number) => {
  if (!createdDate) {
    return 'Creation time unknown'
  }

  const milliseconds = createdDate > 10_000_000_000 ? createdDate : createdDate * 1000
  return `Created ${new Date(milliseconds).toLocaleString()}`
}

const PasskeySettings = () => {
  const auth = useAuthenticationContext()
  const [isRegistering, setIsRegistering] = useState(false)
  const [credentials, setCredentials] = useState<IKeycloakCredential[]>([])
  const [isLoadingCredentials, setIsLoadingCredentials] = useState(true)
  const [deletingCredentialId, setDeletingCredentialId] = useState<string>()

  useEffect(() => {
    const fetchCredentials = async () => {
      setIsLoadingCredentials(true)
      try {
        const allCredentials = await auth.listCredentials()
        const passkeys = allCredentials
          .filter(isPasskeyCredential)
          .sort((a, b) => (b.createdDate ?? 0) - (a.createdDate ?? 0))
        setCredentials(passkeys)
      } catch (error) {
        showSimpleError(getErrorMessage(error))
      } finally {
        setIsLoadingCredentials(false)
      }
    }

    void fetchCredentials()
  }, [auth])

  const refreshCredentials = async () => {
    try {
      const allCredentials = await auth.listCredentials()
      const passkeys = allCredentials
        .filter(isPasskeyCredential)
        .sort((a, b) => (b.createdDate ?? 0) - (a.createdDate ?? 0))
      setCredentials(passkeys)
    } catch (error) {
      showSimpleError(getErrorMessage(error))
    }
  }

  const onRegisterPasskey = async () => {
    setIsRegistering(true)
    try {
      await auth.registerPasskey()
      showSimpleSuccess('Passkey registered successfully')
      await refreshCredentials()
    } catch (error) {
      showSimpleError(getErrorMessage(error))
    } finally {
      setIsRegistering(false)
    }
  }

  const onDeleteCredential = async (credentialId: string) => {
    setDeletingCredentialId(credentialId)
    try {
      await auth.deleteCredential(credentialId)
      showSimpleSuccess('Passkey deleted successfully')
      await refreshCredentials()
    } catch (error) {
      showSimpleError(getErrorMessage(error))
    } finally {
      setDeletingCredentialId(undefined)
    }
  }

  return (
    <Stack>
      <Title order={3}>Passkeys</Title>
      <Text>
        Register a passkey on this device to sign in without entering your password next time.
      </Text>
      <Text size='sm' c='dimmed'>
        Your registered passkeys are loaded from Keycloak account credentials.
      </Text>
      {isLoadingCredentials && <Loader size='sm' />}
      {!isLoadingCredentials && credentials.length === 0 && (
        <Text size='sm' c='dimmed'>
          No passkeys registered yet.
        </Text>
      )}
      {!isLoadingCredentials && credentials.length > 0 && (
        <Stack gap='sm'>
          {credentials.map((credential) => (
            <Paper withBorder p='sm' key={credential.id}>
              <Group justify='space-between' align='center'>
                <Stack gap={0}>
                  <Text fw={500}>{credential.userLabel?.trim() || 'Unnamed passkey'}</Text>
                  <Text size='sm' c='dimmed'>
                    {formatCreatedDate(credential.createdDate)}
                  </Text>
                </Stack>
                <Button
                  color='red'
                  variant='light'
                  onClick={() => void onDeleteCredential(credential.id)}
                  loading={deletingCredentialId === credential.id}
                >
                  Delete
                </Button>
              </Group>
            </Paper>
          ))}
        </Stack>
      )}
      <Group>
        {!auth.isPasskeySupported && (
          <Alert color='yellow' title='Passkeys are unavailable'>
            Use a compatible browser on HTTPS (or localhost) and try again.
          </Alert>
        )}
        <Button
          onClick={() => void onRegisterPasskey()}
          loading={isRegistering}
          disabled={!auth.isPasskeySupported}
        >
          Register Passkey
        </Button>
      </Group>
    </Stack>
  )
}

export default PasskeySettings
