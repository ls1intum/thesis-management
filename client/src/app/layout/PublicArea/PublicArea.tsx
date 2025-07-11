import { PropsWithChildren } from 'react'
import Footer from '../../../components/Footer/Footer'
import {
  AppShell,
  Box,
  Container,
  Divider,
  Flex,
  MantineSize,
  Stack,
  useComputedColorScheme,
} from '@mantine/core'
import ScrollToTop from '../ScrollToTop/ScrollToTop'
import Header from '../../../components/Header/Header'

interface IPublicAreaProps {
  size?: MantineSize
}

const PublicArea = (props: PropsWithChildren<IPublicAreaProps>) => {
  const { size = 'md', children } = props

  const HEADER_HEIGHT = 50
  const FOOTER_HEIGHT = 50

  return (
    <AppShell header={{ height: HEADER_HEIGHT }}>
      <AppShell.Header>
        <Container size={size} fluid={!size} h='100%'>
          <Header authenticatedArea={false}></Header>
        </Container>
      </AppShell.Header>

      <AppShell.Main>
        <Box h={`calc(100vh - ${HEADER_HEIGHT}px)`}>
          <Flex direction='column' h='100%' w='100%'>
            <Box flex={1}>
              <Container
                size={size}
                fluid={!size}
                px={{ base: 20, sm: 40 }}
                py={{ base: 10, sm: 20 }}
                h='100%'
              >
                {children}
              </Container>
              <ScrollToTop />
            </Box>

            <Stack style={{ flexShrink: 0 }} gap={0} w='100%'>
              <Divider />
              <Box
                h={`${FOOTER_HEIGHT}px`}
                mih={'fit-content'}
                style={{
                  flexShrink: 0,
                }}
              >
                <Footer size={size} />
              </Box>
            </Stack>
          </Flex>
        </Box>
      </AppShell.Main>
    </AppShell>
  )
}

export default PublicArea
