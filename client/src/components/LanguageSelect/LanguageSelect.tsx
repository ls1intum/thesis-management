import { GLOBAL_CONFIG } from '../../config/global'
import { Select } from '@mantine/core'
import { formatLanguage } from '../../utils/format'
import type { SelectProps } from '@mantine/core'

const LanguageSelect = (props: Omit<SelectProps, 'data'>) => {
  return (
    <Select
      data={Object.keys(GLOBAL_CONFIG.languages).map((key) => ({
        label: formatLanguage(key),
        value: key,
      }))}
      {...props}
    />
  )
}

export default LanguageSelect
