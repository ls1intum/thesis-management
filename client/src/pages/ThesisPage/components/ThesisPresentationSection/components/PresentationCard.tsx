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
  CheckIcon,
  DotsThreeVerticalIcon,
  GlobeSimpleIcon,
  MapPinIcon,
  NotepadIcon,
  NotePencilIcon,
  PlusIcon,
  TrashIcon,
  WarningCircleIcon,
  WebcamIcon,
} from '@phosphor-icons/react'
import { useThesisUpdateAction } from '../../../../../providers/ThesisProvider/hooks'
import { doRequest } from '../../../../../requests/request'
import { ApiError } from '../../../../../requests/handler'
import { hasAdvisorAccess } from '../../../../../utils/thesis'
import { useUser } from '../../../../../hooks/authentication'
import { useState } from 'react'
import ReplacePresentationModal from '../../../../../components/PresentationsTable/components/ReplacePresentationModal/ReplacePresentationModal'
import SchedulePresentationModal from '../../../../../components/PresentationsTable/components/SchedulePresentationModal/SchedulePresentationModal'

interface IPresentationCardProps {
  presentation: IThesisPresentation
  thesis: IThesis
  thesisName?: string
  thesisType?: string
}

const PresentationCard = ({
  presentation,
  thesis,
  thesisName,
  thesisType,
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

  const user = useUser()

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
            <Group gap={'0.5rem'}>
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
            </Group>
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
          <Divider />
          {presentation.state !== 'DRAFTED' ? (
            <Group justify='space-between' gap={'0.5rem'} align='center'>
              {getThesisInfoItem(<NotepadIcon color={'gray'} weight='bold' />, 'Presentation Note')}
              <Button variant='default' size='xs' c={'gray'} leftSection={<PlusIcon size={14} />}>
                Add
              </Button>
            </Group>
          ) : (
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
          )}
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
