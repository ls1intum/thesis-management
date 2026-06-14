export interface IResearchGroupSettings {
  rejectSettings: IResearchGroupSettingsReject
  presentationSettings: IResearchGroupSettingsPresentation
  phaseSettings: IResearchGroupSettingsPhase
  emailSettings: IResearchGroupSettingsEmail
  writingGuideSettings: IResearchGroupSettingsWritingGuide
  applicationEmailSettings: IResearchGroupSettingsApplicationEmail
  gradingSchemeSettings?: IResearchGroupSettingsGradingScheme
}

export interface IResearchGroupSettingsReject {
  automaticRejectEnabled: boolean
  rejectDuration: number
}

export interface IResearchGroupSettingsPresentation {
  presentationSlotDuration: number
}

export interface IResearchGroupSettingsPhase {
  proposalPhaseActive: boolean
}

export interface IResearchGroupSettingsEmail {
  applicationNotificationEmail?: string | null
}

export interface IResearchGroupSettingsWritingGuide {
  scientificWritingGuideLink?: string | null
}

export interface IResearchGroupSettingsApplicationEmail {
  includeApplicationDataInEmail: boolean
}

export interface IGradingSchemeComponent {
  componentId?: string
  name: string
  weight: number
  isBonus: boolean
  position: number
}

export interface IResearchGroupSettingsGradingScheme {
  components: IGradingSchemeComponent[]
}
