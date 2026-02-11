import { Node } from '@tiptap/core'
import { ReactNodeViewRenderer } from '@tiptap/react'
import VariableComponent from './VariableComponent'
import { IMailVariableDto } from '../../../../../requests/responses/emailtemplate'

export default Node.create({
  name: 'react-component',
  group: 'inline',
  inline: true,
  atom: true,

  addOptions() {
    return {
      selectableVariables: [] as IMailVariableDto[],
    }
  },

  addAttributes() {
    return {
      variable: { default: null },
      template: { default: null },
    }
  },

  parseHTML() {
    return [{ tag: 'react-component' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['react-component', HTMLAttributes]
  },

  addNodeView() {
    return ReactNodeViewRenderer(VariableComponent)
  },
})
