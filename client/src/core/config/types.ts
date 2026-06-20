export type Environment = 'production' | 'staging' | 'test' | 'dev'

export interface IGlobalConfig {
  title: string

  chair_name: string
  chair_url: string

  environment?: Environment

  allow_suggested_topics: boolean

  server_host: string
  passkey_rp_id: string
  passkey_rp_name: string
  passkey_prompt_apps: string[]

  genders: Record<string, string>
  study_programs: Record<string, string>
  study_degrees: Record<string, string>
  languages: Record<string, string>

  topic_views_options: Record<string, string>

  research_groups_location: Record<string, string>

  thesis_types: Record<
    string,
    {
      long: string
      short: string
    }
  >

  custom_data: Record<
    string,
    {
      label: string
      required: boolean
    }
  >

  thesis_files: Record<
    string,
    {
      label: string
      description: string
      accept: UploadFileType
      required: boolean
    }
  >

  keycloak: {
    client_id: string
    realm: string
    host: string
  }
}

export type UploadFileType = 'pdf' | 'image' | 'any'
