import { useThesesContext } from '../../providers/ThesesProvider/hooks'
import GanttChart from '../GanttChart/GanttChart'
import React, { useMemo, useState } from 'react'
import { formatDate, formatPresentationType, formatThesisType } from '../../utils/format'
import { ThesisStateColor, ThesisTypeColor } from '../../config/colors'
import ThesisPreviewModal from '../ThesisPreviewModal/ThesisPreviewModal'
import { IThesis, ThesisState } from '../../requests/responses/thesis'
import { Badge, Center, Group, Indicator, Pagination, Stack, Text } from '@mantine/core'
import ThesisStateBadge from '../ThesisStateBadge/ThesisStateBadge'
import { Presentation } from 'phosphor-react'
import { arrayUnique } from '../../utils/array'
import AvatarUserList from '../AvatarUserList/AvatarUserList'
import AvatarUser from '../AvatarUser/AvatarUser'
import LabeledItem from '../LabeledItem/LabeledItem'
import { IGanttChartDataElement } from '../GanttChart/context'

const ThesesGanttChart = () => {
  const { theses, page, setPage } = useThesesContext()

  const [openedThesis, setOpenedThesis] = useState<IThesis>()

  const currentTime = useMemo(() => Date.now(), [])

  const data = useMemo<IGanttChartDataElement[] | undefined>(() => {
    if (!theses) {
      return undefined
    }

    const result: IGanttChartDataElement[] = []

    for (const thesis of theses.content) {
      const getAdjustedStartDate = (state: ThesisState, startDate: Date) => {
        if (state === ThesisState.PROPOSAL && thesis.startDate) {
          // Overwrite start date if it's in the writing phase
          return new Date(Math.min(startDate.getTime(), new Date(thesis.startDate).getTime()))
        }

        if (state === ThesisState.WRITING && thesis.startDate) {
          // Use thesis start date if before writing phase has been started
          return new Date(thesis.startDate)
        }

        if (state === ThesisState.SUBMITTED && thesis.endDate) {
          // Use thesis end date if thesis was submitted after end date
          return new Date(Math.min(startDate.getTime(), new Date(thesis.endDate).getTime()))
        }

        return startDate
      }

      const getAdjustedEndDate = (state: ThesisState, startDate: Date, endDate: Date) => {
        if (state === ThesisState.PROPOSAL && state === thesis.state) {
          // Proposal phase should be at least 4 weeks long if not completed yet
          return new Date(Math.max(endDate.getTime(), startDate.getTime() + 3600 * 24 * 28 * 1000))
        } else if (state === ThesisState.PROPOSAL && thesis.startDate) {
          // End proposal phase when thesis is started
          return new Date(thesis.startDate)
        }

        if (state === ThesisState.WRITING && thesis.endDate) {
          // Writing phase should end at endDate if not completed yet
          if (thesis.state === ThesisState.WRITING) {
            return new Date(thesis.endDate)
          }

          return new Date(Math.min(endDate.getTime(), new Date(thesis.endDate).getTime()))
        }

        return endDate
      }

      const advisor = thesis.advisors[0]

      result.push({
        id: thesis.thesisId,
        groupId: advisor.userId,
        groupNode: <AvatarUser user={advisor} />,
        columns: [
          <AvatarUserList
            key='student'
            users={thesis.students}
            withUniversityId={false}
            size='xs'
            oneLine
          />,
          <Indicator
            key='title'
            color={ThesisTypeColor[thesis.type]}
            position='middle-start'
            pl={10}
            mx={5}
            zIndex={5}
            style={{ width: '100%' }}
          >
            <Text size='xs' truncate>
              {thesis.title}
            </Text>
          </Indicator>,
          <Text key='keywords' size='xs' truncate>
            {thesis.keywords.join(', ')}
          </Text>,
        ],
        timeline: thesis.states.map((state) => ({
          id: state.state,
          startDate: getAdjustedStartDate(state.state, new Date(state.startedAt)),
          endDate: getAdjustedEndDate(
            state.state,
            new Date(state.startedAt),
            new Date(state.endedAt),
          ),
          color: ThesisStateColor[state.state],
        })),
        events: thesis.presentations.map((presentation) => ({
          id: presentation.presentationId,
          icon: <Presentation />,
          time: new Date(presentation.scheduledAt),
        })),
      })
    }

    return result
  }, [theses])

  const visibleTypes: string[] = theses
    ? arrayUnique([...theses.content.map((thesis) => thesis.type)])
    : []

  const visibleStates: ThesisState[] = theses
    ? arrayUnique<ThesisState>([
        ...theses.content.reduce<ThesisState[]>(
          (prev, curr) => [
            ...prev,
            ...curr.states.filter((row) => row.startedAt !== row.endedAt).map((row) => row.state),
          ],
          [],
        ),
      ])
    : []

  const hasKeywordsColumn = !!theses?.content.some((thesis) => !!thesis.keywords.length)

  return (
    <Stack>
      <GanttChart
        columns={[
          { label: 'Student', width: '10rem' },
          { label: 'Title', width: '13rem' },
          ...(hasKeywordsColumn ? [{ label: 'Keywords', width: '10rem' }] : []),
        ]}
        data={data}
        minRange={[currentTime - 1000 * 3600 * 24 * 365 * 2, currentTime + 1000 * 3600 * 24 * 365]}
        rangeStorageKey='thesis-gantt-chart'
        itemPopover={(item, timeline, event) => {
          const thesis = theses?.content.find((row) => row.thesisId === item.id)

          if (!thesis) {
            return null
          }

          const presentation = thesis.presentations.find((row) => row.presentationId === event?.id)

          return (
            <Stack gap='md'>
              {timeline && (
                <Group>
                  <Text fw='bold' fz='sm'>
                    Dates for state:
                  </Text>
                  <ThesisStateBadge state={timeline.id as ThesisState} />
                  <Text fw='bold' fz='sm'>
                    {formatDate(timeline.startDate)} - {formatDate(timeline.endDate)}
                  </Text>
                </Group>
              )}
              {presentation && (
                <Text fw='bold' fz='sm'>
                  {formatPresentationType(presentation.type)} Presentation scheduled at{' '}
                  {formatDate(presentation.scheduledAt)}
                </Text>
              )}
              <LabeledItem label='Title' value={thesis.title} />
            </Stack>
          )
        }}
        onItemClick={(item) =>
          setOpenedThesis(theses?.content.find((thesis) => thesis.thesisId === item.id))
        }
      />
      {visibleStates.length > 0 && (
        <Center>
          <Group gap='xs'>
            <Text>Legend:</Text>
            {visibleTypes.map((type) => (
              <Badge key={type} color='gray'>
                <Indicator
                  color={ThesisTypeColor[type]}
                  position='middle-start'
                  offset={5}
                  pl={15}
                  zIndex={5}
                >
                  {formatThesisType(type)}
                </Indicator>
              </Badge>
            ))}
            {visibleStates.map((state) => (
              <ThesisStateBadge key={state} state={state} />
            ))}
          </Group>
        </Center>
      )}
      {theses && theses.totalPages > 1 && (
        <Center>
          <Pagination
            size='sm'
            total={theses?.totalPages || 0}
            value={page + 1}
            onChange={(newPage) => setPage(newPage - 1)}
          />
        </Center>
      )}
      <ThesisPreviewModal
        opened={!!openedThesis}
        onClose={() => setOpenedThesis(undefined)}
        thesis={openedThesis}
      />
    </Stack>
  )
}

export default ThesesGanttChart
