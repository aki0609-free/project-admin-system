/* eslint-disable @typescript-eslint/no-explicit-any */
import { apiClient } from "@/app/plugins/apiClient";

export async function get<T>(path: any, options?: any) {
    const { data, error } = await apiClient.GET(path, options)

    if (error) {
        throw error
    }

    if (data === undefined) {
        throw new Error(`GET ${String(path)} returned undefined`)
    }

    return data as T
}

export async function post<TResponse, TBody = unknown>(
  path: any,
  body?: TBody,
  options?: any,
) {
  const { data, error } = await apiClient.POST(path, {
    ...options,
    body,
  })

  if (error) {
    throw error
  }

  return data as TResponse
}

export async function put<TResponse, TBody = unknown>(
  path: any,
  body?: TBody,
  options?: any,
) {
  const { data, error } = await apiClient.PUT(path, {
    ...options,
    body,
  })

  if (error) {
    throw error
  }

  return data as TResponse
}


export const patch = async <TResponse, TRequest>(
  path: string,
  options?: any,
): Promise<TResponse> => {
  const { data, error } = await apiClient.PATCH(path, options)

  if (error) {
    throw error
  }

  return data as TResponse
}

export async function del<TResponse = void>(
  path: any,
  options?: any,
) {
  const { data, error } = await apiClient.DELETE(path, options)

  if (error) {
    throw error
  }

  return data as TResponse
}

export async function postForm<TResponse>(
  path: string,
  formData: FormData,
): Promise<TResponse> {
  const { data, error } = await (apiClient as any).POST(path, {
    body: formData,
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });

  if (error) throw error;
  return data as TResponse;
}

export async function postBlob<TBody = unknown>(
  path: string,
  body?: TBody,
  options?: Record<string, unknown>,
): Promise<Blob> {
  const { data, error } = await (apiClient as any).POST(path, {
    ...options,
    body,
    parseAs: "blob",
  });

  if (error) {
    throw error;
  }

  if (!(data instanceof Blob)) {
    throw new Error(`POST BLOB ${path} did not return Blob`);
  }

  return data;
}

export type BlobDownloadResponse = {
  blob: Blob
  fileName: string | null
}

export async function postBlobDownload<TBody = unknown>(
  path: string,
  body?: TBody,
): Promise<BlobDownloadResponse> {
  const { data, error, response } = await (apiClient as any).POST(path, {
    body,
    parseAs: 'blob',
  })

  if (error) {
    throw error
  }
  if (!(data instanceof Blob)) {
    throw new Error(`POST BLOB ${path} did not return Blob`)
  }

  return {
    blob: data,
    fileName: parseContentDispositionFileName(
      response?.headers?.get('content-disposition') ?? null,
    ),
  }
}

const parseContentDispositionFileName = (
  contentDisposition: string | null,
): string | null => {
  if (!contentDisposition) return null

  const encoded = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encoded) {
    try {
      return decodeURIComponent(encoded)
    } catch {
      return encoded
    }
  }

  return contentDisposition.match(/filename=\"?([^\";]+)\"?/i)?.[1] ?? null
}

export const getBlob = async (url: string): Promise<Blob> => {
  const token = localStorage.getItem('accessToken')

  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
    headers: token
      ? {
          Authorization: `Bearer ${token}`,
        }
      : {},
  })

  if (!response.ok) {
    throw new Error(`ファイル取得に失敗しました。 status=${response.status}`)
  }

  return await response.blob()
}
