import { Badge, Button, Popover, Stack } from '@mantine/core'
import { RichTextEditor } from '@mantine/tiptap'
import { NodeViewWrapper, NodeViewProps } from '@tiptap/react'
import { useState } from 'react'

export default (props: NodeViewProps) => {
  const { node, updateAttributes } = props
  const [variable, setVariable] = useState(node.attrs.variable || '')

  return (
    <NodeViewWrapper style={{ display: 'inline-block' }}>
      <Popover width={200} position='bottom' withArrow shadow='md'>
        <Popover.Target>
          <Badge variant='light' color='grape' radius='xs'>
            {variable ? variable : 'Add variable'}
          </Badge>
        </Popover.Target>
        <Popover.Dropdown>
          <Stack>
            <Button
              onClick={() => {
                const newVariable = 'test.test'
                setVariable(newVariable)
                updateAttributes({ variable: newVariable })
              }}
            >
              change variable
            </Button>
          </Stack>
        </Popover.Dropdown>
      </Popover>
    </NodeViewWrapper>
  )
}
