export type PartialNull<T> = {
  [P in keyof T]: T[P] | null
}

export function isNotEmptyUserList(group: 'student' | 'advisor' | 'supervisor') {
  return (values: string[]) => {
    if (values.length === 0) {
      return `You must select at least one ${group}`
    }
  }
}
