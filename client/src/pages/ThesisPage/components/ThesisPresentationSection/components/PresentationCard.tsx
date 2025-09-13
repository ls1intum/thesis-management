import {
  Badge,
  Card,
  Group,
  Stack,
  Title,
  Text,
  Divider,
  Button,
  Menu,
  UnstyledButton,
  Modal,
  Alert,
  Accordion,
  Transition,
} from '@mantine/core'
import { IThesis, IThesisPresentation } from '../../../../../requests/responses/thesis'
import {
  formatDate,
  formatLanguage,
  formatPresentationState,
  formatPresentationType,
  formatThesisType,
} from '../../../../../utils/format'
import {
  CalendarBlankIcon,
  CaretDownIcon,
  CaretUpIcon,
  CheckIcon,
  DotsThreeVerticalIcon,
  FloppyDiskIcon,
  GlobeSimpleIcon,
  MapPinIcon,
  NotepadIcon,
  NotePencilIcon,
  PlusIcon,
  TrashIcon,
  WarningCircleIcon,
  WebcamIcon,
  XIcon,
} from '@phosphor-icons/react'
import { useThesisUpdateAction } from '../../../../../providers/ThesisProvider/hooks'
import { doRequest } from '../../../../../requests/request'
import { ApiError } from '../../../../../requests/handler'
import { hasAdvisorAccess } from '../../../../../utils/thesis'
import { useUser } from '../../../../../hooks/authentication'
import { useState } from 'react'
import ReplacePresentationModal from '../../../../../components/PresentationsTable/components/ReplacePresentationModal/ReplacePresentationModal'
import SchedulePresentationModal from '../../../../../components/PresentationsTable/components/SchedulePresentationModal/SchedulePresentationModal'
import DocumentEditor from '../../../../../components/DocumentEditor/DocumentEditor'

interface IPresentationCardProps {
  presentation: IThesisPresentation
  thesis: IThesis
  thesisName?: string
  thesisType?: string
  hasEditAccess: boolean
}

