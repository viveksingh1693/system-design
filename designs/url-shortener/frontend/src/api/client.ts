import type { CreateShortUrlRequest, ErrorResponse, ShortUrlResponse } from "../types";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export class ApiError extends Error {
  status: number;
  details: string[];
  path?: string;

  constructor(message: string, status: number, details: string[] = [], path?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
    this.path = path;
  }
}

export function buildApiUrl(path: string): string {
  if (!apiBaseUrl) {
    return path;
  }

  return `${apiBaseUrl}${path.startsWith("/") ? path : `/${path}`}`;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(buildApiUrl(path), {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    ...init
  });

  if (!response.ok) {
    throw await parseApiError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function parseApiError(response: Response): Promise<ApiError> {
  let payload: ErrorResponse | undefined;

  try {
    payload = (await response.json()) as ErrorResponse;
  } catch {
    payload = undefined;
  }

  return new ApiError(
    payload?.message ?? `Request failed with status ${response.status}`,
    response.status,
    payload?.details ?? [],
    payload?.path
  );
}

export function createShortUrl(payload: CreateShortUrlRequest): Promise<ShortUrlResponse> {
  return request<ShortUrlResponse>("/api/v1/urls", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getShortUrl(shortCode: string): Promise<ShortUrlResponse> {
  return request<ShortUrlResponse>(`/api/v1/urls/${encodeURIComponent(shortCode)}`);
}

export function disableShortUrl(shortCode: string): Promise<ShortUrlResponse> {
  return request<ShortUrlResponse>(`/api/v1/urls/${encodeURIComponent(shortCode)}`, {
    method: "DELETE"
  });
}
