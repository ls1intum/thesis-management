import { PropsWithChildren } from 'react'
import { Container, MantineSize } from '@mantine/core'

interface IContentContainerProps {
  size?: MantineSize
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