const PresentationCard = ({
  presentation,
  thesis,
  thesisName,
  thesisType,
  hasEditAccess,
}: IPresentationCardProps) => {
  const [deleting, deletePresentation] = useThesisUpdateAction(
    async (presentation: IThesisPresentation) => {
      const response = await doRequest<IThesis>(
        `/v2/theses/${presentation.thesisId}/presentations/${presentation.presentationId}`,
        {
          method: 'DELETE',
          requiresAuth: true,
        },
      )

      if (response.ok) {
        return response.data
      } else {
        throw new ApiError(response)
      }
    },
    'Presentation deleted successfully',
  )

  const [openDeleteModal, setOpenDeleteModal] = useState(false)

  const [editPresentationModal, setEditPresentationModal] = useState(false)
  const [schedulePresentationModal, setSchedulePresentationModal] = useState<
    IThesisPresentation | undefined
  >(undefined)

  const [presentationNoteOpen, setPresentationNoteOpen] = useState<boolean>(false)
  const [editMode, setEditMode] = useState(presentation.presentationNoteHtml ? false : true)

  const user = useUser()

  const [editingPresentationNote, setEditingPresentationNote] = useState<string>(
    presentation.presentationNoteHtml ?? '',
  )

  const [updating, updatePresentationNote] = useThesisUpdateAction(
    async (presentationId: string, note: string, onSuccess?: () => void) => {
      const response = await doRequest<IThesis>(
        `/v2/theses/${thesis.thesisId}/presentations/${presentationId}/note`,
        {
          method: 'PUT',
          requiresAuth: true,
          data: { note },
        },
      )

      if (response.ok) {
        onSuccess?.()
        return response.data
      } else {
        throw new ApiError(response)
      }
    },
    'Presentation note updated successfully',
  )

  const getPresentationColor = (state: string) => {
    if (state === 'SCHEDULED') {
      return 'violet'
    }

    return 'gray'
  }

  const getPresentationTypeColor = (type: string) => {
    if (type === 'FINAL') {
      return 'blue'
    }

    if (type === 'INTERMEDIATE') {
      return 'green'
    }

    return 'gray'
  }

  const getThesisInfoItem = (icon: React.ReactNode, text: string, link?: string) => {
    return (
      <Group gap={'0.25rem'}>
        {icon}
        {link ? (
          <a
            href={link}
            target='_blank'
            rel='noopener noreferrer'
            style={{ color: 'inherit', textDecoration: 'underline', wordBreak: 'break-all' }}
          >
            <Text size='sm' c={'gray'} fw={500}>
              {text.length > 30 ? text.slice(0, 30) + '...' : text}
            </Text>
          </a>
        ) : (
          <Text size='sm' c={'gray'} fw={500}>
            {text}
          </Text>
        )}
      </Group>
    )
  }

  const showNoteWhenEmpty = () => {
    if (!presentation.presentationNoteHtml || presentation.presentationNoteHtml === '') {
      return hasEditAccess
    } else {
      return true
    }
  }

  return (
    <Card
      withBorder
      shadow={'xs'}
      radius='md'
      h='100%'
      w='100%'
      bg={getPresentationColor(presentation.state)}
      p={0}
    >
      <Card radius='md' h='100%' w='100%' ml={5}>
        <Stack gap={'1rem'}>
          <Group justify='space-between' align={'flex-start'} gap={'0.5rem'} wrap='nowrap'>
            <Stack gap={'0.5rem'}>
              <Title order={5}>
                {thesisName ? thesisName : `${formatThesisType(thesisType)} Presentation`}
              </Title>
              <Group gap={'0.5rem'}>
                <Badge
                  radius='sm'
                  variant='light'
                  color={getPresentationColor(presentation.state)}
                  tt={'none'}
                >
                  {formatPresentationState(presentation.state)}
                </Badge>
                <Badge
                  radius='sm'
                  variant='light'
                  color={getPresentationTypeColor(presentation.type)}
                  tt={'none'}
                >
                  {formatPresentationType(presentation.type)}
                </Badge>
              </Group>
            </Stack>

            {hasEditAccess && (
              <Menu
                shadow='md'
                width={200}
                position='bottom-end'
                withArrow
                transitionProps={{ transition: 'scale-y', duration: 200 }}
              >
                <Menu.Target>
                  <UnstyledButton
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <DotsThreeVerticalIcon size={24} />
                  </UnstyledButton>
                </Menu.Target>

                <Menu.Dropdown>
                  <Menu.Item>
                    <Group
                      justify='flex-start'
                      align='center'
                      gap='xs'
                      onClick={() => setEditPresentationModal(true)}
                    >
                      <NotePencilIcon size={16} />
                      <Text size='xs'>Edit Presentation</Text>
                    </Group>
                  </Menu.Item>
                  {hasAdvisorAccess(thesis, user) && (
                    <Menu.Item>
                      <Group
                        justify='flex-start'
                        align='center'
                        gap='xs'
                        onClick={() => setOpenDeleteModal(true)}
                      >
                        <TrashIcon size={16} color='red' />
                        <Text size='xs' c={'red'}>
                          Delete Presentation
                        </Text>
                      </Group>
                    </Menu.Item>
                  )}
                </Menu.Dropdown>
              </Menu>
            )}
          </Group>
          <Group pt={'0.5rem'} gap={'0.5rem'}>
            {getThesisInfoItem(
              <CalendarBlankIcon color={'gray'} weight='bold' />,
              formatDate(presentation.scheduledAt, { withTime: true }),
            )}
            {presentation.location &&
              getThesisInfoItem(<MapPinIcon color={'gray'} weight='bold' />, presentation.location)}
            {presentation.streamUrl &&
              getThesisInfoItem(
                <WebcamIcon color={'gray'} weight='bold' />,
                presentation.streamUrl,
                presentation.streamUrl,
              )}
            {getThesisInfoItem(
              <GlobeSimpleIcon color={'gray'} weight='bold' />,
              formatLanguage(presentation.language),
            )}
          </Group>
          {showNoteWhenEmpty() && <Divider />}
          {presentation.state !== 'DRAFTED' && showNoteWhenEmpty() ? (
            <Stack>
              <Group justify={'space-between'} gap={'0.5rem'} align='center'>
                {getThesisInfoItem(
                  <NotepadIcon color={'gray'} weight='bold' />,
                  'Presentation Note',
                )}
                <Group gap={'0.5rem'} align='center'>
                  {presentationNoteOpen && !editMode && hasEditAccess && (
                    <Button
                      size='xs'
                      variant='outline'
                      c={'primary'}
                      leftSection={<NotePencilIcon size={14} />}
                      onClick={() => {
                        setEditMode(true)
                      }}
                    >
                      Edit
                    </Button>
                  )}
                  {editMode && presentationNoteOpen && hasEditAccess ? (
                    <>
                      <Button
                        variant='default'
                        size='xs'
                        c={'gray'}
                        leftSection={<XIcon size={14} />}
                        onClick={() => {
                          setEditingPresentationNote(presentation.presentationNoteHtml ?? '')
                          if (
                            !presentation.presentationNoteHtml ||
                            presentation.presentationNoteHtml === ''
                          ) {
                            setPresentationNoteOpen(false)
                          } else {
                            setEditMode(false)
                          }
                        }}
                      >
                        Cancel
                      </Button>
                      <Button
                        size='xs'
                        variant='outline'
                        c={'primary'}
                        leftSection={<FloppyDiskIcon size={14} />}
                        loading={updating}
                        onClick={() => {
                          updatePresentationNote(
                            presentation.presentationId,
                            editingPresentationNote,
                            () => {
                              if (editingPresentationNote && editingPresentationNote !== '') {
                                setEditMode(false)
                              } else {
                                setPresentationNoteOpen(false)
                              }
                            },
                          )
                        }}
                      >
                        Save
                      </Button>
                    </>
                  ) : (
                    <Button
                      variant='default'
                      size='xs'
                      c={'gray'}
                      leftSection={
                        presentation.presentationNoteHtml ? (
                          presentationNoteOpen ? (
                            <CaretUpIcon size={14} />
                          ) : (
                            <CaretDownIcon size={14} />
                          )
                        ) : (
                          <PlusIcon size={14} />
                        )
                      }
                      onClick={() => {
                        setPresentationNoteOpen(!presentationNoteOpen)
                      }}
                    >
                      {presentation.presentationNoteHtml
                        ? presentationNoteOpen
                          ? 'Hide'
                          : 'Show'
                        : 'Add presentation note'}
                    </Button>
                  )}
                </Group>
              </Group>
              <Transition mounted={presentationNoteOpen} transition='scale-y' duration={100}>
                {(styles) => (
                  <DocumentEditor
                    value={editingPresentationNote}
                    onChange={(e) => setEditingPresentationNote(e.target.value)}
                    editMode={editMode}
                    style={styles}
                  />
                )}
              </Transition>
            </Stack>
          ) : hasEditAccess ? (
            <Group justify={'flex-end'} gap={'0.5rem'} align='center'>
              <Button
                variant='outline'
                size='xs'
                color={'green'}
                leftSection={<CheckIcon size={14} />}
                onClick={() => setSchedulePresentationModal(presentation)}
              >
                Accept
              </Button>
            </Group>
          ) : undefined}
        </Stack>
      </Card>
      <Modal
        opened={openDeleteModal}
        onClose={() => setOpenDeleteModal(false)}
        title='Confirm Deletion'
        centered
      >
        <Stack>
          <Text>Are you sure you want to delete this presentation?</Text>

          <Alert
            variant='light'
            color='orange'
            title='Important'
            icon={<WarningCircleIcon size={16} />}
          >
            This action cannot be undone. Deleting the presentation will also remove the
            presentation note.
          </Alert>

          <Group justify='flex-end'>
            <Button
              variant='outline'
              color='red'
              onClick={() => {
                deletePresentation(presentation)
                setOpenDeleteModal(false)
              }}
            >
              Delete
            </Button>
          </Group>
        </Stack>
      </Modal>
      <ReplacePresentationModal
        thesis={thesis}
        presentation={presentation}
        opened={editPresentationModal}
        onClose={() => setEditPresentationModal(false)}
      />
      <SchedulePresentationModal
        presentation={schedulePresentationModal}
        onClose={() => setSchedulePresentationModal(undefined)}
      />
    </Card>
  )
}
export default PresentationCard
