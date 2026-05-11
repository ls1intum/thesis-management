import type { PropsWithChildren } from 'react'
import type { MantineSize } from '@mantine/core'
import { Container } from '@mantine/core'

interface IContentContainerProps {
  size?: number | MantineSize | (string & {}) | undefined
}

const ContentContainer = (props: PropsWithChildren<IContentContainerProps>) => {
  const { size, children } = props

  return (
    <Container size={size} fluid={!size} h={'100%'}>
      {children}
    </Container>
  )
}

export default ContentContainer
