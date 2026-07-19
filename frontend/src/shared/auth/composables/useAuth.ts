import { computed } from "vue";
import { useAuthStore } from "../store/useAuthStore";
import type { AuthUser, Role } from "../types/types";
import { router } from "@/app/router";

export function useAuth() {
    const store = useAuthStore()

    const user = computed(() => store.user)
    const isAuthenticated = computed(() => store.isAuthenticated)

    const hasRole = (role: Role) => store.user?.roles.includes(role) ?? false

    const logout = () => {
        store.clearAuth()
        router.push('/login')
    }

    return {
        user,
        isAuthenticated,
        hasRole,
        logout,
    }
}