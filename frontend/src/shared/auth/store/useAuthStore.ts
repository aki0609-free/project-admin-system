import { defineStore } from "pinia";
import { computed, ref } from "vue";
import { AuthUser } from "../types/types";
import axiosApiClient from "@/app/plugins/axiosApiClient";

export const useAuthStore = defineStore('auth', () => {
    const user = ref<AuthUser | null>(null)
    const accessToken = ref<string | null>(null)
    const refreshToken = ref<string | null>(null)
    const authReady = ref(false)

    const isAuthenticated = computed(() => !!accessToken.value)

    function setAuth(data: {
        user: AuthUser,
        accessToken: string,
        refreshToken: string
    }) {
        user.value = data.user ?? null
        accessToken.value = data.accessToken
        refreshToken.value = data.refreshToken

        localStorage.setItem("accessToken", data.accessToken)
        localStorage.setItem("refreshToken", data.refreshToken)
    }

    function loadAuth() {
        const at = localStorage.getItem("accessToken")
        const rt = localStorage.getItem("refreshToken")

        if (at) accessToken.value = at
        if (rt) refreshToken.value = rt
    }

    function clearAuth() {
        user.value = null
        accessToken.value = null
        refreshToken.value = null

        localStorage.clear()
    }

    async function initAuth() {
        loadAuth()

        if (accessToken.value) {
            try {
                const res = await axiosApiClient.get("/auth/me")
                user.value = res.data
            } catch {
                clearAuth()
            }
        }
        authReady.value = true
    }

    return {
        user,
        accessToken,
        refreshToken,
        isAuthenticated,
        authReady,
        setAuth,
        loadAuth,
        clearAuth,
        initAuth,
    }
})