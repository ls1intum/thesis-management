import { Anchor, Container, Flex, MantineSize } from '@mantine/core'
import { GLOBAL_CONFIG } from '../../config/global'
import { Link } from 'react-router'
import packageJson from '../../../package.json'

const links = [
  { link: '/about', label: 'About', visible: true },
  { link: '/imprint', label: 'Imprint', visible: true },
  { link: '/privacy', label: 'Privacy', visible: true },
]

interface IFooterProps {
  size?: MantineSize
}

const Footer = (props: IFooterProps) => {
  const { size } = props

  const version = packageJson.version

  return (
    <Container fluid={!size} size={size} h='100%'>
      <Flex
        justify='space-between'
        align='center'
        h='100%'
        gap={{ base: 'xs', xs: 'md' }}
        direction={{ base: 'column', sm: 'row' }}
        py={'xs'}
      >
        <Flex
          gap={{ base: 'xs', xs: 'sm' }}
          direction={{ base: 'column', xs: 'row' }}
          justify='center'
          align='center'
        >
          <Anchor href={GLOBAL_CONFIG.chair_url} target='_blank' c='dimmed'>
            {GLOBAL_CONFIG.chair_name}
          </Anchor>
          <Anchor
            href={`https://github.com/ls1intum/thesis-management/releases`}
            target='_blank'
            c='dimmed'
          >
            v{version}
          </Anchor>
        </Flex>
        <Flex
          gap={{ base: 'xs', xs: 'sm' }}
          direction={{ base: 'column', xs: 'row' }}
          justify='center'
          align='center'
        >
          {links
            .filter((link) => link.visible)
            .map((link) => (
              <Anchor key={link.label} component={Link} c='dimmed' to={link.link}>
                {link.label}
              </Anchor>
            ))}
        </Flex>
      </Flex>
    </Container>
  )
}

export default Footer
