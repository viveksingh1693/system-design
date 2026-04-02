import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  ApiError,
  cancelBooking,
  confirmBooking,
  getBooking,
  getShow,
  holdBooking
} from "../../../shared/api/client";
import { FeedbackMessage } from "../../../shared/components/FeedbackMessage";
import { PageHeader } from "../../../shared/components/PageHeader";
import { SectionCard } from "../../../shared/components/SectionCard";
import { formatCurrency, formatDateTime } from "../../../shared/utils/format";
import type { BookingResponse, ShowResponse } from "../../../types/api";
import { BookingSummary } from "../components/BookingSummary";

const defaultPaymentToken = "tok_live_demo_visa";

export function BookingDeskPage() {
  const [searchParams] = useSearchParams();
  const [showId, setShowId] = useState(searchParams.get("showId") ?? "");
  const [customerEmail, setCustomerEmail] = useState("fan@example.com");
  const [seatCount, setSeatCount] = useState("2");
  const [bookingIdInput, setBookingIdInput] = useState(searchParams.get("bookingId") ?? "");
  const [paymentToken, setPaymentToken] = useState(defaultPaymentToken);
  const [selectedShow, setSelectedShow] = useState<ShowResponse | null>(null);
  const [booking, setBooking] = useState<BookingResponse | null>(null);
  const [feedback, setFeedback] = useState<{ variant: "success" | "error"; message: string } | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!showId) {
      setSelectedShow(null);
      return;
    }

    const parsedShowId = Number(showId);
    if (Number.isNaN(parsedShowId)) {
      return;
    }

    const controller = new AbortController();

    getShow(parsedShowId, controller.signal)
      .then((response) => {
        setSelectedShow(response);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        const message = error instanceof Error ? error.message : "Unable to load show details.";
        setFeedback({ variant: "error", message });
      });

    return () => controller.abort();
  }, [showId]);

  async function handleHold(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    try {
      const heldBooking = await holdBooking({
        showId: Number(showId),
        customerEmail,
        seatCount: Number(seatCount)
      });

      setBooking(heldBooking);
      setBookingIdInput(String(heldBooking.id));
      setFeedback({ variant: "success", message: `Hold created. Booking #${heldBooking.id} is waiting for confirmation.` });
    } catch (error: unknown) {
      const message = error instanceof ApiError ? error.message : "Unable to hold seats.";
      setFeedback({ variant: "error", message });
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleLookup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    try {
      const resolvedBooking = await getBooking(Number(bookingIdInput));
      setBooking(resolvedBooking);
      setFeedback({ variant: "success", message: `Loaded booking #${resolvedBooking.id}.` });
    } catch (error: unknown) {
      const message = error instanceof ApiError ? error.message : "Unable to load the booking.";
      setFeedback({ variant: "error", message });
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleConfirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    try {
      const confirmedBooking = await confirmBooking(Number(bookingIdInput), { paymentToken });
      setBooking(confirmedBooking);
      setFeedback({ variant: "success", message: `Booking #${confirmedBooking.id} is confirmed.` });
    } catch (error: unknown) {
      const message = error instanceof ApiError ? error.message : "Unable to confirm the booking.";
      setFeedback({ variant: "error", message });
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleCancel() {
    if (!bookingIdInput) {
      setFeedback({ variant: "error", message: "Enter a booking id to cancel." });
      return;
    }

    setIsSubmitting(true);
    setFeedback(null);

    try {
      const cancelledBooking = await cancelBooking(Number(bookingIdInput));
      setBooking(cancelledBooking);
      setFeedback({ variant: "success", message: `Booking #${cancelledBooking.id} is now ${cancelledBooking.status}.` });
    } catch (error: unknown) {
      const message = error instanceof ApiError ? error.message : "Unable to cancel the booking.";
      setFeedback({ variant: "error", message });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="page-grid">
      <div className="page-grid__main">
        <PageHeader
          eyebrow="Booking desk"
          title="Work the hold, confirm, and cancel flow from one place."
          description="The booking desk mirrors the backend orchestration flow: reserve seats first, then confirm with a payment token, then inspect or cancel if needed."
        />

        {feedback ? <FeedbackMessage variant={feedback.variant} message={feedback.message} /> : null}

        <div className="stack-layout">
          <SectionCard
            title="Hold seats"
            description="Use this for the write-heavy part of the flow. Keep the payload small so checkout paths stay efficient."
          >
            <form className="stack-form" onSubmit={handleHold}>
              <div className="form-grid">
                <label>
                  <span>Show id</span>
                  <input type="number" min="1" value={showId} onChange={(event) => setShowId(event.target.value)} required />
                </label>
                <label>
                  <span>Customer email</span>
                  <input type="email" value={customerEmail} onChange={(event) => setCustomerEmail(event.target.value)} required />
                </label>
                <label>
                  <span>Seats</span>
                  <input type="number" min="1" value={seatCount} onChange={(event) => setSeatCount(event.target.value)} required />
                </label>
              </div>

              <button className="button button--primary" type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Holding seats..." : "Hold seats"}
              </button>
            </form>
          </SectionCard>

          <SectionCard
            title="Confirm or inspect booking"
            description="Lookup is cached briefly so operators can refresh status without hammering the booking endpoint."
          >
            <div className="stack-layout stack-layout--compact">
              <form className="inline-form" onSubmit={handleLookup}>
                <label>
                  <span>Booking id</span>
                  <input type="number" min="1" value={bookingIdInput} onChange={(event) => setBookingIdInput(event.target.value)} required />
                </label>
                <button className="button button--secondary" type="submit" disabled={isSubmitting}>
                  Lookup
                </button>
              </form>

              <form className="inline-form" onSubmit={handleConfirm}>
                <label>
                  <span>Payment token</span>
                  <input value={paymentToken} onChange={(event) => setPaymentToken(event.target.value)} required />
                </label>
                <button className="button button--primary" type="submit" disabled={isSubmitting || !bookingIdInput}>
                  Confirm booking
                </button>
              </form>

              <button className="button button--ghost" type="button" onClick={handleCancel} disabled={isSubmitting || !bookingIdInput}>
                Cancel booking
              </button>
            </div>
          </SectionCard>
        </div>
      </div>

      <aside className="page-grid__sidebar">
        <SectionCard title="Selected show" description="Useful when the booking desk is opened directly from discovery.">
          {selectedShow ? (
            <div className="highlight-card">
              <strong>{selectedShow.eventTitle}</strong>
              <p>{selectedShow.performer}</p>
              <p>{formatDateTime(selectedShow.startTime)}</p>
              <p>{formatCurrency(selectedShow.price)}</p>
              <p>{selectedShow.availableSeats} seats remaining</p>
            </div>
          ) : (
            <p className="muted-copy">Enter a show id or jump here from discovery to load show details.</p>
          )}
        </SectionCard>

        <SectionCard title="Current booking" description="The latest booking state from your session.">
          {booking ? <BookingSummary booking={booking} /> : <p className="muted-copy">No booking loaded yet.</p>}
        </SectionCard>

        <SectionCard title="Flow notes" description="Small guardrails for a busy booking desk.">
          <ul className="insight-list">
            <li>Held bookings expire in the backend after the configured hold window.</li>
            <li>After any write, the frontend invalidates booking and show caches to avoid stale operator views.</li>
            <li>Keeping confirmation separate from hold mirrors real checkout systems where payment and reservation are decoupled.</li>
          </ul>
        </SectionCard>
      </aside>
    </div>
  );
}
