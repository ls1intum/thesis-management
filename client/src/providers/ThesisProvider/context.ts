import { IThesis } from '../../requests/responses/thesis'
import React from 'react'

export interface IThesisContext {
  thesis: IThesis | undefined | false
  updateThesis: (thesis: IThesis) => unknown
  access: {
    student: boolean
    supervisor: boolean
    examiner: boolean
  }
}

export const ThesisContext = React.createContext<IThesisContext | undefined>(undefined)
