import { mergeAttributes, Node } from '@tiptap/core'
import { ReactNodeViewRenderer } from '@tiptap/react'
import VariableComponent from './VariableComponent'

export default Node.create({
  name: 'reactComponent',

  group: 'inline',
  inline: true,

  atom: true,

  addAttributes() {
    return {
      variable: {
        default: '',
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'react-component',
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ['react-component', mergeAttributes(HTMLAttributes)]
  },

  addNodeView() {
    return ReactNodeViewRenderer(VariableComponent)
  },
})
