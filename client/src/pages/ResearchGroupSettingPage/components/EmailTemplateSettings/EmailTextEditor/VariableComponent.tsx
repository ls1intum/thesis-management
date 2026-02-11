import { Badge, Combobox, Group, useCombobox } from '@mantine/core'
import { CaretDownIcon, CaretUpIcon } from '@phosphor-icons/react'
import { NodeViewWrapper, NodeViewProps } from '@tiptap/react'
import { useEffect, useState } from 'react'
import { IMailVariableDto } from '../../../../../requests/responses/emailtemplate'
import VariableComboboxOptions from './VariableComboboxOptions'

export default (props: NodeViewProps) => {
  const { node, updateAttributes, extension } = props

  const selectableVariables = (extension.options.selectableVariables as IMailVariableDto[]) ?? []

  const [variable, setVariable] = useState<string>(node.attrs.variable ?? '')
  const [template, setTemplate] = useState<string>(node.attrs.template ?? '')

  useEffect(() => {
    setVariable(node.attrs.variable ?? '')
    setTemplate(node.attrs.template ?? '')
  }, [node.attrs.variable, node.attrs.template])

  const setAndPersist = (v: string, t: string) => {
    setVariable(v)
    updateAttributes({ variable: v })
    setTemplate(t)
    updateAttributes({ template: t })
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
          const variable = selectableVariables.find((v) => v.templateVariable === val)
          if (variable) {
            setAndPersist(variable.label, variable.templateVariable)
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
              {variable || 'Add variable'}
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
