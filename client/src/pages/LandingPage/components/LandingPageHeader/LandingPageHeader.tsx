import { Card, Title, Flex, useComputedColorScheme } from '@mantine/core'
import LogoCircle from './LogoCircle'

const LandingPageHeader = () => {
  const computedColorScheme = useComputedColorScheme()

  return (
    <Card
      radius='md'
      bg={computedColorScheme === 'dark' ? 'dark.6' : 'gray.1'}
      p='xl'
      style={{ flexShrink: 0 }}
    >
      <Flex justify='flex-start' align='center' gap='xl' wrap='nowrap'>
        <Flex direction='column' gap='xs' flex={1}>
          <Flex justify='space-between' align='flex-start' gap='xs'>
            <Title order={2}>Find a Thesis Topic</Title>
            <LogoCircle size={40} logoSize={30} hiddenFrom='sm' />
          </Flex>
          <Title order={5} c='dimmed'>
            Whether you&apos;re looking for inspiration or have a unique idea in mind, Thesis
            Management makes it easy. Explore topics posted by instructors or suggest your own.
          </Title>
        </Flex>

        <LogoCircle size={100} logoSize={80} visibleFrom='sm' />
      </Flex>
    </Card>
  )
}

export default LandingPageHeader
