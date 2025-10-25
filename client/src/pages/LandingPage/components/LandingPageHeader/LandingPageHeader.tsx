import { Card, Title, Flex, useComputedColorScheme } from '@mantine/core'
import LogoCircle from './LogoCircle'
import { useParams } from 'react-router'
import { useEffect, useState } from 'react'
import { IResearchGroup } from '../../../../requests/responses/researchGroup'
import { doRequest } from '../../../../requests/request'
import { showSimpleError } from '../../../../utils/notification'
import { getApiResponseErrorMessage } from '../../../../requests/handler'

interface LandingPageHeaderProps {
  researchGroupId?: string
}

const LandingPageHeader = ({ researchGroupId }: LandingPageHeaderProps) => {
  const computedColorScheme = useComputedColorScheme()

  const [researchGroup, setResearchGroup] = useState<IResearchGroup | undefined>()

  useEffect(() => {
    if (!researchGroupId) return

    return doRequest<IResearchGroup>(
      `/v2/research-groups/${researchGroupId}`,
      {
        method: 'GET',
        requiresAuth: false,
      },
      (response) => {
        if (response.ok) {
          setResearchGroup(response.data)
        } else {
          showSimpleError(getApiResponseErrorMessage(response))
        }
      },
    )
  }, [researchGroupId])

  return (
    <Card
      radius='md'
      bg={computedColorScheme === 'dark' ? 'dark.6' : 'gray.1'}
      p='xl'
      style={{ flexShrink: 0 }}
    >
      <Flex justify='flex-start' align='center' gap='xl' wrap='nowrap'>
        <Flex direction='column' gap='xs' flex={1}>
          <Flex justify='space-between' align='flex-start' gap='xs'>
            <Title order={2}>{researchGroup ? researchGroup.name : 'Find a Thesis Topic'}</Title>
            <LogoCircle size={40} logoSize={30} hiddenFrom='sm' />
          </Flex>
          <Title order={5} c='dimmed'>
            {researchGroup
              ? researchGroup.description
              : 'Whether you are looking for inspiration or have a unique idea in mind, ' +
                'Thesis Management makes it easy. Explore topics posted by instructors or suggest your own.'}
          </Title>
        </Flex>

        <LogoCircle size={100} logoSize={80} visibleFrom='sm' />
      </Flex>
    </Card>
  )
}

export default LandingPageHeader
