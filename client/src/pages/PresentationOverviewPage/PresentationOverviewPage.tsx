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
  Select,
} from '@mantine/core'
import { useIsSmallerBreakpoint, usePageTitle } from '../../hooks/theme'
import { GLOBAL_CONFIG } from '../../config/global'
import { CopyIcon, CheckIcon } from '@phosphor-icons/react'
import { useEffect, useRef, useState } from 'react'
import type { ILightResearchGroup } from '../../requests/responses/researchGroup'
import { useAuthenticationContext, useUser } from '../../hooks/authentication'
import { Calendar } from '@mantine/dates'
import dayjs from 'dayjs'
import type { PaginationResponse } from '../../requests/responses/pagination'
import type { IPublishedPresentation, IThesisPresentation } from '../../requests/responses/thesis'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import PresentationCard from '../ThesisPage/components/ThesisPresentationSection/components/PresentationCard'
import { CalendarXIcon } from '@phosphor-icons/react/dist/ssr'
import { useNavigate } from 'react-router'
import { pickTargetDate } from './pickTargetDate'

const NEVER_SCROLLED = Symbol('never-scrolled')

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

  const calendarUrl = `${GLOBAL_CONFIG.server_host}/api/v2/calendar/presentations${selectedGroup ? `/${selectedGroup.abbreviation}` : ''}`

  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    // On mobile let the page scroll natively; the custom hijack causes the
    // calendar to slide under the sticky header (#729).
    if (isSmaller) {
      return
    }

    const onWheel = (e: WheelEvent) => {
      // Check if body has modal-open class or if any modal backdrop is present
      const hasModalBackdrop = document.querySelector(
        '.mantine-Modal-overlay, .mantine-Drawer-overlay, [data-mantine-modal]',
      )
      const bodyHasModalOpen = document.body.style.overflow === 'hidden'

      // Only handle wheel events if no modal is open and we have scrollRef
      if (!hasModalBackdrop && !bodyHasModalOpen && scrollRef.current) {
        scrollRef.current.scrollTop += e.deltaY
        e.preventDefault()
      }
    }

    window.addEventListener('wheel', onWheel, { passive: false })
    return () => window.removeEventListener('wheel', onWheel)
  }, [isSmaller])

  const [presentations, setPresentations] = useState<Map<string, IPublishedPresentation[]>>()
  // Tracks which research group the current `presentations` map belongs to,
  // so the auto-scroll effect doesn't fire with the *previous* group's data
  // during the gap between groupId change and fetch resolution.
  const presentationsGroupId = useRef<string | null>(null)

  useEffect(() => {
    // Clear the stale map immediately on group switch — otherwise the auto
    // scroll effect would compute its target from the previous group's data
    // and mark the new group as "scrolled" before its presentations arrive.
    setPresentations(undefined)
    presentationsGroupId.current = null
    const newGroupId = selectedGroup?.id ?? null
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
          ;(res.data.content ?? []).forEach((presentation) => {
            const date = dayjs(presentation.scheduledAt).format('YYYY-MM-DD')
            if (!presentationsByDate.has(date)) {
              presentationsByDate.set(date, [])
            }
            presentationsByDate.get(date)!.push(presentation)
          })
          presentationsGroupId.current = newGroupId
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

  // Auto-scroll to today's date heading once after presentations load for
  // a given research group. If today has no presentation, fall back to the
  // next upcoming day, then to the most recent past date — never the
  // earliest, which would put the user furthest from where they care to be.
  //
  // The ref tracks which groupId has already been scroll-targeted so user
  // edits via onDelete / onUpdate (which produce new presentations Map
  // identities) do not yank the viewport back to today while the user is
  // interacting with a card.
  //
  // Initial value uses a unique sentinel rather than `null` so we can tell
  // apart "no scroll yet" from "scrolled for a user with no research group"
  // — otherwise the first effect run can lock the ref to `null` *before*
  // `selectedGroup` resolves, and the eventual real group never gets a
  // scroll.
  const lastScrolledGroupId = useRef<string | typeof NEVER_SCROLLED | null>(NEVER_SCROLLED)

  useEffect(() => {
    if (!presentations || presentations.size === 0 || !scrollRef.current) {
      return
    }
    // If we have research groups available but none picked yet, wait —
    // selectedGroup is about to resolve and we'd otherwise consume the
    // scroll for a transient `null` group key.
    if (researchGroups.length > 0 && !selectedGroup) {
      return
    }
    const groupKey = selectedGroup?.id ?? null
    // Race guard: between a group switch and the new fetch resolving, the
    // `presentations` map still belongs to the previous group. Comparing
    // against the ref written by the fetch callback ensures we only scroll
    // when the rendered data actually matches the currently selected group.
    if (presentationsGroupId.current !== groupKey) {
      return
    }
    if (lastScrolledGroupId.current === groupKey) {
      return
    }

    const today = dayjs().format('YYYY-MM-DD')
    const target = pickTargetDate(today, Array.from(presentations.keys()))

    if (target) {
      // Defer to the next frame so layout has had a chance to commit the
      // newly-rendered presentation cards before we measure scroll offsets,
      // and only mark the group as "scrolled" once the scroll actually
      // fires — otherwise a stale render that bails out mid-effect could
      // still consume the scroll.
      const handle = requestAnimationFrame(() => {
        scrollTo(target)
        lastScrolledGroupId.current = groupKey
      })
      return () => cancelAnimationFrame(handle)
    }
    lastScrolledGroupId.current = groupKey
  }, [presentations, selectedGroup?.id, researchGroups.length])

  const onDelete = (presentationId: string, date: string) => {
    const updatedMap = new Map(presentations)
    const updatedList =
      updatedMap.get(date)?.filter((item) => item.presentationId !== presentationId) || []
    if (updatedList.length === 0) {
      updatedMap.delete(date)
    } else {
      updatedMap.set(date, updatedList)
    }
    setPresentations(updatedMap)
  }

  const onUpdate = (updated: IPublishedPresentation | IThesisPresentation) => {
    if ('presentationNoteHtml' in updated) {
      return
    } else {
      const updatedMap = new Map(presentations)
      const date = dayjs(updated.scheduledAt).format('YYYY-MM-DD')

      //Check if the Date changed
      updatedMap.forEach((list, key) => {
        if (list.find((p) => p.presentationId === updated.presentationId) && key !== date) {
          const newList = list.filter((p) => p.presentationId !== updated.presentationId)
          if (newList.length === 0) {
            updatedMap.delete(key)
          } else {
            updatedMap.set(key, newList)
          }
        } else if (key === date) {
          const index = list.findIndex((p) => p.presentationId === updated.presentationId)
          if (index !== -1) {
            list[index] = updated
          } else {
            list.push(updated)
          }
          list.sort((a, b) => (dayjs(a.scheduledAt).isAfter(dayjs(b.scheduledAt)) ? 1 : -1))
          updatedMap.set(key, list)
        }
      })

      if (!updatedMap.has(date)) {
        updatedMap.set(date, [updated])
      }

      setPresentations(updatedMap)
    }
  }

  return (
    <Stack h={'100%'} gap='md'>
      <Title>Presentations</Title>

      {researchGroups.length > 5 && (
        <Select
          value={selectedGroup?.name}
          placeholder='Pick Research Group'
          data={researchGroups.map((group) => group.name)}
          searchable
          onChange={(value) => {
            const group = researchGroups.find((g) => g.name === value)
            setSelectedGroup(group)
          }}
        />
      )}

      {researchGroups.length > 1 && researchGroups.length < 6 && (
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
        {presentations?.size === 0 && (
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
                                user?.groups?.includes('admin') ||
                                user?.researchGroupId === p.thesis.researchGroup.id ||
                                (p.thesis.students ?? []).some(
                                  (student) => student.userId === user?.userId,
                                )
                              }
                              hasAcceptAccess={
                                user?.groups?.includes('admin') ||
                                user?.researchGroupId === p.thesis.researchGroup.id
                              }
                              thesisName={p.thesis.title}
                              titleOrder={6}
                              includeStudents={true}
                              includeThesisStatus={true}
                              onClick={() => navigate(`/presentations/${p.presentationId}`)}
                              onDelete={() => {
                                onDelete(p.presentationId, date)
                              }}
                              onChange={(updated) => {
                                onUpdate(updated)
                              }}
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
                                  user?.groups?.includes('admin') ||
                                  user?.researchGroupId === p.thesis.researchGroup.id ||
                                  (p.thesis.students ?? []).some(
                                    (student) => student.userId === user?.userId,
                                  )
                                }
                                hasAcceptAccess={
                                  user?.groups?.includes('admin') ||
                                  user?.researchGroupId === p.thesis.researchGroup.id
                                }
                                thesisName={p.thesis.title}
                                includeThesisStatus={true}
                                titleOrder={6}
                                includeStudents={true}
                                onClick={() => navigate(`/presentations/${p.presentationId}`)}
                                onDelete={() => {
                                  onDelete(p.presentationId, date)
                                }}
                                onChange={(updated) => {
                                  onUpdate(updated)
                                }}
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
