import { MultiSelect } from '@mantine/core'
import { useEffect, useState } from 'react'
import { doRequest } from '../../requests/request'
import { PaginationResponse } from '../../requests/responses/pagination'
import { ILightUser } from '../../requests/responses/user'
import { useDebouncedValue } from '@mantine/hooks'
import type { GetInputPropsReturnType } from '@mantine/form'
import { formatUser } from '../../utils/format'
import { arrayUnique } from '../../utils/array'
import { showSimpleError } from '../../utils/notification'
import { getApiResponseErrorMessage } from '../../requests/handler'
import AvatarUser from '../AvatarUser/AvatarUser'

interface IUserMultiSelectProps extends GetInputPropsReturnType {
  /** Maximum number of users that can be selected. Defaults to Infinity. */
  maxValues?: number
  /** Keycloak group names to filter the user search results by (e.g. ['student'], ['advisor', 'supervisor']). */
  groups: string[]
  /** Whether the select is disabled. When disabled, no API calls are made. */
  disabled?: boolean
  /** Label displayed above the select input. */
  label?: string
  /** Whether the field is required. */
  required?: boolean
  /** Pre-loaded users to display as selected options without requiring an API call. */
  initialUsers?: ILightUser[]
}

/**
 * A searchable multi-select component for choosing users from the system.
 *
 * Users are fetched from the `/v2/users` API endpoint with group filtering and search support.
 * To avoid unnecessary API calls on mount, the component uses lazy fetching: no request is made
 * until the user interacts with the dropdown (click or open). Already-selected users are displayed
 * using the `initialUsers` prop without requiring a fetch.
 */
export const UserMultiSelect = (props: IUserMultiSelectProps) => {
  const {
    groups,
    maxValues = Infinity,
    initialUsers = [],
    disabled,
    label,
    required,
    ...inputProps
  } = props

  const selected: string[] = inputProps.value ?? []

  const [loading, setLoading] = useState(false)
  // Incremented on user interaction (click/dropdown open) to trigger a fetch.
  // Starts at 0 meaning no fetch has been requested yet.
  const [fetchVersion, setFetchVersion] = useState(0)
  const [data, setData] = useState<Array<{ value: string; label: string; user: ILightUser }>>([])
  const [searchValue, setSearchValue] = useState('')

  const [debouncedSearchValue] = useDebouncedValue(searchValue, 500)

  useEffect(() => {
    // Skip fetching when disabled or until the user interacts with the dropdown
    if (disabled || fetchVersion === 0) {
      setLoading(false)
      return
    }

    setLoading(true)

    return doRequest<PaginationResponse<ILightUser>>(
      '/v2/users',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          groups: groups.join(','),
          searchQuery: debouncedSearchValue,
          page: '0',
          limit: '100',
        },
      },
      (res) => {
        if (res.ok) {
          // Merge newly fetched users with previously selected ones to avoid losing selections
          setData((prevState) =>
            arrayUnique(
              [
                ...prevState.filter((item) => selected.includes(item.value)),
                ...(res.data.content ?? []).map((user) => ({
                  value: user.userId,
                  label: formatUser(user, { withUniversityId: true }),
                  user: user,
                })),
              ],
              (a, b) => a.value === b.value,
            ),
          )
          setLoading(false)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
          setLoading(false)
        }
      },
    )
  }, [groups.join(','), debouncedSearchValue, fetchVersion, disabled])

  // Combine fetched data with initialUsers so selected options are always visible,
  // even before the first API call
  const mergedData = arrayUnique(
    [
      ...data,
      ...initialUsers
        .filter((user) => selected.includes(user.userId))
        .map((user) => ({
          value: user.userId,
          label: formatUser(user, { withUniversityId: true }),
          user,
        })),
    ],
    (a, b) => a.value === b.value,
  )

  return (
    <MultiSelect
      {...inputProps}
      data={mergedData}
      renderOption={({ option }) => {
        const item = mergedData.find((row) => row.value === option.value)

        if (!item) {
          return null
        }

        return <AvatarUser user={item.user} withUniversityId={true} />
      }}
      disabled={disabled}
      searchable={selected.length < maxValues}
      clearable={true}
      searchValue={searchValue}
      onDropdownOpen={() => setFetchVersion((v) => v + 1)}
      onSearchChange={setSearchValue}
      hidePickedOptions={selected.length < maxValues}
      maxValues={maxValues}
      limit={10}
      filter={({ options }) => options}
      placeholder={selected.length < maxValues ? 'Search...' : undefined}
      nothingFoundMessage={!loading ? 'Nothing found...' : 'Loading...'}
      label={label}
      required={required}
    />
  )
}
