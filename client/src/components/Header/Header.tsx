import {
  Burger,
  Button,
  Divider,
  Flex,
  Group,
  Menu,
  Stack,
  Text,
  UnstyledButton,
} from '@mantine/core'
import Logo from '../Logo/Logo'
import { Link } from 'react-router'
import ColorSchemeToggleButton from '../ColorSchemeToggleButton/ColorSchemeToggleButton'
import { useUser } from '../../hooks/authentication'
import CustomAvatar from '../CustomAvatar/CustomAvatar'
import { formatUser } from '../../utils/format'
import { CaretDown, Divide, GearSix, NewspaperClipping, SignOut } from 'phosphor-react'

interface HeaderProps {
  authenticatedArea: boolean
  opened?: boolean | undefined
  toggle?: () => void
}

const Header = ({ opened, toggle, authenticatedArea }: HeaderProps) => {
  //TODO: THIS ALLWAYS THROWS AND ERROR WHEN USER IS NOT LOGGED IN I DONT WANT THAT HERE
  const user = useUser()

  return (
    <Flex justify='space-between' align='center' h='100%' w='100%'>
      <Flex
        gap={'xs'}
        justify='flex-start'
        align='center'
        h='100%'
        style={{ cursor: 'pointer' }}
        onClick={() => (window.location.href = '/')}
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
        <ColorSchemeToggleButton iconSize={'70%'} size={'md'} />
        {user ? (
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
                  <CustomAvatar user={user} size={30} />
                </Group>
              </UnstyledButton>
            </Menu.Target>

            <Menu.Dropdown>
              {!authenticatedArea && (
                <Menu.Item
                  component={Link}
                  to='/dashboard'
                  leftSection={<NewspaperClipping size={16} />}
                >
                  Dashboard
                </Menu.Item>
              )}
              <Menu.Item component={Link} to='/settings' leftSection={<GearSix size={16} />}>
                Settings
              </Menu.Item>
              <Divider />
              <Menu.Item component={Link} to='/logout' leftSection={<SignOut size={16} />}>
                Logout
              </Menu.Item>
            </Menu.Dropdown>
          </Menu>
        ) : (
          <Button component={Link} to='/dashboard'>
            Login
          </Button>
        )}
      </Flex>
      {authenticatedArea && opened !== undefined && (
        <Group h='100%' hiddenFrom='md'>
          <Burger opened={opened} onClick={toggle} size='md' />
        </Group>
      )}
    </Flex>
  )
}

export default Header
