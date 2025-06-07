import { PropsWithChildren, ReactNode } from 'react'
import Footer from '../../../components/Footer/Footer'
import { AppShell, Box, Container, Flex, MantineSize } from '@mantine/core'
import ScrollToTop from '../ScrollToTop/ScrollToTop'
import Header from '../../../components/Header/Header'

interface IPublicAreaProps {
  size?: MantineSize
  withBackButton?: boolean
}

const PublicArea = (props: PropsWithChildren<IPublicAreaProps>) => {
  const { size = 'md', children } = props

  const HEADER_HEIGHT = 50
  const FOOTER_HEIGHT = 50

  return (
    <AppShell header={{ height: HEADER_HEIGHT }}>
      <AppShell.Header>
        <Container size={size} fluid={!size} h='100%' p={0}>
          <Header size={size}></Header>
        </Container>
      </AppShell.Header>

      <AppShell.Main>
        <Box h={`calc(100vh - ${HEADER_HEIGHT}px)`}>
          <Flex direction='column' h='100%' w='100%'>
            <Box flex={1}>
              <Container size={size} fluid={!size} px={30} py={20} h='100%'>
                {children}
              </Container>
              <ScrollToTop />
            </Box>

            <Box
              h={`${FOOTER_HEIGHT}px`}
              style={{ borderTop: '1px solid var(--mantine-color-gray-3)' }}
            >
              <Container fluid={!size} size={size} h='100%' p={0}>
                <Footer size={size} />
              </Container>
            </Box>
          </Flex>
        </Box>
      </AppShell.Main>
    </AppShell>
  )
}

export default PublicArea
