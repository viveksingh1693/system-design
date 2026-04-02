import { useEffect, useState } from "react";
import { searchEvents } from "../../../shared/api/client";
import { FeedbackMessage } from "../../../shared/components/FeedbackMessage";
import { PageHeader } from "../../../shared/components/PageHeader";
import { SectionCard } from "../../../shared/components/SectionCard";
import { formatDateTime } from "../../../shared/utils/format";
import type { EventResponse, ShowResponse } from "../../../types/api";
import { EventForm } from "../components/EventForm";
import { ShowForm } from "../components/ShowForm";

export function OperationsPage() {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [recentShows, setRecentShows] = useState<ShowResponse[]>([]);
  const [isLoadingEvents, setIsLoadingEvents] = useState(true);
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    searchEvents({}, controller.signal)
      .then((response) => {
        setEvents(response);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        const message = error instanceof Error ? error.message : "Unable to load events.";
        setFeedback(message);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoadingEvents(false);
        }
      });

    return () => controller.abort();
  }, []);

  return (
    <div className="page-grid">
      <div className="page-grid__main">
        <PageHeader
          eyebrow="Operations console"
          title="Separate catalog onboarding from inventory activation."
          description="This page keeps event metadata and show inventory distinct, which makes future pricing, venue, and supply workflows easier to extend without rewriting the UI."
        />

        {feedback ? <FeedbackMessage variant="error" message={feedback} /> : null}

        <div className="stack-layout">
          <EventForm
            onCreated={(event) => {
              setEvents((current) => [event, ...current]);
            }}
          />

          <ShowForm
            events={events}
            onCreated={(show) => {
              setRecentShows((current) => [show, ...current].slice(0, 5));
            }}
          />
        </div>
      </div>

      <aside className="page-grid__sidebar">
        <SectionCard title="Catalog health" description="Operators can quickly see whether the catalog is ready for new show launches.">
          {isLoadingEvents ? <p className="muted-copy">Loading event catalog...</p> : null}
          {!isLoadingEvents && events.length === 0 ? (
            <p className="muted-copy">No events yet. Start by creating the first event record.</p>
          ) : null}
          {events.length > 0 ? (
            <dl className="stat-list">
              <div>
                <dt>Events onboarded</dt>
                <dd>{events.length}</dd>
              </div>
              <div>
                <dt>Distinct cities</dt>
                <dd>{new Set(events.map((event) => event.city)).size}</dd>
              </div>
              <div>
                <dt>Distinct categories</dt>
                <dd>{new Set(events.map((event) => event.category)).size}</dd>
              </div>
            </dl>
          ) : null}
        </SectionCard>

        <SectionCard title="Recent launches" description="Freshly created shows stay visible here for fast QA and handoff.">
          {recentShows.length === 0 ? <p className="muted-copy">No shows launched in this session yet.</p> : null}
          <ul className="timeline-list">
            {recentShows.map((show) => (
              <li key={show.id}>
                <strong>{show.eventTitle}</strong>
                <span>{formatDateTime(show.startTime)}</span>
                <span>{show.availableSeats} seats</span>
              </li>
            ))}
          </ul>
        </SectionCard>

        <SectionCard title="Production posture" description="A few frontend decisions that help when load climbs.">
          <ul className="insight-list">
            <li>Separate modules reduce the blast radius when operations workflows change faster than discovery.</li>
            <li>Shared API helpers centralize error handling, cache policy, and future auth headers.</li>
            <li>Build output lands directly in Spring static resources, which keeps deployment simple.</li>
          </ul>
        </SectionCard>
      </aside>
    </div>
  );
}
