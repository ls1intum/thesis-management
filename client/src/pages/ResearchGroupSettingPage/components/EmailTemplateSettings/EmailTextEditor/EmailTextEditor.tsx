import { Link, RichTextEditor, useRichTextEditorContext } from '@mantine/tiptap'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import { BubbleMenu, useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import TextStyle from '@tiptap/extension-text-style'
import { Button, Group, Popover, Stack, Text } from '@mantine/core'
import { useState } from 'react'
import ReactComponent from './Extension'
import { IEmailTemplate } from '../../../../../requests/responses/emailtemplate'
import { Plus } from 'phosphor-react'

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
      TextStyle,
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      ReactComponent,
    ],
    content: convertTemplateVariablesToHtml(editingTemplate?.bodyHtml ?? ''),
    onUpdate: ({ editor }) => {
      setEditingTemplate &&
        setEditingTemplate({
          ...editingTemplate!,
          bodyHtml: convertHtmlToTemplateVariables(editor.getHTML()),
        })

      console.log(convertHtmlToTemplateVariables(editor.getHTML()))
    },
  })

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

        <InsertVariableButton />
      </RichTextEditor.Toolbar>

      {editor && (
        <BubbleMenu editor={editor}>
          <RichTextEditor.ControlsGroup>
            <RichTextEditor.Link />
            <RichTextEditor.Unlink />
          </RichTextEditor.ControlsGroup>
        </BubbleMenu>
      )}

      <RichTextEditor.Content />
    </RichTextEditor>
  )
}

function InsertVariableButton() {
  const { editor } = useRichTextEditorContext()

  return (
    <Popover width={200} position='bottom' withArrow shadow='md'>
      <Popover.Target>
        <RichTextEditor.Control aria-label='Insert variable' title='Insert variable'>
          <Group py={2} px={5} gap={5}>
            <Plus size={14} />
            Add variable
          </Group>
        </RichTextEditor.Control>
      </Popover.Target>
      <Popover.Dropdown>
        <Stack>
          <Button
            onClick={() =>
              editor?.commands.insertContent(
                '<react-component variable="proposal.createBy.firstName"></react-component>',
              )
            }
          >
            Insert testcomponent
          </Button>
        </Stack>
      </Popover.Dropdown>
    </Popover>
  )
}

export default EmailTextEditor
