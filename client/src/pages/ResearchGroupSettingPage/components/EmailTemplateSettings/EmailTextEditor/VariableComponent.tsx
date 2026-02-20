import { Badge, Combobox, Group, useCombobox } from '@mantine/core'
import { CaretDownIcon, CaretUpIcon } from '@phosphor-icons/react'
import { NodeViewWrapper, NodeViewProps } from '@tiptap/react'
import { useEffect, useState } from 'react'
import { IMailVariableDto } from '../../../../../requests/responses/emailtemplate'
import VariableComboboxOptions from './VariableComboboxOptions'

export default function VariableComponent(props: NodeViewProps) {
  const { node, updateAttributes, extension } = props

  const selectableVariables = (extension.options.selectableVariables as IMailVariableDto[]) ?? []

  const [variableLabel, setVariableLabel] = useState<string>(node.attrs.variable ?? '')

  useEffect(() => {
    setVariableLabel(node.attrs.variable ?? '')
  }, [node.attrs.variable, node.attrs.group])

  const setAndPersist = (v: string, g: string) => {
    setVariableLabel(v)
    updateAttributes({ variable: v })
    updateAttributes({ group: g })
  }

  const [search, setSearch] = useState('')

  const combobox = useCombobox({
    onDropdownClose: () => {
      combobox.resetSelectedOption()
      combobox.focusTarget()
      setSearch('')
    },

    onDropdownOpen: () => {
      combobox.focusSearchInput()
    },
  })

  return (
    <NodeViewWrapper style={{ display: 'inline-block' }}>
      <Combobox
        store={combobox}
        width={250}
        position='bottom-start'
        onOptionSubmit={(val) => {
          const matched = selectableVariables.find((v) => v.templateVariable === val)
          if (matched) {
            setAndPersist(matched.label, matched.group)
          }
        }}
        offset={4}
        shadow='md'
      >
        <Combobox.Target withAriaAttributes={false}>
          <Badge
            variant='light'
            color={'primary'}
            radius='sm'
            onClick={() => combobox.toggleDropdown()}
          >
            <Group gap={5}>
              {variableLabel || 'Add variable'}
              {combobox.dropdownOpened ? <CaretUpIcon size={12} /> : <CaretDownIcon size={12} />}
            </Group>
          </Badge>
        </Combobox.Target>

        <Combobox.Dropdown>
          <VariableComboboxOptions
            selectableVariables={selectableVariables}
            search={search}
            setSearch={setSearch}
          />
        </Combobox.Dropdown>
      </Combobox>
    </NodeViewWrapper>
  )
}
