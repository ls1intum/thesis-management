import { Link, RichTextEditor, useRichTextEditorContext } from '@mantine/tiptap'
import TextAlign from '@tiptap/extension-text-align'
import Underline from '@tiptap/extension-underline'
import { useEditor } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import { Combobox, Group, Select, useCombobox } from '@mantine/core'
import ReactComponent from './Extension'
import { IEmailTemplate, IMailVariableDto } from '../../../../../requests/responses/emailtemplate'
import { useEffect, useMemo, useState } from 'react'
import { CaretDownIcon, CaretUpIcon, MagnifyingGlassIcon, Plus } from '@phosphor-icons/react'
import { FontSize, TextStyle } from '@tiptap/extension-text-style'
import { doRequest } from '../../../../../requests/request'
import { showSimpleError } from '../../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../../requests/handler'
import VariableComboboxOptions from './VariableComboboxOptions'

const getVariableLabel = (variable: IMailVariableDto): string => {
  return `<react-component variable="${variable.label}" group="${variable.group}"></react-component>`
}

// Function to convert HTML react-component tags to template variables
const convertHtmlToTemplateVariables = (
  html: string,
  templateVariables: IMailVariableDto[],
): string => {
  for (const variable of templateVariables) {
    const reactComponentTag = getVariableLabel(variable)
    html = html.replaceAll(reactComponentTag, variable.templateVariable)
  }
  return html
}

// Function to convert template variables back to HTML react-component tags
const convertTemplateVariablesToHtml = (
  text: string,
  templateVariables: IMailVariableDto[],
): string => {
  for (const variable of templateVariables) {
    const reactComponentTag = getVariableLabel(variable)
    text = text.replaceAll(variable.templateVariable, reactComponentTag)
  }

  return text
}

const convertExample = (text: string, templateVariables: IMailVariableDto[]): string => {
  for (const variable of templateVariables) {
    text = text.replaceAll(getVariableLabel(variable), variable.example)
  }
  return text
}

interface IEmailTextEditorProps {
  editingTemplate?: IEmailTemplate | null
  setEditingTemplate?: (template: IEmailTemplate | null) => void
  setExampleText?: (text: string) => void
  stickyOffset?: number
}

const EmailTextEditor = ({
  editingTemplate,
  setEditingTemplate,
  setExampleText,
  stickyOffset = 0,
}: IEmailTextEditorProps) => {
  const [templateVariables, setTemplateVariables] = useState<IMailVariableDto[]>([])

  const editor = useEditor(
    {
      extensions: [
        StarterKit,
        Underline,
        Link,
        TextAlign.configure({ types: ['heading', 'paragraph', 'div'] }),
        ReactComponent.configure({ selectableVariables: templateVariables }),
        TextStyle,
        FontSize,
      ],
      content: convertTemplateVariablesToHtml(editingTemplate?.bodyHtml ?? '', templateVariables),
      onCreate: ({ editor }) => {
        setExampleText && setExampleText(convertExample(editor.getHTML(), templateVariables))
      },
      onUpdate: ({ editor }) => {
        setEditingTemplate &&
          setEditingTemplate({
            ...editingTemplate!,
            bodyHtml: convertHtmlToTemplateVariables(editor.getHTML(), templateVariables),
          })

        setExampleText && setExampleText(convertExample(editor.getHTML(), templateVariables))
      },
    },
    [templateVariables],
  )

  // Update editor content if editingTemplate.bodyHtml changes
  useEffect(() => {
    if (
      editor &&
      editingTemplate &&
      convertTemplateVariablesToHtml(editingTemplate.bodyHtml ?? '', templateVariables) !==
        editor.getHTML()
    ) {
      editor.commands.setContent(
        convertTemplateVariablesToHtml(editingTemplate.bodyHtml ?? '', templateVariables),
      )
    }
  }, [editingTemplate?.bodyHtml, editor, templateVariables])

  const fetchTemplateVariables = async () => {
    try {
      const response = await doRequest<IMailVariableDto[]>(
        `/v2/email-templates/${editingTemplate?.id}/variables`,
        {
          method: 'GET',
          requiresAuth: true,
        },
      )
      if (response.ok) {
        setTemplateVariables(response.data)
      } else {
        showSimpleError(getApiResponseErrorMessage(response))
      }
    } catch (error) {
      showSimpleError('Failed to fetch template variables')
    }
  }

  useEffect(() => {
    if (editingTemplate) {
      fetchTemplateVariables()
    }
  }, [editingTemplate?.id])

  return (
    <RichTextEditor editor={editor}>
      <RichTextEditor.Toolbar sticky stickyOffset={stickyOffset}>
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

        <InsertVariableButton selectableVariables={templateVariables} />
      </RichTextEditor.Toolbar>

      <RichTextEditor.Content />
    </RichTextEditor>
  )
}

