import { Button, Combobox, Group, useCombobox, useComputedColorScheme } from '@mantine/core'
import { CaretDown, CaretUp, MagnifyingGlass } from 'phosphor-react'
import { useEffect, useState } from 'react'

interface IDropDownMultiSelectProps {
  data: string[]
  searchPlaceholder?: string
  dropdownLable?: string
  withSearch?: boolean
  renderOption?: (item: string) => React.ReactNode
  selectedItems: string[]
  setSelectedItem: (item: string) => void
  showSelectedOnTop?: boolean
  withoutDropdown?: boolean
}

const DropDownMultiSelect = ({
  data,
  searchPlaceholder,
  dropdownLable,
  withSearch = true,
  renderOption,
  selectedItems,
  setSelectedItem,
  showSelectedOnTop = true,
  withoutDropdown = false,
}: IDropDownMultiSelectProps) => {
  const [search, setSearch] = useState('')
  const [displayData, setDisplayData] = useState<string[]>(data)

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

  const computedColorScheme = useComputedColorScheme()

  useEffect(() => {
    const filteredData = data.filter((item) => item.toLowerCase().includes(search.toLowerCase()))

    setDisplayData(
      showSelectedOnTop
        ? filteredData.filter((item) => !selectedItems.includes(item))
        : filteredData,
    )
  }, [data, search, selectedItems])

  const content = () => (
    <>
      {withSearch && (
        <Combobox.Search
          styles={{
            input: {
              display: 'block',
              width: '100%',
            },
          }}
          value={search}
          onChange={(event) => setSearch(event.currentTarget.value)}
          placeholder={searchPlaceholder ?? 'Search...'}
          leftSection={<MagnifyingGlass size={16} />}
        />
      )}

      <Combobox.Options mah={withoutDropdown ? undefined : 200} style={{ overflowY: 'auto' }}>
        {showSelectedOnTop && selectedItems.length > 0 && (
          <Combobox.Group label='Selected' pb={10}>
            {selectedItems.length > 0 &&
              selectedItems.map((item) => (
                <Combobox.Option key={item} value={item}>
                  {renderOption ? renderOption(item) : item}
                </Combobox.Option>
              ))}
          </Combobox.Group>
        )}

        <Combobox.Group
          label={selectedItems.length > 0 && showSelectedOnTop ? 'Suggestions' : undefined}
          styles={{
            groupLabel: {
              backgroundColor:
                computedColorScheme === 'dark'
                  ? 'var(--mantine-color-dark-4)'
                  : 'var(--mantine-color-gray-2)',
              color:
                computedColorScheme === 'dark'
                  ? 'var(--mantine-color-dark-0)'
                  : 'var(--mantine-color-gray-9)',
            },
          }}
        >
          {displayData.length > 0 ? (
            displayData.map((item) => (
              <Combobox.Option key={item} value={item}>
                {renderOption ? renderOption(item) : item}
              </Combobox.Option>
            ))
          ) : (
            <Combobox.Empty>Nothing found</Combobox.Empty>
          )}
        </Combobox.Group>
      </Combobox.Options>
    </>
  )

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

        {withoutDropdown ? (
          combobox.dropdownOpened ? (
            content()
          ) : undefined
        ) : (
          <Combobox.Dropdown>{content()}</Combobox.Dropdown>
        )}
      </Combobox>
    </>
  )
}
export default DropDownMultiSelect
