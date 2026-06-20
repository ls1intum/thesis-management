import type { PropsWithChildren } from 'react'
import React, { useEffect, useMemo, useState } from 'react'
import type { IThesis } from '@/thesis/requests/responses/thesis'
import { useThesis } from '@/core/hooks/fetcher'
import type { IThesisContext } from '@/thesis/providers/ThesisProvider/context'
import { ThesisContext } from '@/thesis/providers/ThesisProvider/context'
import NotFound from '@/core/components/NotFound/NotFound'
import PageLoader from '@/core/components/PageLoader/PageLoader'
import { useThesisAccess } from '@/thesis/providers/ThesisProvider/hooks'

interface IThesisProviderProps {
  thesisId: string | undefined
  requireLoadedThesis?: boolean
}

const ThesisProvider = (props: PropsWithChildren<IThesisProviderProps>) => {
  const { children, thesisId, requireLoadedThesis = false } = props

  const loadedThesis = useThesis(thesisId)

  const [thesis, setThesis] = useState<IThesis | undefined | false>(loadedThesis)

  useEffect(() => {
    setThesis(loadedThesis)
  }, [loadedThesis])

  const access = useThesisAccess(thesis)

  const contextState = useMemo<IThesisContext>(() => {
    return {
      thesis,
      updateThesis: (newThesis: IThesis) => {
        if (newThesis.thesisId !== thesisId) {
          return
        }

        setThesis(newThesis)
      },
      access,
    }
  }, [thesis, thesisId, access])

  if (requireLoadedThesis) {
    if (thesis === false) {
      return <NotFound />
    }

    if (thesis === undefined) {
      return <PageLoader />
    }
  }

  return (
    <ThesisContext value={contextState} key={thesisId}>
      {children}
    </ThesisContext>
  )
}

export default ThesisProvider
