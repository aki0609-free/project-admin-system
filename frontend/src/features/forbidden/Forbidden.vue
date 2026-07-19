<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="forbidden-wrapper">
    <v-card class="forbidden-card text-center pa-8" width="500" elevation="10">
      <v-icon size="72" color="error"> mdi-alert-circle </v-icon>

      <div class="text-h5 mt-4 font-weight-bold">アクセス権限がありません</div>

      <div class="text-body-2 mt-2 text-medium-emphasis">
        このページを閲覧する権限がありません。
      </div>

      <v-btn class="mt-6" color="primary" size="large" @click="goLogin"> ホームへ戻る </v-btn>
    </v-card>
  </div>
</template>

<style scoped>
.forbidden-wrapper {
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 20%, #6366f1 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, #3b82f6 0%, transparent 40%),
    linear-gradient(135deg, #1e293b, #0f172a);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ガラス風カード */
.forbidden-card {
  border-radius: 18px;
  backdrop-filter: blur(20px);
  background: rgba(255, 255, 255, 0.9);
}
</style>
