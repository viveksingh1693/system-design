import { Link } from "react-router-dom";
import { formatCurrency, formatDateTime, formatPercentage } from "../../../shared/utils/format";
import type { ShowResponse } from "../../../types/api";

interface ShowCardProps {
  show: ShowResponse;
}

export function ShowCard({ show }: ShowCardProps) {
  const sellThrough = ((show.totalSeats - show.availableSeats) / show.totalSeats) * 100;

  return (
    <article className="show-card">
      <div className="show-card__header">
        <div>
          <p className="show-card__performer">{show.performer}</p>
          <h3>{show.eventTitle}</h3>
        </div>
        <span className="pill">{formatCurrency(show.price)}</span>
      </div>

      <dl className="show-card__meta">
        <div>
          <dt>Venue</dt>
          <dd>{show.venueName}</dd>
        </div>
        <div>
          <dt>City</dt>
          <dd>{show.city}</dd>
        </div>
        <div>
          <dt>Starts</dt>
          <dd>{formatDateTime(show.startTime)}</dd>
        </div>
      </dl>

      <div className="capacity-meter">
        <div className="capacity-meter__topline">
          <span>{show.availableSeats} seats left</span>
          <span>{formatPercentage(sellThrough)} sold</span>
        </div>
        <div className="capacity-meter__track">
          <div className="capacity-meter__fill" style={{ width: `${Math.min(sellThrough, 100)}%` }} />
        </div>
      </div>

      <div className="show-card__actions">
        <Link to={`/bookings?showId=${show.id}`} className="button button--primary">
          Reserve seats
        </Link>
        <Link to="/operations" className="button button--secondary">
          Open ops view
        </Link>
      </div>
    </article>
  );
}
