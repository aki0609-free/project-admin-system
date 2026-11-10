import { ref } from "vue"
import { useRouter } from "vue-router"
import axios from "axios"
import { loginApi } from "../api/loginApi"
import { useAuthStore } from "@/shared/auth/store/useAuthStore"

export function useLogin() {

  const router = useRouter()
  const authStore = useAuthStore()

  const loading = ref(false)
  const errorMessage = ref(
    new URLSearchParams(window.location.search).get('reason') === 'session-expired'
      ? 'セッションの有効期限が切れました。再度ログインしてください。'
      : '',
  )

  const login = async (username: string, password: string) => {

    loading.value = true
    errorMessage.value = ""

    try {

      const { data } = await loginApi(username, password)

      authStore.setAuth({
        user: data.user,
        accessToken: data.accessToken,
        refreshToken: data.refreshToken
      })

      router.push("/")

    } catch (error: unknown) {

      errorMessage.value = axios.isAxiosError(error) && error.response?.status === 401
        ? 'ユーザー名またはパスワードが正しくありません。'
        : 'ログインに失敗しました。時間をおいて再度お試しください。'

    } finally {

      loading.value = false

    }
  }

  return {
    login,
    loading,
    errorMessage
  }
}
