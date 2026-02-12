export interface IMinimalUser {
  userId: string
  firstName: string | null
  lastName: string | null
  avatar: string | null
}

export interface ILightUser extends IMinimalUser {
  universityId: string
  matriculationNumber: string | null
  email: string | null
  studyDegree: string | null
  studyProgram: string | null
  customData: Record<string, string> | null
  joinedAt: string
  groups?: string[]
}

export interface IUser extends ILightUser {
  researchGroupName: string | null
  researchGroupId: string | null
  gender: string | null
  nationality: string | null
  projects: string | null
  interests: string | null
  specialSkills: string | null
  enrolledAt: string | null
  updatedAt: string
  hasCv: boolean
  hasDegreeReport: boolean
  hasExaminationReport: boolean
}
