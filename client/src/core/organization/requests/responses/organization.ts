export interface IMinimalSchool {
  id: string
  name: string
  abbreviation: string
}

export interface IDepartment {
  id: string
  schoolId: string
  name: string
  abbreviation?: string
  active?: boolean
}

export interface ISchool extends IMinimalSchool {
  websiteUrl?: string
  thesisPortalUrl?: string
  active?: boolean
  departments?: IDepartment[]
}

export interface IStudyProgram {
  id: string
  key: string
  name: string
  active?: boolean
  school?: IMinimalSchool
}
