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
} from '@mantine/core'
import { useIsSmallerBreakpoint, usePageTitle } from '../../hooks/theme'
import { GLOBAL_CONFIG } from '../../config/global'
import { CopyIcon, CheckIcon } from '@phosphor-icons/react'
import PublicPresentationsTable from '../../components/PublicPresentationsTable/PublicPresentationsTable'
import { useEffect, useRef, useState } from 'react'
import { ILightResearchGroup } from '../../requests/responses/researchGroup'
import { useAuthenticationContext } from '../../hooks/authentication'
import { Calendar } from '@mantine/dates'
import dayjs from 'dayjs'
import { PaginationResponse } from '../../requests/responses/pagination'
import { IPublishedPresentation } from '../../requests/responses/thesis'
import { doRequest } from '../../requests/request'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import PresentationCard from '../ThesisPage/components/ThesisPresentationSection/components/PresentationCard'

const PresentationOverviewPage = () => {
  usePageTitle('Presentations')

  const [researchGroups, setResearchGroups] = useState<ILightResearchGroup[]>([])
  const [selectedGroup, setSelectedGroup] = useState<ILightResearchGroup | undefined>(undefined)

  const context = useAuthenticationContext()

  const isSmaller = useIsSmallerBreakpoint('md')
  const isSmallerLG = useIsSmallerBreakpoint('lg')
  const isSmallerXS = useIsSmallerBreakpoint('xs')

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

  const [presentations, setPresentations] = useState<PaginationResponse<IPublishedPresentation>>()

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
          setPresentations(res.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
        }
      },
    )
  }, [selectedGroup?.id])

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
        {!isSmaller ? (
          <ScrollArea
            h={'100%'}
            style={{ flex: 1, minWidth: 0 }}
            offsetScrollbars
            viewportRef={scrollRef}
            type='scroll'
          >
            <Stack h={'100%'}>
              {presentations?.content.map((p) => (
                <PresentationCard
                  key={p.presentationId}
                  presentation={p}
                  thesis={p.thesis}
                  hasEditAccess={false}
                  thesisName={p.thesis.title}
                  titleOrder={6}
                />
              ))}
            </Stack>
          </ScrollArea>
        ) : (
          <Stack h={'100%'}>
            {presentations?.content.map((p) => (
              <PresentationCard
                key={p.presentationId}
                presentation={p}
                thesis={p.thesis}
                hasEditAccess={false}
                thesisName={p.thesis.title}
                titleOrder={6}
              />
            ))}
          </Stack>
        )}
        <Flex
          h={{ base: 'fit-content', md: '100%' }}
          w={{ base: '100%', md: '33%' }}
          style={{ minWidth: 'fit-content', overflow: 'unset' }}
          direction={{ base: 'column-reverse', md: 'row' }}
        >
          <Divider
            orientation={isSmaller ? 'horizontal' : 'vertical'}
            h={{ base: '1px', md: '100%' }}
            w={{ base: '100%', md: '1px' }}
            pb={{ base: 'sm', md: 0 }}
          />
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
              renderDay={(date) => {
                const day = dayjs(date).date()
                return (
                  <Flex align='center' gap={3} direction='column'>
                    <Text size='xl'>{day}</Text>
                    <Group gap={8}>
                      {presentations?.content.find((p) =>
                        dayjs(p.scheduledAt).isSame(dayjs(date), 'date'),
                      ) && <Indicator color={'blue'} size={5} />}
                    </Group>
                  </Flex>
                )
              }}
            />
          </Flex>
        </Flex>
      </Flex>

      {/*
      <PublicPresentationsTable includeDrafts={true} researchGroupId={selectedGroup?.id} />
      */}
    </Stack>
  )
}

export default PresentationOverviewPage
