import { Flex, Text } from '@mantine/core'
import Logo from '../Logo/Logo'
import { useNavigate } from 'react-router'
import ColorSchemeToggleButton from '../ColorSchemeToggleButton/ColorSchemeToggleButton'

const Header = () => {
  let navigate = useNavigate()

  return (
    <Flex justify='space-between' align='center' h='100%' w='100%'>
      <Flex
        gap={'xs'}
        justify='flex-start'
        align='center'
        h='100%'
        style={{ cursor: 'pointer' }}
        onClick={() => navigate('/')}
      >
        <Logo size={40} />
        <Text fw='bold' visibleFrom='sm' pt='2px'>
          Thesis Management
        </Text>
      </Flex>

      <Flex>
        <ColorSchemeToggleButton />
      </Flex>
    </Flex>
  )
}

export default Header
