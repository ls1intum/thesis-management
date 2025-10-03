import {
  Stack,
  Title,
  Text,
  Flex,
  ScrollArea,
  Tabs,
  Group,
  CopyButton,
  TextInput,
  Tooltip,
  ActionIcon,
  Indicator,
  Divider,
  Grid,
  useMantineColorScheme,
} from '@mantine/core'
import { useIsSmallerBreakpoint, usePageTitle } from '../../hooks/theme'
import { GLOBAL_CONFIG } from '../../config/global'
import { CopyIcon, CheckIcon } from '@phosphor-icons/react'
import { useEffect, useRef, useState } from 'react'
import { ILightResearchGroup } from '../../requests/responses/researchGroup'
import { useAuthenticationContext, useUser } from '../../hooks/authentication'
import { Calendar } from '@mantine/dates'
import dayjs from 'dayjs'
import { PaginationResponse } from '../../requests/responses/pagination'
import { IPublishedPresentation } from '../../requests/responses/thesis'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import PresentationCard from '../ThesisPage/components/ThesisPresentationSection/components/PresentationCard'
import { CalendarXIcon } from '@phosphor-icons/react/dist/ssr'
import { useNavigate } from 'react-router'

const PresentationOverviewPage = () => {
  usePageTitle('Presentations')

  const [researchGroups, setResearchGroups] = useState<ILightResearchGroup[]>([])
  const [selectedGroup, setSelectedGroup] = useState<ILightResearchGroup | undefined>(undefined)

  const context = useAuthenticationContext()

  const isSmaller = useIsSmallerBreakpoint('md')
  const isSmallerLG = useIsSmallerBreakpoint('lg')

  const navigate = useNavigate()

  const user = useUser()

  const { colorScheme } = useMantineColorScheme()

  useEffect(() => {
    if (context.researchGroups.length > 0) {
      setResearchGroups(context.researchGroups)
      setSelectedGroup(context.researchGroups[0])
    }
  }, [context.researchGroups])

  const calendarUrl =
    GLOBAL_CONFIG.calendar_url ||
    `${GLOBAL_CONFIG.server_host}/api/v2/calendar/presentations${selectedGroup ? `/${selectedGroup.abbreviation}` : ''}`

  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const onWheel = (e: WheelEvent) => {
      if (scrollRef.current) {
        scrollRef.current.scrollTop += e.deltaY
        e.preventDefault()
      }
    }

    window.addEventListener('wheel', onWheel, { passive: false })
    return () => window.removeEventListener('wheel', onWheel)
  }, [])

  const [presentations, setPresentations] = useState<Map<string, IPublishedPresentation[]>>()

  useEffect(() => {
    return doRequest<PaginationResponse<IPublishedPresentation>>(
      `/v2/published-presentations`,
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          includeDrafts: true,
          ...(selectedGroup?.id ? { researchGroupId: selectedGroup.id } : {}),
        },
      },
      (res) => {
        if (res.ok) {
          const presentationsByDate = new Map<string, IPublishedPresentation[]>()
          res.data.content.forEach((presentation) => {
            const date = dayjs(presentation.scheduledAt).format('YYYY-MM-DD')
            if (!presentationsByDate.has(date)) {
              presentationsByDate.set(date, [])
            }
            presentationsByDate.get(date)!.push(presentation)
          })
          setPresentations(presentationsByDate)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [selectedGroup?.id])

  const scrollTo = (targetDate: string) => {
    if (!scrollRef.current) return

    // Format the date the same way you use as key (`date` is ISO string in your map)
    const selector = `[data-date="${dayjs(targetDate).format('YYYY-MM-DD')}"]`
    const el = scrollRef.current.querySelector(selector)

    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }

  return (
    <Stack h={'100%'} gap='md'>
      <Title>Presentations</Title>

      {researchGroups.length > 1 && (
        <Tabs
          value={selectedGroup?.abbreviation}
          onChange={(value) => {
            const group = researchGroups.find((g) => g.abbreviation === value)
            setSelectedGroup(group)
          }}
        >
          <Tabs.List>
            {researchGroups.map((group) => (
              <Tabs.Tab key={group.id} value={group.abbreviation}>
                {group.name}
              </Tabs.Tab>
            ))}
          </Tabs.List>
        </Tabs>
      )}
      <Group>
        <Text c='dimmed'>Subscribe to Calendar</Text>
        <div style={{ flexGrow: 1 }}>
          <CopyButton value={calendarUrl}>
            {({ copied, copy }) => (
              <TextInput
                value={calendarUrl}
                onChange={() => undefined}
                onClick={(e) => e.currentTarget.select()}
                rightSection={
                  <Tooltip label={copied ? 'Copied' : 'Copy'} withArrow position='right'>
                    <ActionIcon color={copied ? 'teal' : 'gray'} variant='subtle' onClick={copy}>
                      {copied ? <CheckIcon size={16} /> : <CopyIcon size={16} />}
                    </ActionIcon>
                  </Tooltip>
                }
              />
            )}
          </CopyButton>
        </div>
      </Group>
      <Flex h={{ md: '85%' }} w={'100%'} direction={{ base: 'column-reverse', md: 'row' }}>
        {presentations && presentations.size === 0 && (
          <Flex h={'100%'} w={'100%'} align={'center'} justify={'center'} direction={'column'}>
            <CalendarXIcon size={64} color={'gray'} />
            <Title order={4}>No Presentations Scheduled</Title>
            <Text ta='center'>There are currently no presentations planned for the future.</Text>
          </Flex>
        )}
        {!isSmaller ? (
          <ScrollArea
            h={'100%'}
            style={{ flex: 1, minWidth: 0 }}
            offsetScrollbars
            viewportRef={scrollRef}
            type='scroll'
          >
            <Stack h={'100%'}>
              {presentations &&
                Array.from(presentations.entries())
                  .sort(([dateA], [dateB]) => (dayjs(dateA).isAfter(dayjs(dateB)) ? 1 : -1))
                  .map(([date, list]) => (
                    <Grid key={`datesection-${date}`} h={'fit-content'} data-date={date}>
                      <Grid.Col span='content'>
                        <Stack
                          h={'100%'}
                          bg={colorScheme === 'dark' ? 'dark.6' : 'gray.1'}
                          p='xs'
                          style={{ borderRadius: 8 }}
                          w={'100px'}
                          gap={5}
                        >
                          <Title order={5}>{dayjs(date).format('MMM D')}</Title>
                          <Text c='dimmed'>{dayjs(date).format('dddd')}</Text>
                        </Stack>
                      </Grid.Col>
                      <Grid.Col span='auto'>
                        <Stack>
                          {list.map((p) => (
                            <PresentationCard
                              key={p.presentationId}
                              presentation={p}
                              thesis={p.thesis}
                              hasEditAccess={
                                user?.groups.includes('admin') ||
                                user?.researchGroupId === p.thesis.researchGroup.id ||
                                p.thesis.students.some((student) => student.userId === user?.userId)
                              }
                              hasAcceptAccess={
                                user?.groups.includes('admin') ||
                                user?.researchGroupId === p.thesis.researchGroup.id
                              }
                              thesisName={p.thesis.title}
                              titleOrder={6}
                              includeStudents={true}
                              includeThesisStatus={true}
                              onClick={() => navigate(`/presentations/${p.presentationId}`)}
                            />
                          ))}
                        </Stack>
                      </Grid.Col>
                    </Grid>
                  ))}
            </Stack>
          </ScrollArea>
        ) : (
          <ScrollArea
            h={'100%'}
            style={{ flex: 1, minWidth: 0 }}
            offsetScrollbars
            viewportRef={scrollRef}
            type='scroll'
          >
            <Stack h={'100%'}>
              {presentations &&
                Array.from(presentations.entries())
                  .sort(([dateA], [dateB]) => (dayjs(dateA).isAfter(dayjs(dateB)) ? 1 : -1))
                  .map(([date, list]) => (
                    <Stack key={`datesection-${date}`}>
                      <Divider label={dayjs(date).format('MMM D')} data-date={date} />
                      <Grid key={`datesection-${date}`} h={'fit-content'}>
                        <Grid.Col span='auto'>
                          <Stack>
                            {list.map((p) => (
                              <PresentationCard
                                key={p.presentationId}
                                presentation={p}
                                thesis={p.thesis}
                                hasEditAccess={
                                  user?.groups.includes('admin') ||
                                  user?.researchGroupId === p.thesis.researchGroup.id ||
                                  p.thesis.students.some(
                                    (student) => student.userId === user?.userId,
                                  )
                                }
                                hasAcceptAccess={
                                  user?.groups.includes('admin') ||
                                  user?.researchGroupId === p.thesis.researchGroup.id
                                }
                                thesisName={p.thesis.title}
                                includeThesisStatus={true}
                                titleOrder={6}
                                includeStudents={true}
                                onClick={() => navigate(`/presentations/${p.presentationId}`)}
                              />
                            ))}
                          </Stack>
                        </Grid.Col>
                      </Grid>
                    </Stack>
                  ))}
            </Stack>
          </ScrollArea>
        )}
        <Flex
          h={{ base: 'fit-content', md: '100%' }}
          w={{ base: '100%', md: '33%' }}
          style={{ minWidth: 'fit-content', overflow: 'unset' }}
          direction={{ base: 'column-reverse', md: 'row' }}
        >
          {!isSmaller && (
            <Divider
              orientation={isSmaller ? 'horizontal' : 'vertical'}
              h={{ base: '1px', md: '100%' }}
              w={{ base: '100%', md: '1px' }}
              pb={{ base: 'sm', md: 0 }}
            />
          )}
          <Flex
            h={'100%'}
            w={'100%'}
            align={{ base: 'center', md: 'flex-start' }}
            px={{ base: 0, md: 'xs' }}
            py={{ base: 'sm', md: 0 }}
            style={{ minWidth: 'fit-content' }}
            justify={'center'}
          >
            <Calendar
              size={isSmallerLG ? 'sm' : isSmaller ? 'md' : 'lg'}
              pb={{ base: 'xs', md: 0 }}
              renderDay={(date) => {
                const day = dayjs(date).date()
                return (
                  <Flex align='center' gap={3} direction='column' onClick={() => scrollTo(date)}>
                    <Text size='xl'>{day}</Text>
                    <Group gap={8}>
                      {presentations?.has(dayjs(date).format('YYYY-MM-DD')) && (
                        <Indicator color={'blue'} size={5} />
                      )}
                    </Group>
                  </Flex>
                )
              }}
            />
          </Flex>
        </Flex>
      </Flex>
    </Stack>
  )
}

export default PresentationOverviewPage
