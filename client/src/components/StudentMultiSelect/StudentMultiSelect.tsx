import { Badge, Group, MultiSelect, Text } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { doRequest } from '../../requests/request'
import { getApiResponseErrorMessage } from '../../requests/handler'
import { showSimpleError } from '../../utils/notification'
import { formatUser } from '../../utils/format'
import { arrayUnique } from '../../utils/array'
import type { PaginationResponse } from '../../requests/responses/pagination'
import type { IKeycloakStudent, ILightUser, IMinimalUser } from '../../requests/responses/user'
import AvatarUser from '../AvatarUser/AvatarUser'

const DB_PREFIX = 'db:'
const KC_PREFIX = 'kc:'
const KEYCLOAK_FALLBACK_MIN_LENGTH = 2

interface IStudentMultiSelectValue {
  dbUserIds: string[]
  keycloakUsernames: string[]
}

interface IStudentMultiSelectProps {
  label?: string
  required?: boolean
  disabled?: boolean
  /** Pre-loaded users to display as selected options without requiring an API call. */
  initialUsers?: ILightUser[]
  value: IStudentMultiSelectValue
  onChange: (next: IStudentMultiSelectValue) => void
  error?: ReactNode
}

interface IDbOption {
  value: string
  label: string
  source: 'db'
  user: ILightUser
}

interface IKeycloakOption {
  value: string
  label: string
  source: 'keycloak'
  user: IKeycloakStudent
}

type IOption = IDbOption | IKeycloakOption

const dbValue = (userId: string) => `${DB_PREFIX}${userId}`
const kcValue = (username: string) => `${KC_PREFIX}${username}`

const splitSelection = (values: string[]): IStudentMultiSelectValue => ({
  dbUserIds: values
    .filter((value) => value.startsWith(DB_PREFIX))
    .map((value) => value.slice(DB_PREFIX.length)),
  keycloakUsernames: values
    .filter((value) => value.startsWith(KC_PREFIX))
    .map((value) => value.slice(KC_PREFIX.length)),
})

const joinSelection = (value: IStudentMultiSelectValue): string[] => [
  ...value.dbUserIds.map(dbValue),
  ...value.keycloakUsernames.map(kcValue),
]

const keycloakDisplayName = (user: IKeycloakStudent): string => {
  const composed = [user.firstName, user.lastName].filter(Boolean).join(' ').trim()
  if (composed.length > 0) {
    return composed
  }
  const email = user.email?.trim() ?? ''
  return email.length > 0 ? email : user.username
}

const keycloakToMinimalUser = (user: IKeycloakStudent): IMinimalUser => ({
  userId: kcValue(user.username),
  firstName: user.firstName ?? '',
  lastName: user.lastName ?? '',
  avatar: null,
})

/**
 * Student picker that searches the local DB first and transparently falls back to a Keycloak
 * directory search when the DB has no match. Used by the Create Thesis dialog so supervisors
 * can pick a student who has not yet logged in to the portal.
 *
 * Selected entries are tracked separately as DB user IDs (UUIDs) vs. Keycloak usernames so the
 * caller can submit them through the corresponding {@code CreateThesisPayload} fields; the
 * backend materialises the Keycloak entries as part of thesis creation.
 */
