export interface IResearchGroupSettings {
  rejectSettings: IResearchGroupSettingsReject
  presentationSettings: IResearchGroupSettingsPresentation
  phaseSettings: IResearchGroupSettingsPhase
  emailSettings: IResearchGroupSettingsEmail
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
