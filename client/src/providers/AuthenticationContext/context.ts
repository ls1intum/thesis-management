import { createContext } from 'react'
import type { JwtPayload } from 'jwt-decode'
import type { IUser } from '../../requests/responses/user'
import type { IUpdateUserInformationPayload } from '../../requests/payloads/user'
import type { PartialNull } from '../../utils/validation'
import type { ILightResearchGroup } from '../../requests/responses/researchGroup'

export interface IKeycloakCredential {
  id: string
  type?: string
  userLabel?: string
  createdDate?: number
}

export interface IAuthenticationContext {
  isReady: boolean
  isAuthenticated: boolean
  user: IUser | undefined
  groups: string[]
  updateUser: (user: IUser) => void
  updateInformation: (
    data: PartialNull<IUpdateUserInformationPayload>,
    avatar: File | undefined,
    examinationReport: File | undefined,
    cv: File | undefined,
    degreeReport: File | undefined,
  ) => Promise<unknown>
  login: (redirectUri?: string) => unknown
  loginWithPasskey: () => Promise<void>
  registerPasskey: () => Promise<void>
  listCredentials: () => Promise<IKeycloakCredential[]>
  deleteCredential: (credentialId: string) => Promise<void>
  logout: (redirectUrl: string) => unknown
  researchGroups: ILightResearchGroup[]
  isPasskeySupported: boolean | undefined
}

export const AuthenticationContext = createContext<IAuthenticationContext | undefined>(undefined)

export interface IDecodedAccessToken extends JwtPayload {
  given_name: string
  family_name: string
  email: string
  preferred_username: string
  resource_access: Partial<Record<string, { roles: string[] }>>
  [key: string]: unknown
}

export interface IDecodedRefreshToken extends JwtPayload {
  [key: string]: unknown
}
