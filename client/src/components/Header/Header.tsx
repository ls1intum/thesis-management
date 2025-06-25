import { Button, Flex, Text } from '@mantine/core'
import Logo from '../Logo/Logo'
import { Link, useNavigate } from 'react-router'
import ColorSchemeToggleButton from '../ColorSchemeToggleButton/ColorSchemeToggleButton'
import { useUser } from '../../hooks/authentication'

const Header = () => {
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

      <Flex justify='space-between' align='center' h='100%' gap='sm'>
        <ColorSchemeToggleButton iconSize={25} />

        <Button component={Link} to='/dashboard'>
          Login
        </Button>
      </Flex>
    </Flex>
  )
}

export default Header
