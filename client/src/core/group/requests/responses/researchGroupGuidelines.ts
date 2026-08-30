export type GuidelinesStatus = 'ready' | 'failed'

export interface ICategoryGuidelines {
  category: string
  displayName: string
  rules?: string[]
}

/**
 * A research group's custom AI review guidelines. Mirrors the server `GuidelinesDTO`. Fields are
 * omitted by the server when empty (`@JsonInclude(NON_EMPTY)`), so most are optional.
 */
export interface IResearchGroupGuidelines {
  status: GuidelinesStatus
  rawGuidelines?: string
  overview?: string
  categories?: ICategoryGuidelines[]
  failureReason?: string
  processedAt?: string
  updatedAt?: string
}
