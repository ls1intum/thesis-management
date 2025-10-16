import { Link, RichTextEditor, useRichTextEditorContext } from '@mantine/tiptap'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import { useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { Button, Group, Popover, Stack, TextInput } from '@mantine/core'
import ReactComponent from './Extension'
import { IEmailTemplate } from '../../../../../requests/responses/emailtemplate'
import { useEffect, useState } from 'react'
import { Plus } from '@phosphor-icons/react'
import { FontSize, TextStyle } from '@tiptap/extension-text-style'

// Function to convert HTML react-component tags to template variables
const convertHtmlToTemplateVariables = (html: string): string => {
  return html.replace(/<react-component\s+variable="([^"]+)"><\/react-component>/g, '[[${$1}]]')
}

// Function to convert template variables back to HTML react-component tags
const convertTemplateVariablesToHtml = (text: string): string => {
  return text.replace(/\[\[\$\{([^}]+)\}\]\]/g, '<react-component variable="$1"></react-component>')
}

interface IEmailTextEditorProps {
  editingTemplate?: IEmailTemplate | null
  setEditingTemplate?: (template: IEmailTemplate | null) => void
}

const EmailTextEditor = ({ editingTemplate, setEditingTemplate }: IEmailTextEditorProps) => {
  const editor = useEditor({
    extensions: [
      StarterKit,
      Underline,
      Link,
      TextAlign.configure({ types: ['heading', 'paragraph', 'div'] }),
      ReactComponent,
      TextStyle,
      FontSize,
    ],
    content: convertTemplateVariablesToHtml(editingTemplate?.bodyHtml ?? ''),
    onUpdate: ({ editor }) => {
      setEditingTemplate &&
        setEditingTemplate({
          ...editingTemplate!,
          bodyHtml: convertHtmlToTemplateVariables(editor.getHTML()),
        })
    },
  })

  // Update editor content if editingTemplate.bodyHtml changes
  useEffect(() => {
    console.log('useEffect triggered with bodyHtml:', editingTemplate?.bodyHtml)

    if (
      editor &&
      editingTemplate &&
      convertTemplateVariablesToHtml(editingTemplate.bodyHtml ?? '') !== editor.getHTML()
    ) {
      editor.commands.setContent(convertTemplateVariablesToHtml(editingTemplate.bodyHtml ?? ''))
    }
  }, [editingTemplate?.bodyHtml, editor])

  return (
    <RichTextEditor editor={editor}>
      <RichTextEditor.Toolbar sticky stickyOffset='var(--docs-header-height)'>
        <RichTextEditor.ControlsGroup>
          <RichTextEditor.Bold />
          <RichTextEditor.Italic />
          <RichTextEditor.Underline />
          <RichTextEditor.Strikethrough />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          <RichTextEditor.AlignLeft />
          <RichTextEditor.AlignCenter />
          <RichTextEditor.AlignJustify />
          <RichTextEditor.AlignRight />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          <RichTextEditor.BulletList />
          <RichTextEditor.OrderedList />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          <RichTextEditor.Undo />
          <RichTextEditor.Redo />
        </RichTextEditor.ControlsGroup>

        <RichTextEditor.ControlsGroup>
          <RichTextEditor.Link />
          <RichTextEditor.Unlink />
        </RichTextEditor.ControlsGroup>

        <InsertVariableButton />
      </RichTextEditor.Toolbar>

      <RichTextEditor.Content />
    </RichTextEditor>
  )
}

function InsertVariableButton() {
  const { editor } = useRichTextEditorContext()

  const [variable, setVariable] = useState('')

  const insertVariable = () => {
    editor?.commands.insertContent(`<react-component variable="${variable}"></react-component>`)
  }

  return (
    <Popover width={200} position='bottom' withArrow shadow='md'>
      <Popover.Target>
        <RichTextEditor.Control aria-label='Insert variable' title='Insert variable'>
          <Group gap={5} p={5}>
            <Plus size={14} />
            Add variable
          </Group>
        </RichTextEditor.Control>
      </Popover.Target>
      <Popover.Dropdown>
        <Stack>
          <TextInput value={variable} onChange={(e) => setVariable(e.currentTarget.value)} />
          <Button onClick={() => insertVariable()}>Insert Variable</Button>
        </Stack>
      </Popover.Dropdown>
    </Popover>
  )
}

export default EmailTextEditor
