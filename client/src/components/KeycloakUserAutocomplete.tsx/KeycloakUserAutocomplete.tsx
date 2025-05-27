import { Autocomplete, Loader } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { useEffect, useState } from 'react'
import { showSimpleError } from '../../utils/notification'
import { doRequest } from '../../requests/request'
import { getApiResponseErrorMessage } from '../../requests/handler'
import UserInformationRow from '../UserInformationRow/UserInformationRow'
import { ILightUser } from '../../requests/responses/user'
import { IKeycloakUserElement } from '../../requests/responses/keycloakUser'

interface KeycloakUserAutocompleteProps {
  selectedLabel: string
  onSelect: (username: string, label: string) => void
  label?: string
  placeholder?: string
  withAsterisk?: boolean
  previousUser?: ILightUser
}

const KeycloakUserAutocomplete = ({
  selectedLabel,
  onSelect,
  label,
  placeholder = 'Search by name or email...',
  withAsterisk = false,
  previousUser,
}: KeycloakUserAutocompleteProps) => {
  const [searchKey, setSearchKey] = useState('')
  const [debouncedSearchKey] = useDebouncedValue(searchKey, 300)
  const [userOptions, setUserOptions] = useState<IKeycloakUserElement[]>([])
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [selectedUsername, setSelectedUsername] = useState<string>(previousUser?.universityId || '')

  const getUserOptionValue = (user: {
    firstName: string
    lastName: string
    username: string
    email: string
    hasResearchGroup: boolean
  }) =>
    `${user.firstName} ${user.lastName} (${user.username}): ${user.email} -> ${user.hasResearchGroup}`

  // Sync initial selectedLabel into searchKey
  useEffect(() => {
    setSearchKey(selectedLabel)
  }, [selectedLabel])

  useEffect(() => {
    if (!debouncedSearchKey.trim()) {
      setUserOptions([])
      return
    }

    setLoadingUsers(true)

    doRequest<IKeycloakUserElement[]>(
      '/v2/users/keycloak',
      {
        method: 'GET',
        requiresAuth: true,
        params: { searchKey: debouncedSearchKey },
      },
      (res) => {
        if (res.ok) {
          setUserOptions(res.data)
        } else {
          setUserOptions([])
          showSimpleError(getApiResponseErrorMessage(res))
        }
        setLoadingUsers(false)
      },
    )
  }, [debouncedSearchKey])

  return (
    <Autocomplete
      label={label}
      placeholder={placeholder}
      withAsterisk={withAsterisk}
      value={searchKey}
      onChange={(val) => {
        setSearchKey(val)

        // Clear selected user if input is cleared
        if (val.trim() === '') {
          onSelect('', '')
        }
      }}
      data={userOptions.map((user) => ({
        value: getUserOptionValue(user),
        disabled:
          (user.hasResearchGroup && user.username !== previousUser?.universityId) ||
          selectedUsername === user.username,
      }))}
      limit={10}
      rightSection={loadingUsers ? <Loader size='xs' /> : null}
      renderOption={({ option }) => {
        const user = userOptions.find((u) => getUserOptionValue(u) === option.value)

        return (
          <UserInformationRow
            firstName={user?.firstName}
            lastName={user?.lastName}
            username={user?.username}
            email={user?.email}
            disableMessage={
              user?.hasResearchGroup && user?.username !== previousUser?.universityId
                ? 'User already has a research group'
                : selectedUsername === user?.username
                  ? 'User is already selected'
                  : undefined
            }
          />
        )
      }}
      onOptionSubmit={(val) => {
        const selected = userOptions.find((u) => getUserOptionValue(u) === val)
        if (selected) {
          const labelString = `${selected.firstName} ${selected.lastName}`
          onSelect(selected.username, labelString)
          setSearchKey(labelString)
          setSelectedUsername(selected.username)
        }
      }}
    />
  )
}

export default KeycloakUserAutocomplete
