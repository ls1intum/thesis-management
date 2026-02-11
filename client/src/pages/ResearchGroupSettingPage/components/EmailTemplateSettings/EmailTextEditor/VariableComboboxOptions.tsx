import { Combobox } from '@mantine/core'
import { useMemo } from 'react'
import { IMailVariableDto } from '../../../../../requests/responses/emailtemplate'
import { MagnifyingGlassIcon } from '@phosphor-icons/react'

interface IVariableComboboxOptionsProps {
  selectableVariables: IMailVariableDto[]
  search: string
  setSearch: (search: string) => void
}

const VariableComboboxOptions = ({
  selectableVariables,
  search,
  setSearch,
}: IVariableComboboxOptionsProps) => {
  const groupedData = useMemo(() => {
    const filtered = selectableVariables.filter((variable) =>
      variable.label.toLowerCase().includes(search.toLowerCase()),
    )

    return filtered.reduce<Record<string, IMailVariableDto[]>>((acc, variable) => {
      const group = variable.group ?? 'Other'
      acc[group] ??= []
      acc[group].push(variable)
      return acc
    }, {})
  }, [search, selectableVariables])

  return (
    <>
      <Combobox.Search
        styles={{
          input: {
            display: 'block',
            width: '100%',
          },
        }}
        value={search}
        onChange={(event) => setSearch(event.currentTarget.value)}
        placeholder={'Search...'}
        leftSection={<MagnifyingGlassIcon size={16} />}
      />

      <Combobox.Options mah={200} style={{ overflowY: 'auto' }}>
        {Object.entries(groupedData).length > 0 ? (
          Object.entries(groupedData).map(([groupName, variables]) => (
            <Combobox.Group key={groupName} label={groupName}>
              {variables.map((item) => (
                <Combobox.Option key={item.templateVariable} value={item.templateVariable}>
                  {item.label}
                </Combobox.Option>
              ))}
            </Combobox.Group>
          ))
        ) : (
          <Combobox.Empty>Nothing found</Combobox.Empty>
        )}
      </Combobox.Options>
    </>
  )
}

export default VariableComboboxOptions
