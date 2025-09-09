import { Badge, Button, Group, Popover, Stack, TextInput } from '@mantine/core'
import { NodeViewWrapper, NodeViewProps } from '@tiptap/react'
import { CaretDown } from 'phosphor-react'
import { useState } from 'react'

export default (props: NodeViewProps) => {
  const { node, updateAttributes } = props
  const [newVariable, setNewVariable] = useState(node.attrs.variable || '')
  const [variable, setVariable] = useState(node.attrs.variable || '')

  return (
    <NodeViewWrapper style={{ display: 'inline-block' }}>
      <Popover width={200} position='bottom' withArrow shadow='md'>
        <Popover.Target>
          <Badge variant='light' color='grape' radius='xs'>
            <Group gap={5}>
              {variable ? variable : 'Add variable'}
              <CaretDown size={12} />
            </Group>
          </Badge>
        </Popover.Target>
        <Popover.Dropdown>
          <Stack>
            <TextInput
              value={newVariable}
              onChange={(e) => setNewVariable(e.currentTarget.value)}
            />
            <Button
              onClick={() => {
                setVariable(newVariable)
                updateAttributes({ variable: newVariable })
              }}
            >
              Change Variable
            </Button>
          </Stack>
        </Popover.Dropdown>
      </Popover>
    </NodeViewWrapper>
  )
}
