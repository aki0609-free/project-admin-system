import axios, { type InternalAxiosRequestConfig } from "axios";
import {
    expireAuthSession,
    getAccessToken,
    isAuthEndpoint,
    refreshAccessToken,
} from "@/shared/auth/session/authSession";

type RetryableAxiosConfig = InternalAxiosRequestConfig & {
    _authRetry?: boolean
}

const axiosApiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL
});

axiosApiClient.interceptors.request.use((config) => {

    const token = getAccessToken()

    if (token && !isAuthEndpoint(config.url ?? "")) {
        config.headers.set('Authorization', `Bearer ${token}`)
    } else {
        config.headers.delete('Authorization')
    }

    config.headers["X-Tenant-ID"] = "default"

    return config;
})

axiosApiClient.interceptors.response.use(
    (response) => response,
    async (error: unknown) => {
        if (!axios.isAxiosError(error) || !error.config) {
            return Promise.reject(error)
        }

        const config = error.config as RetryableAxiosConfig
        if (
            error.response?.status !== 401 ||
            config._authRetry ||
            isAuthEndpoint(config.url ?? "")
        ) {
            return Promise.reject(error)
        }

        config._authRetry = true
        const accessToken = await refreshAccessToken()
        if (!accessToken) {
            expireAuthSession()
            return Promise.reject(error)
        }

        config.headers.set('Authorization', `Bearer ${accessToken}`)
        return axiosApiClient.request(config)
    },
)

export default axiosApiClient;
