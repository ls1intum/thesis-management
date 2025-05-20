import { Autocomplete, Group, Loader, Text } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { useEffect, useState } from 'react'
import { showSimpleError } from '../../utils/notification'
import { doRequest } from '../../requests/request'
import { getApiResponseErrorMessage } from '../../requests/handler'

interface KeycloakUserElement {
  id: string
  username: string
  firstName: string
  lastName: string
  email: string
  hasResearchGroup: boolean
}

interface KeycloakUserAutocompleteProps {
  selectedLabel: string
  onSelect: (username: string, label: string) => void
  label?: string
  placeholder?: string
  withAsterisk?: boolean
}

const KeycloakUserAutocomplete = ({
  selectedLabel,
  onSelect,
  label,
  placeholder = 'Search by name or email...',
  withAsterisk = false,
}: KeycloakUserAutocompleteProps) => {
  const [searchKey, setSearchKey] = useState('')
  const [debouncedSearchKey] = useDebouncedValue(searchKey, 300)
  const [userOptions, setUserOptions] = useState<KeycloakUserElement[]>([])
  const [loadingUsers, setLoadingUsers] = useState(false)

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

    doRequest<KeycloakUserElement[]>(
      '/v2/users/keycloak-users',
      {
        method: 'GET',
        requiresAuth: true,
        params: { searchKey: debouncedSearchKey },
      },
      (res) => {
        setLoadingUsers(false)
        if (res.ok) {
          setUserOptions(res.data)
        } else {
          setUserOptions([])
          showSimpleError(getApiResponseErrorMessage(res))
        }
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
        value: `${user.firstName} ${user.lastName} (${user.username}): ${user.email} -> ${user.hasResearchGroup}`,
        disabled: user.hasResearchGroup,
      }))}
      limit={10}
      rightSection={loadingUsers ? <Loader size='xs' /> : null}
      renderOption={({ option }) => {
        const user = userOptions.find(
          (u) =>
            `${u.firstName} ${u.lastName} (${u.username}): ${u.email} -> ${u.hasResearchGroup}` ===
            option.value,
        )

        return (
          <div>
            <Group gap='xs' wrap='nowrap'>
              <div>
                <Group>
                  <Text size='sm' fw={500}>
                    {user?.firstName} {user?.lastName}
                  </Text>
                  {user?.hasResearchGroup && (
                    <Text size='xs' c='red'>
                      User already has a research group
                    </Text>
                  )}
                </Group>
                <Text size='xs' c='dimmed'>
                  @{user?.username} • {user?.email}
                </Text>
              </div>
            </Group>
          </div>
        )
      }}
      onOptionSubmit={(val) => {
        const selected = userOptions.find(
          (u) => `${u.firstName} ${u.lastName} (${u.username}): ${u.email}` === val,
        )
        if (selected) {
          const labelString = `${selected.firstName} ${selected.lastName}`
          onSelect(selected.username, labelString)
          setSearchKey(labelString)
        }
      }}
    />
  )
}

export default KeycloakUserAutocomplete
