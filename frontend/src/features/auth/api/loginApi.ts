import axiosApiClient from "@/app/plugins/axiosApiClient"

export const loginApi = (username: string, password: string) => {
    return axiosApiClient.post("/auth/login", {
        username,
        password
    });
}