import { Link, RichTextEditor, useRichTextEditorContext } from '@mantine/tiptap'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import { useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { Button, Group, Popover, Select, Stack, TextInput } from '@mantine/core'
import ReactComponent from './Extension'
import { IEmailTemplate } from '../../../../../requests/responses/emailtemplate'
import { useEffect, useMemo, useState } from 'react'
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

        <RichTextEditor.ControlsGroup>
          <FontSizeControl />
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

//TODO: OUR DEFAULT IS 16px, show that instead of empty as default
//TODO: Nicer Dropdown look
const FONT_SIZES = [
  { value: '', label: 'Default' },
  { value: '8px', label: '8' },
  { value: '9px', label: '9' },
  { value: '10px', label: '10' },
  { value: '11px', label: '11' },
  { value: '12px', label: '12' },
  { value: '14px', label: '14' },
  { value: '16px', label: '16' },
  { value: '18px', label: '18' },
  { value: '20px', label: '20' },
  { value: '24px', label: '24' },
  { value: '28px', label: '28' },
  { value: '32px', label: '32' },
  { value: '48px', label: '48' },
  { value: '64px', label: '64' },
  { value: '72px', label: '72' },
  { value: '96px', label: '96' },
  { value: '128px', label: '128' },
]

function getCurrentFontSize(editor: any): string {
  const attrs = editor.getAttributes('textStyle')
  return (attrs?.fontSize as string) ?? ''
}

export function FontSizeControl() {
  const { editor } = useRichTextEditorContext()
  const [value, setValue] = useState<string>('')

  const data = useMemo(() => FONT_SIZES, [])

  useEffect(() => {
    if (!editor) return

    const sync = () => setValue(getCurrentFontSize(editor))

    sync()
    editor.on('selectionUpdate', sync)
    editor.on('transaction', sync) // helps when typing continues with same mark

    return () => {
      editor.off('selectionUpdate', sync)
      editor.off('transaction', sync)
    }
  }, [editor])

  const apply = (next: string | null) => {
    if (!editor) return

    const v = next ?? ''
    setValue(v)

    if (v === '') {
      editor.chain().focus().unsetFontSize().run()
    } else {
      editor.chain().focus().setFontSize(v).run()
    }
  }

  return (
    <Popover width={160} position='bottom' withArrow shadow='md'>
      <Popover.Target>
        <RichTextEditor.Control aria-label='Font size' title='Font size'>
          <Group gap={6} px={6}>
            {value ? value.replace('px', '') : 'Size'}
          </Group>
        </RichTextEditor.Control>
      </Popover.Target>

      <Popover.Dropdown>
        <Select
          data={data}
          value={value}
          onChange={apply}
          placeholder='Choose size'
          searchable
          nothingFoundMessage='No size'
        />
      </Popover.Dropdown>
    </Popover>
  )
}

export default EmailTextEditor
