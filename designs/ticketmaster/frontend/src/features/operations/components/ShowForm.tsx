import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { ApiError, createShow } from "../../../shared/api/client";
import { FeedbackMessage } from "../../../shared/components/FeedbackMessage";
import { SectionCard } from "../../../shared/components/SectionCard";
import { formatCurrency, formatDateTime, toDateTimeLocalValue } from "../../../shared/utils/format";
import type { EventResponse, ShowResponse } from "../../../types/api";

interface ShowFormProps {
  events: EventResponse[];
  onCreated: (show: ShowResponse) => void;
}

export function ShowForm({ events, onCreated }: ShowFormProps) {
  const [selectedEventId, setSelectedEventId] = useState("");
  const [startTime, setStartTime] = useState(toDateTimeLocalValue());
  const [price, setPrice] = useState("2499");
  const [totalSeats, setTotalSeats] = useState("5000");
  const [feedback, setFeedback] = useState<{ variant: "success" | "error"; message: string } | null>(null);
  const [lastCreatedShow, setLastCreatedShow] = useState<ShowResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!selectedEventId && events[0]) {
      setSelectedEventId(String(events[0].id));
    }
  }, [events, selectedEventId]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedEventId) {
      setFeedback({ variant: "error", message: "Create an event first so inventory has a catalog anchor." });
      return;
    }

    setIsSubmitting(true);
    setFeedback(null);

    try {
      const show = await createShow(Number(selectedEventId), {
        startTime,
        price: Number(price),
        totalSeats: Number(totalSeats)
      });

      setLastCreatedShow(show);
      setFeedback({ variant: "success", message: `Show #${show.id} is now available for bookings.` });
      onCreated(show);
    } catch (error: unknown) {
      const message = error instanceof ApiError ? error.message : "Unable to create the show.";
      setFeedback({ variant: "error", message });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <SectionCard
      title="Create show"
      description="Attach inventory to an existing event. Keeping show creation separate makes it easier to evolve pricing and seat logic later."
    >
      <form className="stack-form" onSubmit={handleSubmit}>
        <div className="form-grid">
          <label>
            <span>Event</span>
            <select value={selectedEventId} onChange={(event) => setSelectedEventId(event.target.value)} required>
              {events.length === 0 ? <option value="">Create an event first</option> : null}
              {events.map((item) => (
                <option key={item.id} value={item.id}>
                  #{item.id} {item.title} - {item.city}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Start time</span>
            <input type="datetime-local" value={startTime} onChange={(event) => setStartTime(event.target.value)} required />
          </label>
          <label>
            <span>Price</span>
            <input type="number" min="1" value={price} onChange={(event) => setPrice(event.target.value)} required />
          </label>
          <label>
            <span>Total seats</span>
            <input type="number" min="1" value={totalSeats} onChange={(event) => setTotalSeats(event.target.value)} required />
          </label>
        </div>

        <button className="button button--primary" type="submit" disabled={isSubmitting || events.length === 0}>
          {isSubmitting ? "Creating show..." : "Create show"}
        </button>
      </form>

      {feedback ? <FeedbackMessage variant={feedback.variant} message={feedback.message} /> : null}

      {lastCreatedShow ? (
        <div className="highlight-card">
          <strong>{lastCreatedShow.eventTitle}</strong>
          <p>{formatDateTime(lastCreatedShow.startTime)}</p>
          <p>{formatCurrency(lastCreatedShow.price)}</p>
          <p>{lastCreatedShow.availableSeats} seats available at launch</p>
        </div>
      ) : null}
    </SectionCard>
  );
}
