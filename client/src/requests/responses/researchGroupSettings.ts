export interface IResearchGroupSettings {
  rejectSettings: IResearchGroupSettingsReject
  presentationSettings: IResearchGroupSettingsPresentation
  phaseSettings: IResearchGroupSettingsPhase
  emailSettings: IResearchGroupSettingsEmail
  writingGuideSettings: IResearchGroupSettingsWritingGuide
  applicationEmailSettings: IResearchGroupSettingsApplicationEmail
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
  emailSignature?: string | null
}

export interface IResearchGroupSettingsWritingGuide {
  scientificWritingGuideLink?: string | null
}

export interface IResearchGroupSettingsApplicationEmail {
  includeApplicationDataInEmail: boolean
}
