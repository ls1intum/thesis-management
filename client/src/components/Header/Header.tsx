import {
  Burger,
  Button,
  Divider,
  Flex,
  Group,
  Menu,
  Modal,
  Skeleton,
  Stack,
  Text,
  UnstyledButton,
  useComputedColorScheme,
} from '@mantine/core'
import Logo from '../Logo/Logo'
import { Link, useNavigate } from 'react-router'
import { ColorSchemeToggleButton } from '../ColorSchemeToggleButton/ColorSchemeToggleButton'
import { useAuthenticationContext, useUser } from '../../hooks/authentication'
import { CustomAvatar } from '../CustomAvatar/CustomAvatar'
import { GearSixIcon, KeyIcon, NewspaperClippingIcon, SignOutIcon } from '@phosphor-icons/react'
import { getPasskeyErrorMessage } from '../../utils/passkey'
import { showSimpleError } from '../../utils/notification'
import { useEffect, useState } from 'react'

interface HeaderProps {
  authenticatedArea: boolean
  opened?: boolean | undefined
  toggle?: () => void
  openLoginModal?: boolean
  hideUnauthenticatedActions?: boolean
}

const Header = ({
  opened,
  toggle,
  authenticatedArea,
  openLoginModal = false,
  hideUnauthenticatedActions = false,
}: HeaderProps) => {
  const colorScheme = useComputedColorScheme('light')
  const user = useUser()
  const context = useAuthenticationContext()
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false)
  const [isPasskeyLoading, setIsPasskeyLoading] = useState(false)

  const navigate = useNavigate()
  const isLoginModalForcedOpen =
    openLoginModal && !context.isAuthenticated && context.isPasskeySupported

  useEffect(() => {
    if (!openLoginModal || context.isAuthenticated || !context.isPasskeySupported) {
      setIsLoginModalOpen(false)
      return
    }

    setIsLoginModalOpen(true)
  }, [context.isAuthenticated, context.isPasskeySupported, openLoginModal])

  const onLoginModalClose = () => {
    if (isLoginModalForcedOpen) {
      void navigate('/', { replace: true })
      return
    }

    setIsLoginModalOpen(false)
  }

  const onPasswordLogin = () => {
    setIsLoginModalOpen(false)
    void context.login('/dashboard')
  }

  const onPasskeyLogin = async () => {
    setIsPasskeyLoading(true)
    try {
      await context.loginWithPasskey()
      setIsLoginModalOpen(false)
      void navigate('/dashboard', { replace: true })
    } catch (error) {
      showSimpleError(await getPasskeyErrorMessage(error, 'Passkey login failed'))
    } finally {
      setIsPasskeyLoading(false)
    }
  }

  // Render the brand (logo + title) as a real <a> via react-router's Link so the
  // browser's native right-click menu ("Open in new tab", "Copy link") works.
  // A <div onClick={navigate}> would only handle left-click and would offer the
  // image context menu when right-clicking the logo.
  return (
    <Flex justify='space-between' align='center' h='100%' w='100%'>
      <Flex
        component={Link}
        to={authenticatedArea ? '/dashboard' : '/'}
        gap={'xs'}
        justify='flex-start'
        align='center'
        h='100%'
        style={{ textDecoration: 'none', color: 'inherit' }}
      >
        <Logo size={40} />
        <Text fw='bold' visibleFrom='sm' pt='2px'>
          Thesis Management
        </Text>
      </Flex>

      <Flex
        justify='space-between'
        align='center'
        h='100%'
        gap='md'
        visibleFrom={authenticatedArea ? 'md' : undefined}
      >
        <ColorSchemeToggleButton iconSize={'70%'} size={'lg'} />
        {!authenticatedArea && context.isAuthenticated && (
          <Button
            component={Link}
            to='/dashboard'
            variant='outline'
            color={colorScheme === 'dark' ? 'gray.4' : 'dark.2'}
            visibleFrom='xs'
            p={'xs'}
          >
            <Group gap='xs' align='center' p={0}>
              <NewspaperClippingIcon size={16} />
              <Text>Dashboard</Text>
            </Group>
          </Button>
        )}
        {context.isAuthenticated ? (
          <Menu
            shadow='md'
            width={200}
            position='bottom-end'
            withArrow
            transitionProps={{ transition: 'scale-y', duration: 200 }}
          >
            <Menu.Target>
              <UnstyledButton>
                <Group gap='xs' align='center'>
                  {user ? <CustomAvatar user={user} size={35} /> : <Skeleton height={35} circle />}
                </Group>
              </UnstyledButton>
            </Menu.Target>

            <Menu.Dropdown>
              {!authenticatedArea && (
                <Menu.Item
                  component={Link}
                  to='/dashboard'
                  leftSection={<NewspaperClippingIcon size={16} />}
                  hiddenFrom='xs'
                >
                  Go to Dashboard
                </Menu.Item>
              )}

              <Menu.Item component={Link} to='/settings' leftSection={<GearSixIcon size={16} />}>
                Settings
              </Menu.Item>
              <Divider />
              <Menu.Item component={Link} to='/logout' leftSection={<SignOutIcon size={16} />}>
                Logout
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        ) : (
          !hideUnauthenticatedActions && (
            <Group gap='xs'>
              {context.isPasskeySupported && (
                <Button
                  variant='outline'
                  leftSection={<KeyIcon size={16} />}
                  onClick={() => void onPasskeyLogin()}
                  loading={isPasskeyLoading}
                >
                  <Text span visibleFrom='sm'>
                    AET Passkey
                  </Text>
                </Button>
              )}
              <Button onClick={() => void context.login('/dashboard')}>Login</Button>
            </Group>
          )
        )}
      </Flex>
      <Modal
        opened={isLoginModalForcedOpen ?? isLoginModalOpen}
        onClose={onLoginModalClose}
        closeOnEscape={!isPasskeyLoading}
        withCloseButton={!isPasskeyLoading}
        closeOnClickOutside={false}
        title='Login'
        centered
      >
        <Stack>
          <Text size='sm' c='dimmed'>
            Choose your preferred sign-in method.
          </Text>
          <Flex direction='column' gap='md'>
            {context.isPasskeySupported && (
              <Button
                variant='outline'
                leftSection={<KeyIcon size={16} />}
                onClick={() => void onPasskeyLogin()}
                loading={isPasskeyLoading}
              >
                AET Passkey
              </Button>
            )}
            <Button onClick={onPasswordLogin} disabled={isPasskeyLoading}>
              Login
            </Button>
          </Flex>
        </Stack>
      </Modal>
      {authenticatedArea && opened !== undefined && (
        <Group h='100%' hiddenFrom='md'>
          <Burger opened={opened} onClick={toggle} size='md' />
        </Group>
      )}
    </Flex>
  )
}

export default Header
