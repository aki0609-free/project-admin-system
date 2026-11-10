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
import { AUTH_SESSION_EXPIRED_EVENT } from './shared/auth/session/authSession'

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
window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, () => {
  authStore.clearAuth()
  if (router.currentRoute.value.path !== '/login') {
    void router.push({ path: '/login', query: { reason: 'session-expired' } })
  }
})
authStore.loadAuth()
await authStore.initAuth()


app.mount('#app')
