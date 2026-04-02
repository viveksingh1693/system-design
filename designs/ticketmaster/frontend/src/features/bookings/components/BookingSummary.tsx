import { formatCurrency, formatDateTime } from "../../../shared/utils/format";
import type { BookingResponse } from "../../../types/api";

interface BookingSummaryProps {
  booking: BookingResponse;
}

export function BookingSummary({ booking }: BookingSummaryProps) {
  return (
    <div className="booking-summary">
      <div className="booking-summary__header">
        <div>
          <p className="eyebrow">Booking #{booking.id}</p>
          <h3>{booking.eventTitle}</h3>
        </div>
        <span className={`status-badge status-badge--${booking.status.toLowerCase()}`}>{booking.status}</span>
      </div>

      <dl className="stat-list">
        <div>
          <dt>Show</dt>
          <dd>#{booking.showId}</dd>
        </div>
        <div>
          <dt>Seats</dt>
          <dd>{booking.seatCount}</dd>
        </div>
        <div>
          <dt>Total</dt>
          <dd>{formatCurrency(booking.totalAmount)}</dd>
        </div>
        <div>
          <dt>Starts</dt>
          <dd>{formatDateTime(booking.startTime)}</dd>
        </div>
        <div>
          <dt>Customer</dt>
          <dd>{booking.customerEmail}</dd>
        </div>
        <div>
          <dt>Hold expires</dt>
          <dd>{formatDateTime(booking.holdExpiresAt)}</dd>
        </div>
        <div>
          <dt>Payment ref</dt>
          <dd>{booking.paymentReference ?? "Pending"}</dd>
        </div>
      </dl>
    </div>
  );
}
