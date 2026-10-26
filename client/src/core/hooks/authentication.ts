import { use } from 'react'
import { AuthenticationContext } from '@/core/providers/AuthenticationContext/context'

export function useAuthenticationContext() {
  const context = use(AuthenticationContext)

  if (!context) {
    throw new Error('Authentication context not initialized')
  }

  return context
}

export function useUser() {
  const context = useAuthenticationContext()

  return context.user
}

export function useLoggedInUser() {
  const user = useUser()
  const auth = useAuthenticationContext()

  if (!user) {
    auth.login()

    throw new Error('Authentication required')
  }

  return user
}

export function useHasGroupAccess(...groups: string[]) {
  const user = useUser()

  return user?.groups?.some((group) => groups.includes(group)) ?? false
}

export function useManagementAccess() {
  return useHasGroupAccess('admin', 'supervisor', 'advisor')
}
