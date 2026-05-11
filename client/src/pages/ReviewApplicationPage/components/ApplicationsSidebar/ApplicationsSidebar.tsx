import { Center, Pagination, Stack, Text } from '@mantine/core'
import ApplicationsFilters from '../../../../components/ApplicationsFilters/ApplicationsFilters'
import React, { useEffect, useRef, useState } from 'react'
import { shouldIgnoreArrowKey } from './keyNavigationFilter'
import type { IApplication } from '../../../../requests/responses/application'
import { useApplicationsContext } from '../../../../providers/ApplicationsProvider/hooks'
import ApplicationListItem from '../ApplicationListItem/ApplicationListItem'

interface IApplicationsSidebarProps {
  selected: IApplication | undefined
  isSmallScreen: boolean
  onSelect: (application: IApplication) => unknown
}

const ApplicationsSidebar = (props: IApplicationsSidebarProps) => {
  const { selected, isSmallScreen, onSelect } = props

  const { page, setPage, applications } = useApplicationsContext()

  const selectedIndex =
    (applications?.content ?? []).findIndex((x) => x.applicationId === selected?.applicationId) ??
    -1

  const [startAtLastApplication, setStartAtLastApplication] = useState(false)

  // Stash the latest mutable handlers in refs so the keydown listener
  // always reads the current value without re-binding on every render
  // (onSelect is an inline function from the parent, so its identity
  // changes constantly).
  const onSelectRef = useRef(onSelect)
  const setPageRef = useRef(setPage)
  const stateRef = useRef({ page, selectedIndex, applications })

  useEffect(() => {
    onSelectRef.current = onSelect
    setPageRef.current = setPage
    stateRef.current = { page, selectedIndex, applications }
  })

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') {
        return
      }
      if (shouldIgnoreArrowKey(e)) {
        return
      }

      const {
        page: currentPage,
        selectedIndex: currentIndex,
        applications: apps,
      } = stateRef.current
      let newIndex = currentIndex

      newIndex += e.key === 'ArrowRight' ? 1 : 0
      newIndex += e.key === 'ArrowLeft' ? -1 : 0

      if (newIndex === currentIndex) {
        return
      }

      if (apps && newIndex < 0) {
        // start at last application if user navigates to a previous page with arrow keys
        setStartAtLastApplication(currentPage > 0)
        setPageRef.current(currentPage > 0 ? currentPage - 1 : 0)
      }

      if (apps && newIndex >= (apps.content ?? []).length && !apps.last) {
        // make sure that state is reset when navigating to next page with arrow keys
        setStartAtLastApplication(false)
        setPageRef.current(currentPage + 1)
      }

      if ((apps?.content ?? [])[newIndex]) {
        onSelectRef.current((apps?.content ?? [])[newIndex])
      }
    }

    window.addEventListener('keydown', onKeyDown)

    return () => {
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [])

  const applicationIdsKey = (applications?.content ?? []).map((x) => x.applicationId).join(',')

  useEffect(() => {
    if (isSmallScreen) {
      return
    }

    if (page === 0 && !startAtLastApplication) {
      return
    }

    if (applications) {
      onSelect(
        startAtLastApplication
          ? (applications.content ?? [])[(applications.content ?? []).length - 1]
          : (applications.content ?? [])[0],
      )
    }
    // eslint-disable-next-line @eslint-react/exhaustive-deps -- onSelect is recreated by the parent each render; applications identity is tracked via applicationIdsKey
  }, [page, startAtLastApplication, isSmallScreen, applicationIdsKey])

  return (
    <Stack gap='sm'>
      <ApplicationsFilters size='sm' />
      {applications && (applications.content ?? []).length === 0 && (
        <Text ta='center' fw='bold' my='md'>
          No applications found
        </Text>
      )}
      {(applications?.content ?? []).map((application) => (
        <ApplicationListItem
          key={application.applicationId}
          selected={application.applicationId === selected?.applicationId}
          application={application}
          onClick={() => onSelect(application)}
        />
      ))}
      <Text c='dimmed' ta='center' size='xs'>
        Tip: Navigate with arrow left and right keys
      </Text>
      <Center>
        <Pagination
          size='sm'
          total={applications?.totalPages ?? 0}
          value={page + 1}
          onChange={(newPage) => setPage(newPage - 1)}
        />
      </Center>
    </Stack>
  )
}

export default ApplicationsSidebar
