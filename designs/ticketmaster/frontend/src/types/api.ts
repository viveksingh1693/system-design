export interface EventResponse {
  id: number;
  title: string;
  performer: string;
  city: string;
  venueName: string;
  category: string;
}

export interface CreateEventRequest {
  title: string;
  performer: string;
  city: string;
  venueName: string;
  category: string;
}

export interface ShowResponse {
  id: number;
  eventId: number;
  eventTitle: string;
  performer: string;
  city: string;
  venueName: string;
  startTime: string;
  price: number;
  totalSeats: number;
  availableSeats: number;
}

export interface CreateShowRequest {
  startTime: string;
  price: number;
  totalSeats: number;
}

export type BookingStatus = "HELD" | "CONFIRMED" | "CANCELLED" | "EXPIRED";

export interface BookingResponse {
  id: number;
  showId: number;
  eventTitle: string;
  startTime: string;
  customerEmail: string;
  seatCount: number;
  totalAmount: number;
  status: BookingStatus;
  holdExpiresAt: string | null;
  paymentReference: string | null;
}

export interface HoldBookingRequest {
  showId: number;
  customerEmail: string;
  seatCount: number;
}

export interface ConfirmBookingRequest {
  paymentToken: string;
}

export interface ErrorResponse {
  code?: string;
  message?: string;
  timestamp?: string;
}

export interface EventFilters {
  city?: string;
  query?: string;
}

export interface ShowFilters {
  city?: string;
  query?: string;
  from?: string;
  to?: string;
}
