import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { vuetify } from './app/plugins/vuetify'
import { router } from './app/router'

import 'vuetify/styles'
import { queryPlugin } from './app/plugins/query'
import { useAuthStore } from './shared/auth/store/useAuthStore'
import { setupMonacoWorkers } from './app/monaco/setupMonacoWorkers'
import { configureSyncfusion } from './app/plugins/syncfusion'

// Syncfusionコンポーネントが初回描画される前にライセンスを登録する。
// 各画面での遅延登録では、開発環境で試用版バナーが先に生成される場合がある。
configureSyncfusion()

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
