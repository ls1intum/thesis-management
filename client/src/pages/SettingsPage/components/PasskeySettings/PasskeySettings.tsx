import { Button, Group, Loader, Paper, Stack, Text, Title } from '@mantine/core'
import { useCallback, useEffect, useState } from 'react'
import { useAuthenticationContext } from '../../../../hooks/authentication'
import { showSimpleError, showSimpleSuccess } from '../../../../utils/notification'
import type { IKeycloakCredential } from '../../../../providers/AuthenticationContext/context'
import { getPasskeyErrorMessage, isPasskeyCredential } from '../../../../utils/passkey'

const formatCreatedDate = (createdDate?: number) => {
  if (!createdDate) {
    return 'Creation time unknown'
  }

  return `Created ${new Date(createdDate).toLocaleString()}`
}

const PasskeySettings = () => {
  const auth = useAuthenticationContext()
  const [isRegistering, setIsRegistering] = useState(false)
  const [credentials, setCredentials] = useState<IKeycloakCredential[]>([])
  const [isLoadingCredentials, setIsLoadingCredentials] = useState(true)
  const [deletingCredentialId, setDeletingCredentialId] = useState<string>()

  const loadPasskeyCredentials = useCallback(async () => {
    const allCredentials = await auth.listCredentials()
    const passkeys = allCredentials
      .filter(isPasskeyCredential)
      .sort((a, b) => (b.createdDate ?? 0) - (a.createdDate ?? 0))

    setCredentials(passkeys)
  }, [auth])

  useEffect(() => {
    if (!auth.isPasskeySupported) {
      setCredentials([])
      setIsLoadingCredentials(false)
      return
    }

    const fetchCredentials = async () => {
      setIsLoadingCredentials(true)
      try {
        await loadPasskeyCredentials()
      } catch (error) {
        showSimpleError(await getPasskeyErrorMessage(error, undefined, 'list'))
      } finally {
        setIsLoadingCredentials(false)
      }
    }

    void fetchCredentials()
  }, [auth.isPasskeySupported, loadPasskeyCredentials])

  const onRegisterPasskey = async () => {
    setIsRegistering(true)
    try {
      await auth.registerPasskey()
      showSimpleSuccess('Passkey registered. You can now use it to sign in.')
      await loadPasskeyCredentials()
    } catch (error) {
      showSimpleError(await getPasskeyErrorMessage(error, undefined, 'register'))
    } finally {
      setIsRegistering(false)
    }
  }

  const onDeleteCredential = async (credentialId: string) => {
    setDeletingCredentialId(credentialId)
    try {
      await auth.deleteCredential(credentialId)
      showSimpleSuccess('Passkey deleted successfully.')
      await loadPasskeyCredentials()
    } catch (error) {
      showSimpleError(await getPasskeyErrorMessage(error, undefined, 'delete'))
    } finally {
      setDeletingCredentialId(undefined)
    }
  }

  if (!auth.isPasskeySupported) {
    return null
  }

  return (
    <Stack>
      <Title order={3}>Passkeys</Title>
      <Text>Register a passkey on this device to sign in across AET apps.</Text>
      <Text size='sm' c='dimmed'>
        These passkeys are linked to your AET account.
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
                  <Text fw={500}>{credential.userLabel?.trim() ?? 'Unnamed passkey'}</Text>
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
        <Button onClick={() => void onRegisterPasskey()} loading={isRegistering}>
          Register Passkey
        </Button>
      </Group>
    </Stack>
  )
}

export default PasskeySettings
