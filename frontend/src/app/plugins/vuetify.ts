import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'
import { createVuetify } from 'vuetify'
import { aliases, mdi } from 'vuetify/iconsets/mdi'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { ja } from 'vuetify/locale'

export const vuetify = createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'light',
  },
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: { mdi },
  },
  locale: {
    locale: 'ja',
    fallback: 'ja',
    messages: { ja },
  },
  date: {
    locale: {
      ja: 'ja-JP',
    },
  },
})
