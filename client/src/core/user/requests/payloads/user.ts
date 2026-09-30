export interface IUpdateUserInformationPayload {
  firstName: string
  lastName: string
  gender: string
  nationality: string
  email: string
  studyDegree: string
  studyProgramId: string
  enrolledAt: Date | null
  specialSkills: string
  interests: string
  projects: string
  customData: Record<string, string>
}