interface IInsertVariableButtonProps {
  selectableVariables?: IMailVariableDto[]
}

function InsertVariableButton({ selectableVariables = [] }: IInsertVariableButtonProps) {
  const { editor } = useRichTextEditorContext()

  const insertVariable = (variable: IMailVariableDto) => {
    editor?.commands.insertContent(getVariableLabel(variable))
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
    <Combobox
      store={combobox}
      width={250}
      position='bottom-start'
      onOptionSubmit={(val) => {
        const variable = selectableVariables.find((v) => v.templateVariable === val)
        if (variable) {
          insertVariable(variable)
        }
      }}
      offset={4}
      shadow='md'
    >
      <Combobox.Target withAriaAttributes={false}>
        <RichTextEditor.Control
          aria-label='Insert variable'
          title='Insert variable'
          onClick={() => combobox.toggleDropdown()}
          variant='default'
        >
          <Group gap={5} p={5}>
            <Plus size={14} />
            Add variable
          </Group>
        </RichTextEditor.Control>
      </Combobox.Target>

      <Combobox.Dropdown>
        <VariableComboboxOptions
          selectableVariables={selectableVariables}
          search={search}
          setSearch={setSearch}
        />
      </Combobox.Dropdown>
    </Combobox>
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
    editor.on('transaction', sync)

    return () => {
      editor.off('selectionUpdate', sync)
      editor.off('transaction', sync)
    }
  }, [editor])

  const apply = (next: string) => {
    if (!editor) return
    setValue(next)

    if (next === '') {
      editor.chain().focus().unsetFontSize().run()
    } else {
      editor.chain().focus().setFontSize(next).run()
    }
  }

  const combobox = useCombobox({
    onDropdownClose: () => {
      combobox.resetSelectedOption()
      combobox.focusTarget()
    },
  })

  return (
    <Combobox
      store={combobox}
      width={100}
      position='bottom-start'
      shadow='md'
      withinPortal
      onOptionSubmit={(val) => {
        apply(val) // val can be '' for Default
        combobox.closeDropdown()
      }}
    >
      <Combobox.Target withAriaAttributes={false}>
        <RichTextEditor.Control
          aria-label='Font size'
          title='Font size'
          onClick={() => combobox.toggleDropdown()}
          variant='default'
        >
          <Group gap={6} px={6}>
            {value ? value.replace('px', '') : 'Size'}
            {combobox.dropdownOpened ? <CaretUpIcon size={12} /> : <CaretDownIcon size={12} />}
          </Group>
        </RichTextEditor.Control>
      </Combobox.Target>

      <Combobox.Dropdown>
        <Combobox.Options mah={200} style={{ overflowY: 'auto' }}>
          {data.length > 0 ? (
            data.map((item) => (
              <Combobox.Option key={item.value || 'default'} value={item.value}>
                {item.label}
              </Combobox.Option>
            ))
          ) : (
            <Combobox.Empty>Nothing found</Combobox.Empty>
          )}
        </Combobox.Options>
      </Combobox.Dropdown>
    </Combobox>
  )
}

export default EmailTextEditor
