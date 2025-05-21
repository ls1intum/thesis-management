import { Badge, Box, Card, Flex, Group, Stack, Text } from '@mantine/core'
import { Buildings, Users } from 'phosphor-react'
import React, { useEffect } from 'react'
import { IResearchGroup } from '../../../requests/responses/researchGroup'
import CustomAvatar from '../../../components/CustomAvatar/CustomAvatar'
import { formatUser } from '../../../utils/format'
import { useNavigate } from 'react-router'
import { doRequest } from '../../../requests/request'
import { ILightUser } from '../../../requests/responses/user'

const ResearchGroupCard = (props: IResearchGroup) => {
  const navigate = useNavigate()

  const onResearchGroupCardClick = () => {
    navigate(`/research-groups/${props.id}`)
  }

  const [researchGroupMemberNumber, setResearchGroupMemberNumber] = React.useState(0)

  const fetchMemberAmount = () => {
    //TODO: Add this information maybe into the getAllResearchGroups call
    doRequest<{ content: ILightUser[] }>(
      `/v2/research-groups/${props.id}/members`,
      {
        method: 'GET',
        requiresAuth: true,
      },
      (res) => {
        if (res.ok) {
          setResearchGroupMemberNumber(res.data.content.length)
        }
      },
    )
  }

  useEffect(() => {
    fetchMemberAmount()
  }, [])

  return (
    <Card
      withBorder
      shadow='sm'
      radius='md'
      h='100%'
      w='100%'
      style={{ display: 'flex', flexDirection: 'column' }}
      onClick={onResearchGroupCardClick}
    >
      <Flex
        justify={{ base: 'flex-start', sm: 'space-between' }}
        align={{ base: 'flex-start', sm: 'center' }}
        gap={{ sm: 'md' }}
        direction={{ base: 'column', sm: 'row' }}
        h={{ sm: 60 }}
      >
        <Text fw={500} lineClamp={2}>
          {props.name}
        </Text>
        {
          <Badge leftSection={<Users />} variant='outline' mb={5} my={5}>
            {researchGroupMemberNumber}
          </Badge>
        }
      </Flex>

      <Stack gap={5} style={{ flexGrow: 1 }}>
        <Text size='sm' c='dimmed' py={5}>
          <Flex justify={'start'} align={'center'} gap={5}>
            <Buildings size={16} />
            {props.campus ? props.campus : 'No campus specified'}
          </Flex>
        </Text>

        <Text size='sm' c='dimmed'>
          {props.description ? props.description : 'No description provided'}
        </Text>

        <Box style={{ flexGrow: 1 }} />
      </Stack>

      <Group pt={5}>
        <CustomAvatar user={props.head} size={35} />
        <Stack gap={0}>
          <Text size='sm'>{formatUser(props.head)}</Text>
          <Text size='xs' c='dimmed'>
            Group Head
          </Text>
        </Stack>
      </Group>
    </Card>
  )
}

export default ResearchGroupCard
