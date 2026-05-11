import type { ComponentType, PropsWithChildren } from 'react'
import { Suspense, useEffect, useRef } from 'react'
import type { MantineSize } from '@mantine/core'
import {
  ActionIcon,
  AppShell,
  Box,
  Center,
  Container,
  Divider,
  Flex,
  Group,
  Stack,
  Text,
  Tooltip,
} from '@mantine/core'
import * as classes from './AuthenticatedArea.module.css'
import { Link, useLocation, useNavigationType } from 'react-router'
import { useDebouncedValue, useDisclosure } from '@mantine/hooks'
import {
  CaretDoubleLeftIcon,
  CaretDoubleRightIcon,
  ChatsCircleIcon,
  FolderSimplePlusIcon,
  GearIcon,
  GearSixIcon,
  KanbanIcon,
  NewspaperClippingIcon,
  PaperPlaneTiltIcon,
  PresentationIcon,
  ScrollIcon,
  SignOutIcon,
  TableIcon,
  UsersThreeIcon,
} from '@phosphor-icons/react'
import { useAuthenticationContext, useUser } from '../../../hooks/authentication'
import ScrollToTop from '../ScrollToTop/ScrollToTop'
import PageLoader from '../../../components/PageLoader/PageLoader'
import { useLocalStorage } from '../../../hooks/local-storage'
import { CustomAvatar } from '../../../components/CustomAvatar/CustomAvatar'
import { formatUser } from '../../../utils/format'
import ContentContainer from '../ContentContainer/ContentContainer'
import Footer from '../../../components/Footer/Footer'
import EnvironmentBanner, {
  ENVIRONMENT_BANNER_HEIGHT,
  isEnvironmentBannerVisible,
} from '../../../components/EnvironmentBanner/EnvironmentBanner'
import Header from '../../../components/Header/Header'
import { useIsSmallerBreakpoint } from '../../../hooks/theme'

