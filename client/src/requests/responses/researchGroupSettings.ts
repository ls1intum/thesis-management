export interface IResearchGroupSettings {
  rejectSettings: IResearchGroupSettingsReject
  presentationSettings: IResearchGroupSettingsPresentation
  phaseSettings: IResearchGroupSettingsPhase
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
