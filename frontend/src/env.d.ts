interface ImportMetaEnv {
    readonly VITE_API_BASE_URL?: string
    readonly VITE_APP_NAME?: string
    readonly VITE_SYNCFUSION_LICENSE_KEY?: string
    readonly VITE_SYNCFUSION_SPREADSHEET_OPEN_URL?: string
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}
