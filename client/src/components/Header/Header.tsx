import { Burger, Button, Flex, Group, Text } from '@mantine/core'
import Logo from '../Logo/Logo'
import { Link } from 'react-router'
import ColorSchemeToggleButton from '../ColorSchemeToggleButton/ColorSchemeToggleButton'
import { useUser } from '../../hooks/authentication'
import CustomAvatar from '../CustomAvatar/CustomAvatar'

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
        gap='sm'
        visibleFrom={authenticatedArea ? 'md' : undefined}
      >
        <ColorSchemeToggleButton iconSize={'70%'} />
        {user ? (
          <Link to='/dashboard'>
            <CustomAvatar user={user} size={30} />
          </Link>
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
