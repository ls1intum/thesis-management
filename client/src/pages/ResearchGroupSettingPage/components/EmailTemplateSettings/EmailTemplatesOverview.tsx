import { useEffect, useState } from 'react'
import { getApiResponseErrorMessage } from '../../../../requests/handler'
import { doRequest } from '../../../../requests/request'
import { IEmailTemplate } from '../../../../requests/responses/emailtemplate'
import { PaginationResponse } from '../../../../requests/responses/pagination'
import { showSimpleError } from '../../../../utils/notification'
import { Box, Divider, Flex, SimpleGrid, Stack, TextInput, Text } from '@mantine/core'
import { ResearchGroupSettingsCard } from '../ResearchGroupSettingsCard'
import EmailTemplateCard from './EmailTemplateCard'
import { useSearchParams } from 'react-router'
import { MagnifyingGlass, Spinner } from '@phosphor-icons/react'

const EmailTemplatesOverview = () => {
  const [loading, setLoading] = useState(true)
  const [emailTemplates, setEmailTemplates] = useState<
    Record<
      string,
      Record<
        string,
        { default: IEmailTemplate | null; researchGroupTemplate: IEmailTemplate | null }
      >
    >
  >({})

  //Used to not refetch and filter on every key stroke
  const [displayEmailTemplates, setDisplayEmailTemplates] = useState<
    Record<
      string,
      Record<
        string,
        { default: IEmailTemplate | null; researchGroupTemplate: IEmailTemplate | null }
      >
    >
  >({})

  const [searchParams, setSearchParams] = useSearchParams()
  const [searchKey, setSearchKey] = useState(searchParams.get('search') ?? '')

  const getEmailTemplateCategory = (templateCase: string) => {
    switch (true) {
      case templateCase.startsWith('THESIS_PROPOSAL'):
        return 'Thesis Proposal'
      case templateCase.startsWith('THESIS_PRESENTATION'):
        return 'Thesis Presentation'
      case templateCase.startsWith('THESIS_FINAL'):
        return 'Thesis Finalaization'
      case templateCase.startsWith('APPLICATION_REJECTED'):
        return 'Application Rejected'
      case templateCase.startsWith('APPLICATION_ACCEPTED'):
        return 'Application Acceptance'
      case templateCase.startsWith('APPLICATION_'):
        return 'Application Open'
      case templateCase.startsWith('THESIS_'):
        return 'Thesis Lifecycle'
      default:
        return 'Others'
    }
  }

  const emailTemplateContainsSearchKey = (
    template: { default: IEmailTemplate | null; researchGroupTemplate: IEmailTemplate | null },
    searchKey: string,
    category: string,
  ) => {
    const lowerKey = searchKey.toLowerCase()

    return (
      template.default?.description.toLowerCase().includes(lowerKey) ||
      template.default?.subject.toLowerCase().includes(lowerKey) ||
      template.default?.templateCase.toLowerCase().includes(lowerKey) ||
      category.toLowerCase().includes(lowerKey)
    )
  }

  useEffect(() => {
    if (searchKey.trim() === '') {
      setDisplayEmailTemplates(emailTemplates)
    } else {
      const filtered: Record<
        string,
        Record<
          string,
          { default: IEmailTemplate | null; researchGroupTemplate: IEmailTemplate | null }
        >
      > = {}

      Object.entries(emailTemplates).forEach(([category, templates]) => {
        Object.entries(templates).forEach(([templateCase, template]) => {
          if (emailTemplateContainsSearchKey(template, searchKey, category)) {
            if (!filtered[category]) {
              filtered[category] = {}
            }
            filtered[category][templateCase] = template
          }
        })
      })

      setDisplayEmailTemplates(filtered)
    }
  }, [searchKey, emailTemplates])

  useEffect(() => {
    doRequest<PaginationResponse<IEmailTemplate>>(
      '/v2/email-templates',
      {
        method: 'GET',
        requiresAuth: true,
        params: {
          page: 0,
          limit: -1,
        },
      },
      (res) => {
        if (res.ok) {
          const grouped: Record<
            string,
            Record<
              string,
              { default: IEmailTemplate | null; researchGroupTemplate: IEmailTemplate | null }
            >
          > = {}

          res.data.content.forEach((template) => {
            const category = getEmailTemplateCategory(template.templateCase)
            const key = template.templateCase

            // Initialize category if it doesn't exist
            if (!grouped[category]) {
              grouped[category] = {}
            }

            // Initialize the specific template key within the category
            if (!grouped[category][key]) {
              grouped[category][key] = { default: null, researchGroupTemplate: null }
            }

            // Populate the template data
            if (template.researchGroup) {
              grouped[category][key].researchGroupTemplate = template
            } else {
              grouped[category][key].default = template
            }
          })

          setEmailTemplates(grouped)
          setLoading(false)
        } else {
          showSimpleError(getApiResponseErrorMessage(res))
          setEmailTemplates({})
          setLoading(false)
        }
      },
    )
  }, [])
  return (
    <ResearchGroupSettingsCard
      title={'Email Templates'}
      subtle={'Manage email templates for this research group.'}
      children={
        <Stack>
          <Flex
            justify='space-between'
            align='stretch'
            gap='md'
            direction={{ base: 'column', sm: 'row' }}
          >
            <Box style={{ flex: 1 }}>
              <TextInput
                w='100%'
                placeholder='Search Email Template...'
                leftSection={<MagnifyingGlass size={16} />}
                value={searchKey}
                onChange={(x) => {
                  setSearchKey(x.target.value || '')
                  const params = new URLSearchParams(searchParams)

                  if (x.target.value) {
                    params.set('search', x.target.value)
                  } else {
                    params.delete('search')
                  }

                  setSearchParams(params, { replace: true })
                }}
              />
            </Box>
          </Flex>

          {emailTemplates && !loading ? (
            <Stack>
              {Object.entries(displayEmailTemplates)
                .map(([category, templates]) => (
                  <Stack key={category}>
                    <Divider label={<Text>{category}</Text>} labelPosition='center' my='sm' />
                    <SimpleGrid
                      cols={{ base: 1, sm: 2, xl: 3 }}
                      spacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
                      verticalSpacing={{ base: 'xs', sm: 'sm', xl: 'md' }}
                    >
                      {Object.entries(templates).map(([templateCase, template]) => (
                        <EmailTemplateCard
                          key={`${category}-${templateCase}`}
                          emailTemplate={template}
                        />
                      ))}
                    </SimpleGrid>
                  </Stack>
                ))
                .flat()}
            </Stack>
          ) : (
            <Spinner />
          )}
        </Stack>
      }
    />
  )
}

export default EmailTemplatesOverview
