import { computed } from "vue";
import { useAuthStore } from "../store/useAuthStore";
import type { Role } from "../types/types";
import { router } from "@/app/router";
import { logoutAuthSession } from "@/shared/auth/session/authSession";

export function useAuth() {
    const store = useAuthStore()

    const user = computed(() => store.user)
    const isAuthenticated = computed(() => store.isAuthenticated)

    const hasRole = (role: Role) => store.user?.roles.includes(role) ?? false

    const logout = async () => {
        try {
            await logoutAuthSession('MANUAL')
        } finally {
            store.clearAuth()
            await router.push('/login')
        }
    }

    return {
        user,
        isAuthenticated,
        hasRole,
        logout,
    }
}
