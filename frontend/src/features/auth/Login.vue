<template>
  <v-container class="login-wrapper" fluid>
    <v-row class="fill-height" align="center" justify="center">
      <v-col cols="12" sm="8" md="4">

        <v-card class="pa-6 login-card" elevation="12">

          <!-- ロゴ -->
          <div class="text-center mb-6">
            <v-icon size="56" color="primary">mdi-shield-account</v-icon>
            <div class="text-h5 font-weight-bold mt-2">
              HR Analytics & Administration System
            </div>
            <div class="text-caption text-medium-emphasis">
              Login to HR Analytics & Administration System
            </div>
          </div>

          <!-- フォーム -->
          <v-text-field
            v-model="username"
            label="ユーザー名"
            variant="outlined"
            prepend-inner-icon="mdi-account"
            density="comfortable"
            class="mb-3"
          />

          <v-text-field
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            label="パスワード"
            variant="outlined"
            prepend-inner-icon="mdi-lock"
            :append-inner-icon="showPassword ? 'mdi-eye-off' : 'mdi-eye'"
            @click:append-inner="showPassword = !showPassword"
            density="comfortable"
            class="mb-4"
          />

          <v-alert
            v-if="errorMessage"
            type="error"
            density="compact"
            variant="tonal"
            class="mb-4"
          >
            {{ errorMessage }}
          </v-alert>

          <v-btn
            block
            size="large"
            color="primary"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            ログイン
          </v-btn>

        </v-card>

      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { useLogin } from "@/features/auth/composables/useLogin"

const username = ref("")
const password = ref("")
const showPassword = ref(false)

const { login, loading, errorMessage } = useLogin()

const handleLogin = () => {
  login(username.value, password.value)
}
</script>

<style scoped>

.login-wrapper {
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 20%, #6366f1 0%, transparent 40%),
    radial-gradient(circle at 80% 70%, #3b82f6 0%, transparent 40%),
    linear-gradient(135deg, #1e293b, #0f172a);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  border-radius: 16px;
  backdrop-filter: blur(10px);
  margin-bottom: 50px;
}

.login-btn {
  font-weight: 600;
  letter-spacing: 0.5px;
}

</style>