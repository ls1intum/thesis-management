import { use } from 'react'
import { ApplicationsContext } from './context'
import type { IApplication } from '../../requests/responses/application'

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