export const StudentMultiSelect = ({
  label,
  required,
  disabled,
  initialUsers = [],
  value,
  onChange,
  error,
}: IStudentMultiSelectProps) => {
  const selected = joinSelection(value)

  const [loading, setLoading] = useState(false)
  const [fetchVersion, setFetchVersion] = useState(0)
  const [searchValue, setSearchValue] = useState('')
  const [debouncedSearch] = useDebouncedValue(searchValue, 500)
  const [dbOptions, setDbOptions] = useState<IDbOption[]>([])
  const [keycloakOptions, setKeycloakOptions] = useState<IKeycloakOption[]>([])

  // Read inside async callbacks so they see the latest selection even when
  // the effect itself doesn't re-run on `value` changes (otherwise a pick
  // that happens between a fetch start and its response can get dropped
  // from the options list and render as the raw `db:<uuid>` / `kc:<id>`).
  const valueRef = useRef(value)
  useEffect(() => {
    valueRef.current = value
  }, [value])

  useEffect(() => {
    if (disabled || fetchVersion === 0) {
      setLoading(false)
      return
    }
    setLoading(true)
    const trimmed = debouncedSearch.trim()
    let cancelled = false

    const cancel = doRequest<PaginationResponse<ILightUser>>(
      '/v2/users',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          groups: 'student',
          searchQuery: trimmed,
          page: '0',
          limit: '100',
        },
      },
      (res) => {
        if (cancelled) {
          return
        }
        if (!res.ok) {
          showSimpleError(getApiResponseErrorMessage(res))
          setLoading(false)
          return
        }

        const fetchedDbOptions: IDbOption[] = (res.data.content ?? []).map((user) => ({
          value: dbValue(user.userId),
          label: formatUser(user, { withUniversityId: true }),
          source: 'db' as const,
          user,
        }))

        const selectedDbValues = new Set(valueRef.current.dbUserIds.map(dbValue))
        setDbOptions((prev) =>
          arrayUnique(
            [...prev.filter((option) => selectedDbValues.has(option.value)), ...fetchedDbOptions],
            (a, b) => a.value === b.value,
          ),
        )

        const shouldQueryKeycloak =
          fetchedDbOptions.length === 0 && trimmed.length >= KEYCLOAK_FALLBACK_MIN_LENGTH

        if (!shouldQueryKeycloak) {
          setKeycloakOptions((prev) =>
            prev.filter((option) =>
              valueRef.current.keycloakUsernames.some(
                (username) => kcValue(username) === option.value,
              ),
            ),
          )
          setLoading(false)
          return
        }

        doRequest<IKeycloakStudent[]>(
          '/v2/users/keycloak/students',
          {
            method: 'GET',
            requiresAuth: true,
            params: { searchKey: trimmed },
          },
          (kcRes) => {
            if (cancelled) {
              return
            }
            if (!kcRes.ok) {
              showSimpleError(getApiResponseErrorMessage(kcRes))
              setLoading(false)
              return
            }
            const fetchedKeycloak: IKeycloakOption[] = kcRes.data
              .filter((user) => !user.existsLocally)
              .map((user) => ({
                value: kcValue(user.username),
                label: keycloakDisplayName(user),
                source: 'keycloak' as const,
                user,
              }))
            const selectedKcValues = new Set(valueRef.current.keycloakUsernames.map(kcValue))
            setKeycloakOptions((prev) =>
              arrayUnique(
                [
                  ...prev.filter((option) => selectedKcValues.has(option.value)),
                  ...fetchedKeycloak,
                ],
                (a, b) => a.value === b.value,
              ),
            )
            setLoading(false)
          },
        )
      },
    )

    return () => {
      cancelled = true
      cancel?.()
    }
  }, [debouncedSearch, fetchVersion, disabled])

  const initialDbOptions: IDbOption[] = initialUsers
    .filter((user) => value.dbUserIds.includes(user.userId))
    .map((user) => ({
      value: dbValue(user.userId),
      label: formatUser(user, { withUniversityId: true }),
      source: 'db' as const,
      user,
    }))

  const allOptions: IOption[] = arrayUnique(
    [...dbOptions, ...initialDbOptions, ...keycloakOptions],
    (a, b) => a.value === b.value,
  )

  const dbGroupItems = allOptions.filter((option) => option.source === 'db')
  const keycloakGroupItems = allOptions.filter((option) => option.source === 'keycloak')

  const data: Array<{ group: string; items: Array<{ value: string; label: string }> }> = []
  if (dbGroupItems.length > 0) {
    data.push({
      group: 'Existing users',
      items: dbGroupItems.map((option) => ({
        value: option.value,
        label: option.label,
      })),
    })
  }
  if (keycloakGroupItems.length > 0) {
    data.push({
      group: 'New users — will be added on save',
      items: keycloakGroupItems.map((option) => ({
        value: option.value,
        label: option.label,
      })),
    })
  }

  return (
    <MultiSelect
      label={label}
      required={required}
      disabled={disabled}
      placeholder='Search...'
      value={selected}
      onChange={(next) => onChange(splitSelection(next))}
      data={data}
      searchable={true}
      clearable={true}
      searchValue={searchValue}
      onSearchChange={setSearchValue}
      onDropdownOpen={() => setFetchVersion((v) => v + 1)}
      hidePickedOptions={true}
      limit={20}
      filter={({ options }) => options}
      nothingFoundMessage={loading ? 'Loading...' : 'Nothing found...'}
      error={error}
      renderOption={({ option }) => {
        const found = allOptions.find((item) => item.value === option.value)
        if (!found) {
          return null
        }
        if (found.source === 'db') {
          return <AvatarUser user={found.user} withUniversityId={true} />
        }
        return (
          <Group gap='xs' wrap='nowrap'>
            <AvatarUser user={keycloakToMinimalUser(found.user)} />
            <Text size='xs' c='dimmed'>
              {found.user.email ?? found.user.username}
            </Text>
            <Badge size='xs' color='blue' variant='light'>
              new
            </Badge>
          </Group>
        )
      }}
    />
  )
}
