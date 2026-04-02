import type {
  BookingResponse,
  ConfirmBookingRequest,
  CreateEventRequest,
  CreateShowRequest,
  ErrorResponse,
  EventFilters,
  EventResponse,
  HoldBookingRequest,
  ShowFilters,
  ShowResponse
} from "../../types/api";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");
const apiCache = new Map<string, { expiresAt: number; value: unknown }>();

export class ApiError extends Error {
  status: number;
  code?: string;
  timestamp?: string;

  constructor(message: string, status: number, code?: string, timestamp?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.timestamp = timestamp;
  }
}

interface RequestOptions {
  cacheTtlMs?: number;
}

function buildApiUrl(path: string): string {
  if (!apiBaseUrl) {
    return path;
  }

  return `${apiBaseUrl}${path.startsWith("/") ? path : `/${path}`}`;
}

function buildQueryString(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === "") {
      return;
    }

    search.set(key, String(value));
  });

  const query = search.toString();
  return query ? `?${query}` : "";
}

function getCacheKey(path: string, init?: RequestInit): string | null {
  const method = init?.method ?? "GET";
  return method === "GET" ? path : null;
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
    payload?.code,
    payload?.timestamp
  );
}

async function request<T>(path: string, init?: RequestInit, options?: RequestOptions): Promise<T> {
  const cacheKey = getCacheKey(path, init);
  const cacheTtlMs = options?.cacheTtlMs ?? 0;
  const now = Date.now();

  if (cacheKey && cacheTtlMs > 0) {
    const cached = apiCache.get(cacheKey);
    if (cached && cached.expiresAt > now) {
      return cached.value as T;
    }
  }

  const response = await fetch(buildApiUrl(path), {
    headers: {
      Accept: "application/json",
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
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

  const data = (await response.json()) as T;

  if (cacheKey && cacheTtlMs > 0) {
    apiCache.set(cacheKey, {
      value: data,
      expiresAt: now + cacheTtlMs
    });
  }

  return data;
}

function invalidateCache(prefixes: string[]): void {
  Array.from(apiCache.keys()).forEach((key) => {
    if (prefixes.some((prefix) => key.startsWith(prefix))) {
      apiCache.delete(key);
    }
  });
}

export function resetApiCacheForTests(): void {
  apiCache.clear();
}

export function searchEvents(filters: EventFilters = {}, signal?: AbortSignal): Promise<EventResponse[]> {
  return request<EventResponse[]>(
    `/events${buildQueryString({
      city: filters.city?.trim(),
      q: filters.query?.trim()
    })}`,
    { signal },
    { cacheTtlMs: 15_000 }
  );
}

export async function createEvent(payload: CreateEventRequest): Promise<EventResponse> {
  const event = await request<EventResponse>("/events", {
    method: "POST",
    body: JSON.stringify(payload)
  });

  invalidateCache(["/events", "/shows"]);
  return event;
}

export function searchShows(filters: ShowFilters = {}, signal?: AbortSignal): Promise<ShowResponse[]> {
  return request<ShowResponse[]>(
    `/shows${buildQueryString({
      city: filters.city?.trim(),
      q: filters.query?.trim(),
      from: filters.from,
      to: filters.to
    })}`,
    { signal },
    { cacheTtlMs: 5_000 }
  );
}

export function getShow(showId: number, signal?: AbortSignal): Promise<ShowResponse> {
  return request<ShowResponse>(`/shows/${showId}`, { signal }, { cacheTtlMs: 5_000 });
}

export async function createShow(eventId: number, payload: CreateShowRequest): Promise<ShowResponse> {
  const show = await request<ShowResponse>(`/events/${eventId}/shows`, {
    method: "POST",
    body: JSON.stringify(payload)
  });

  invalidateCache(["/events", "/shows"]);
  return show;
}

export async function holdBooking(payload: HoldBookingRequest): Promise<BookingResponse> {
  const booking = await request<BookingResponse>("/bookings/hold", {
    method: "POST",
    body: JSON.stringify(payload)
  });

  invalidateCache(["/shows", `/shows/${payload.showId}`, "/bookings"]);
  return booking;
}

export async function confirmBooking(
  bookingId: number,
  payload: ConfirmBookingRequest
): Promise<BookingResponse> {
  const booking = await request<BookingResponse>(`/bookings/${bookingId}/confirm`, {
    method: "POST",
    body: JSON.stringify(payload)
  });

  invalidateCache(["/bookings"]);
  return booking;
}

export async function cancelBooking(bookingId: number): Promise<BookingResponse> {
  const booking = await request<BookingResponse>(`/bookings/${bookingId}/cancel`, {
    method: "POST"
  });

  invalidateCache(["/bookings", "/shows"]);
  return booking;
}

export function getBooking(bookingId: number, signal?: AbortSignal): Promise<BookingResponse> {
  return request<BookingResponse>(`/bookings/${bookingId}`, { signal }, { cacheTtlMs: 2_000 });
}
