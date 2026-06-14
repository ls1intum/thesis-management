import { use } from 'react'
import { ApplicationsContext } from '@/core/application/providers/ApplicationsProvider/context'
import type { IApplication } from '@/core/application/requests/responses/application'

export function useApplicationsContext() {
  const data = use(ApplicationsContext)

  if (!data) {
    throw new Error('ApplicationsContext not initialized')
  }

  return data
}

export function useApplicationsContextUpdater(): (application: IApplication) => unknown {
  const data = use(ApplicationsContext)

  if (!data) {
    return () => undefined
  }

  return data.updateApplication
}
