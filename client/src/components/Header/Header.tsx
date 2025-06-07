import { Flex, Group, MantineSize, Text } from '@mantine/core'
import Logo from '../Logo/Logo'
import { useNavigate } from 'react-router'

interface IHeaderProps {
  size?: MantineSize
}

const Header = (props: IHeaderProps) => {
  const { size } = props
  let navigate = useNavigate()

  return (
    <Flex justify='space-between' align='center' h='100%' w='100%'>
      <Group preventGrowOverflow={false} gap={'xs'}>
        <Logo />
        <Text fw='bold' style={{ cursor: 'pointer' }} onClick={() => navigate('/')}>
          Thesis Management
        </Text>
      </Group>
    </Flex>
  )
}

export default Header
