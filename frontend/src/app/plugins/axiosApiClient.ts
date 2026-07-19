import axios from "axios";

const axiosApiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL
});

axiosApiClient.interceptors.request.use((config) => {

    const token = localStorage.getItem("accessToken")

    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }

    config.headers["X-Tenant-ID"] = "default"

    return config;
})

export default axiosApiClient;