import { defineStore } from "pinia";
import { computed, ref } from "vue";
import type { AuthUser } from "../types/types";
import axiosApiClient from "@/app/plugins/axiosApiClient";
import {
    clearStoredAuth,
    getAccessToken,
    getRefreshToken,
    saveAuthTokens,
} from "@/shared/auth/session/authSession";

export const useAuthStore = defineStore('auth', () => {
    const user = ref<AuthUser | null>(null)
    const accessToken = ref<string | null>(null)
    const refreshToken = ref<string | null>(null)
    const authReady = ref(false)
    let initPromise: Promise<void> | null = null

    const isAuthenticated = computed(() => !!accessToken.value)

    function setAuth(data: {
        user: AuthUser,
        accessToken: string,
        refreshToken: string
    }) {
        user.value = data.user ?? null
        accessToken.value = data.accessToken
        refreshToken.value = data.refreshToken

        saveAuthTokens(data.accessToken, data.refreshToken)
    }

    function loadAuth() {
        const at = getAccessToken()
        const rt = getRefreshToken()

        if (at) accessToken.value = at
        if (rt) refreshToken.value = rt
    }

    function clearAuth() {
        user.value = null
        accessToken.value = null
        refreshToken.value = null

        clearStoredAuth()
    }

    async function initAuth() {
        if (authReady.value) return
        if (!initPromise) {
            initPromise = (async () => {
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
            })()
        }

        try {
            await initPromise
        } finally {
            if (authReady.value) {
                initPromise = null
            }
        }
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
