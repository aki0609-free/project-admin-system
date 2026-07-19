import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { vuetify } from './app/plugins/vuetify'
import { router } from './app/router'

import 'vuetify/styles'
import { queryPlugin } from './app/plugins/query'
import { useAuthStore } from './shared/auth/store/useAuthStore'
import { setupMonacoWorkers } from './app/monaco/setupMonacoWorkers'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(vuetify)
app.use(queryPlugin)

setupMonacoWorkers()

const authStore = useAuthStore()
authStore.loadAuth()
await authStore.initAuth()


app.mount('#app')
