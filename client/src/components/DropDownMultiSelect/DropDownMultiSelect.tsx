import { Button, Combobox, Group, useCombobox } from '@mantine/core'
import { CaretDown, CaretUp, MagnifyingGlass } from 'phosphor-react'
import { useState } from 'react'

interface IDropDownMultiSelectProps {
  data: string[]
  searchPlaceholder?: string
  dropdownLable?: string
  withSearch?: boolean
  renderOption?: (item: string) => React.ReactNode
  selectedItems: string[]
  setSelectedItem: (item: string) => void
}

const DropDownMultiSelect = ({
  data,
  searchPlaceholder,
  dropdownLable,
  withSearch = true,
  renderOption,
  selectedItems,
  setSelectedItem,
}: IDropDownMultiSelectProps) => {
  const [search, setSearch] = useState('')

  const combobox = useCombobox({
    onDropdownClose: () => {
      combobox.resetSelectedOption()
      combobox.focusTarget()
      setSearch('')
    },

    onDropdownOpen: () => {
      if (withSearch) {
        combobox.focusSearchInput()
      }
    },
  })

  return (
    <>
      <Combobox
        store={combobox}
        width={250}
        position='bottom-start'
        onOptionSubmit={(val) => {
          setSelectedItem(val)
        }}
        offset={4}
        shadow='md'
      >
        <Combobox.Target withAriaAttributes={false}>
          <Button onClick={() => combobox.toggleDropdown()} variant='default'>
            <Group>
              {dropdownLable ?? 'Pick item'}
              {combobox.dropdownOpened ? <CaretUp size={16} /> : <CaretDown size={16} />}
            </Group>
          </Button>
        </Combobox.Target>

        <Combobox.Dropdown>
          {withSearch && (
            <Combobox.Search
              value={search}
              onChange={(event) => setSearch(event.currentTarget.value)}
              placeholder={searchPlaceholder ?? 'Search...'}
              leftSection={<MagnifyingGlass size={16} />}
            />
          )}

          <Combobox.Options>
            {data.length > 0 ? (
              data
                .filter((item) => item.toLowerCase().includes(search.toLowerCase()))
                .map((item) => (
                  <Combobox.Option key={item} value={item}>
                    {renderOption ? renderOption(item) : item}
                  </Combobox.Option>
                ))
            ) : (
              <Combobox.Empty>Nothing found</Combobox.Empty>
            )}
          </Combobox.Options>
        </Combobox.Dropdown>
      </Combobox>
    </>
  )
}
export default DropDownMultiSelect