export interface IAuthenticatedAreaProps {
  size?: MantineSize
  collapseNavigation?: boolean
  requiredGroups?: string[]
  handleScrollInView?: boolean
}
const AuthenticatedArea = (props: PropsWithChildren<IAuthenticatedAreaProps>) => {
  const { children, size, collapseNavigation = false, requiredGroups, handleScrollInView } = props

  const links: Array<{
    link: string
    label: string
    icon: ComponentType<{ size?: number | string; className?: string }>
    groups: string[] | undefined
    hideFromGroups?: string[]
    display?: boolean
  }> = [
    { link: '/dashboard', label: 'Dashboard', icon: NewspaperClippingIcon, groups: undefined },
    {
      link: '/presentations',
      label: 'Presentations',
      icon: PresentationIcon,
      groups: undefined,
      display: useAuthenticationContext().researchGroups.length > 0,
    },
    {
      link: '/submit-application',
      label: 'Submit Application',
      icon: PaperPlaneTiltIcon,
      groups: undefined,
      hideFromGroups: ['advisor', 'supervisor'],
    },
    {
      link: '/applications',
      label: 'Review Applications',
      icon: ScrollIcon,
      groups: ['admin', 'advisor', 'supervisor'],
    },
    {
      link: '/topics',
      label: 'Manage Topics',
      icon: FolderSimplePlusIcon,
      groups: ['admin', 'advisor', 'supervisor'],
    },
    {
      link: '/theses',
      label: 'Browse Theses',
      icon: TableIcon,
      groups: undefined,
    },
    {
      link: '/overview',
      label: 'Theses Overview',
      icon: KanbanIcon,
      groups: ['admin', 'advisor', 'supervisor'],
    },
    {
      link: '/research-groups',
      label: 'Research Groups',
      icon: UsersThreeIcon,
      groups: ['admin'],
    },
    {
      link: '/interviews',
      label: 'Interviews',
      icon: ChatsCircleIcon,
      groups: ['advisor', 'supervisor'],
    },
    {
      link: '/admin',
      label: 'Administration',
      icon: GearSixIcon,
      groups: ['admin'],
    },
  ]

  const user = useUser()
  const [opened, { toggle, close }] = useDisclosure()

  const minimizeAnimationDuration = 200
  const [minimizedState, setMinimized] = useLocalStorage<boolean>('navigation_minimized', {
    usingJson: true,
  })
  const [debouncedMinimized] = useDebouncedValue(
    collapseNavigation || minimizedState,
    minimizeAnimationDuration,
  )
  // only use debounced State if value is false because otherwise the text is formatted weirdly if you expand the navigation
  // eslint-disable-next-line @typescript-eslint/prefer-nullish-coalescing -- intentionally fall through on `false` so the debounced value is used while collapsing
  const minimized = opened ? false : minimizedState || Boolean(debouncedMinimized)

  const location = useLocation()
  const navigationType = useNavigationType()

  const auth = useAuthenticationContext()
  const isAuthenticated = auth.isAuthenticated
  const isPasskeySupported = auth.isPasskeySupported
  const login = auth.login
  const hasTriggeredFallbackLogin = useRef(false)

  const baseHeaderHeight = 50
  const HEADER_HEIGHT =
    baseHeaderHeight + (isEnvironmentBannerVisible() ? ENVIRONMENT_BANNER_HEIGHT : 0)
  const FOOTER_HEIGHT = 50

  const isSmallerBreakpoint = useIsSmallerBreakpoint('md')

  useEffect(() => {
    if (navigationType === 'POP') {
      return
    }

    close()
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- close is a stable disclosure handler; effect intentionally tracks navigation only
  }, [location.pathname, navigationType])

  useEffect(() => {
    if (isAuthenticated || isPasskeySupported) {
      hasTriggeredFallbackLogin.current = false
      return
    }

    if (location.pathname === '/logout' || hasTriggeredFallbackLogin.current) {
      return
    }

    hasTriggeredFallbackLogin.current = true
    void login(`${location.pathname}${location.search}${location.hash}`)
  }, [
    isAuthenticated,
    isPasskeySupported,
    login,
    location.hash,
    location.pathname,
    location.search,
  ])

  return (
    <AppShell
      header={{ height: HEADER_HEIGHT }}
      navbar={{
        width: collapseNavigation || minimizedState ? 70 : 250,
        breakpoint: 'md',
        collapsed: { mobile: !opened, desktop: false },
      }}
      styles={{
        navbar: {
          transition: `width ${minimizeAnimationDuration}ms ease-in-out`,
        },
      }}
      padding={0}
    >
      <AppShell.Header>
        <EnvironmentBanner />
        <Box h={baseHeaderHeight}>
          <Container size={size} fluid={!size} h='100%'>
            <Header
              opened={opened}
              toggle={toggle}
              authenticatedArea={true}
              openLoginModal={!isAuthenticated && location.pathname !== '/logout'}
            />
          </Container>
        </Box>
      </AppShell.Header>

      <AppShell.Navbar p='md'>
        <AppShell.Section grow mb='md'>
          {links
            .filter(
              (item) =>
                !item.groups || item.groups.some((role) => auth.user?.groups?.includes(role)),
            )
            .filter((item) => item.display === undefined || item.display)
            .filter((item) =>
              item.hideFromGroups
                ? !item.hideFromGroups.some((role) => auth.user?.groups?.includes(role))
                : true,
            )
            .map((item) => (
              <Link
                className={minimized ? classes.minimizedLink : classes.fullLink}
                data-active={location.pathname.startsWith(item.link) || undefined}
                key={item.label}
                to={item.link}
              >
                <Tooltip label={item.label} disabled={!minimized} position='right' offset={15}>
                  <item.icon className={classes.linkIcon} size={25} />
                </Tooltip>
                {!minimized && <span>{item.label}</span>}
              </Link>
            ))}
        </AppShell.Section>
        {user && (
          <AppShell.Section>
            {isSmallerBreakpoint && (
              <Link
                to='/settings'
                className={minimized ? classes.minimizedLink : classes.fullLink}
                data-active={location.pathname.startsWith('/settings') || undefined}
              >
                <Group gap={5} align='center'>
                  <CustomAvatar
                    user={user}
                    size={minimized ? 18 : 32}
                    className={classes.linkAvatar}
                  />
                  {!minimized && (
                    <Stack gap={2}>
                      <Text size='sm'>{formatUser(user)}</Text>
                      <Text size='xs'>Settings</Text>
                    </Stack>
                  )}
                </Group>
              </Link>
            )}
            {isSmallerBreakpoint && (
              <Link to='/logout' className={minimized ? classes.minimizedLink : classes.fullLink}>
                <Group gap={5} align='center'>
                  <Tooltip label='Logout' disabled={!minimized} position='right' offset={15}>
                    <SignOutIcon className={classes.linkIcon} size={25} />
                  </Tooltip>
                  {!minimized && <span>Logout</span>}
                </Group>
              </Link>
            )}

            <Group>
              {user.groups?.includes('group-admin') && (
                <Link
                  to={`/research-groups/${user.researchGroupId}`}
                  className={minimized ? classes.minimizedLink : classes.fullLink}
                  data-active={location.pathname.startsWith('/research-groups') || undefined}
                >
                  <Tooltip
                    label='Group Settings'
                    disabled={!minimized}
                    position='right'
                    offset={15}
                  >
                    <GearIcon className={classes.linkIcon} size={25} />
                  </Tooltip>
                  {!minimized && <span>Group Settings</span>}
                </Link>
              )}
              {!collapseNavigation && (
                <ActionIcon
                  visibleFrom='md'
                  ml='auto'
                  mr={minimized ? 'auto' : undefined}
                  variant='transparent'
                  onClick={() => setMinimized((prev) => !prev)}
                >
                  {minimized ? <CaretDoubleRightIcon /> : <CaretDoubleLeftIcon />}
                </ActionIcon>
              )}
            </Group>
          </AppShell.Section>
        )}
      </AppShell.Navbar>

      <AppShell.Main>
        <Box h={`calc(100vh - ${HEADER_HEIGHT}px)`}>
          <Flex direction='column' h='100%' w='100%'>
            <Box flex={1} style={{ overflow: handleScrollInView ? 'hidden' : undefined }}>
              <Container
                size={size}
                fluid={!size}
                px={{ base: 20, sm: 40 }}
                py={{ base: 10, sm: 20 }}
                h='100%'
              >
                {auth.user ? (
                  <Suspense fallback={<PageLoader />}>
                    {!requiredGroups ||
                    requiredGroups.some((role) => auth.user?.groups?.includes(role)) ? (
                      <ContentContainer size={size}>{children}</ContentContainer>
                    ) : (
                      <Center className={classes.fullHeight}>
                        <h1>403 - Unauthorized</h1>
                      </Center>
                    )}
                  </Suspense>
                ) : (
                  <PageLoader />
                )}
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

export default AuthenticatedArea
