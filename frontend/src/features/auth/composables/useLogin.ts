import { ref } from "vue"
import { useRouter } from "vue-router"
import { loginApi } from "../api/loginApi"
import { useAuthStore } from "@/shared/auth/store/useAuthStore"

export function useLogin() {

  const router = useRouter()
  const authStore = useAuthStore()

  const loading = ref(false)
  const errorMessage = ref("")

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

      console.log(data.user);

      router.push("/")

    } catch {

      errorMessage.value = "ログインに失敗しました"

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