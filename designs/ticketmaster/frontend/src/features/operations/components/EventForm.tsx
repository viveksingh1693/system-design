import type { FormEvent } from "react";
import { useState } from "react";
import { ApiError, createEvent } from "../../../shared/api/client";
import { FeedbackMessage } from "../../../shared/components/FeedbackMessage";
import { SectionCard } from "../../../shared/components/SectionCard";
import type { EventResponse } from "../../../types/api";

interface EventFormProps {
  onCreated: (event: EventResponse) => void;
}

const initialForm = {
  title: "",
  performer: "",
  city: "",
  venueName: "",
  category: ""
};

export function EventForm({ onCreated }: EventFormProps) {
  const [form, setForm] = useState(initialForm);
  const [feedback, setFeedback] = useState<{ variant: "success" | "error"; message: string } | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    try {
      const createdEvent = await createEvent(form);
      setForm(initialForm);
      setFeedback({ variant: "success", message: `Event #${createdEvent.id} is live in the catalog.` });
      onCreated(createdEvent);
    } catch (error: unknown) {
      const message = error instanceof ApiError ? error.message : "Unable to create the event.";
      setFeedback({ variant: "error", message });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <SectionCard
      title="Create event"
      description="Onboard a new event without mixing inventory concerns into the catalog form."
    >
      <form className="stack-form" onSubmit={handleSubmit}>
        <div className="form-grid">
          <label>
            <span>Title</span>
            <input value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} required />
          </label>
          <label>
            <span>Performer</span>
            <input value={form.performer} onChange={(event) => setForm((current) => ({ ...current, performer: event.target.value }))} required />
          </label>
          <label>
            <span>City</span>
            <input value={form.city} onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))} required />
          </label>
          <label>
            <span>Venue</span>
            <input value={form.venueName} onChange={(event) => setForm((current) => ({ ...current, venueName: event.target.value }))} required />
          </label>
          <label>
            <span>Category</span>
            <input value={form.category} onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))} required />
          </label>
        </div>

        <button className="button button--primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Creating event..." : "Create event"}
        </button>
      </form>

      {feedback ? <FeedbackMessage variant={feedback.variant} message={feedback.message} /> : null}
    </SectionCard>
  );
}
