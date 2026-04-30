import React from 'react'
import { Button, createTheme, MantineProvider } from '@mantine/core'
import { Notifications } from '@mantine/notifications'
import '../../public/favicon.svg'
import AppRoutes from './Routes'
import AuthenticationProvider from '../providers/AuthenticationContext/AuthenticationProvider'

import '@mantine/core/styles.layer.css'
import '@mantine/dates/styles.layer.css'
import '@mantine/notifications/styles.css'
import '@mantine/tiptap/styles.css'
import '@mantine/dropzone/styles.css'
import 'mantine-datatable/styles.layer.css'
import '@mantine/carousel/styles.css'

import * as buttonClasses from './styles/Buttons.module.css'

const theme = createTheme({
  respectReducedMotion: false,
  colors: {
    primary: [
      '#E7F5FF',
      '#D0EBFF',
      '#A5D8FF',
      '#74C0FC',
      '#4DABF7',
      '#339AF0',
      '#228BE6',
      '#1C7ED6',
      '#1971C2',
      '#1864AB',
      '#054378',
      '#001123',
    ],
  },
  primaryColor: 'primary',
  primaryShade: 7,
  components: {
    Button: Button.extend({
      classNames: buttonClasses,
    }),
  },
  breakpoints: {
    xxl: '88rem',
    '3xl': '120rem',
  },
})

const App = () => {
  return (
    <MantineProvider defaultColorScheme='auto' theme={theme}>
      <AuthenticationProvider>
        <AppRoutes />
        <Notifications limit={5} position='top-right' />
      </AuthenticationProvider>
    </MantineProvider>
  )
}

export default App
