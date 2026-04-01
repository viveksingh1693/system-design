export type ShortUrlStatus = "ACTIVE" | "DISABLED";

export interface CreateShortUrlRequest {
  originalUrl: string;
  customAlias?: string;
  expiresAt?: string;
}

export interface ShortUrlResponse {
  id: number;
  shortCode: string;
  shortUrl: string;
  originalUrl: string;
  status: ShortUrlStatus | string;
  createdAt: string;
  expiresAt?: string | null;
  lastAccessedAt?: string | null;
  redirectCount: number;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details: string[];
}
