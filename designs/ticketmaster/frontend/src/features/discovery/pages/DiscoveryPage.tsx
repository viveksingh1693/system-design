import { startTransition, useDeferredValue, useEffect, useState } from "react";
import { searchShows } from "../../../shared/api/client";
import { FeedbackMessage } from "../../../shared/components/FeedbackMessage";
import { PageHeader } from "../../../shared/components/PageHeader";
import { SectionCard } from "../../../shared/components/SectionCard";
import { formatCurrency, formatDateTime } from "../../../shared/utils/format";
import type { ShowFilters, ShowResponse } from "../../../types/api";
import { ShowCard } from "../components/ShowCard";

const initialFilters: ShowFilters = {
  city: "",
  query: "",
  from: "",
  to: ""
};

export function DiscoveryPage() {
  const [filters, setFilters] = useState<ShowFilters>(initialFilters);
  const deferredFilters = useDeferredValue(filters);
  const [shows, setShows] = useState<ShowResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    setIsLoading(true);
    setFeedback(null);

    searchShows(deferredFilters, controller.signal)
      .then((response) => {
        setShows(response);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        const message = error instanceof Error ? error.message : "Unable to load shows right now.";
        setFeedback(message);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      });

    return () => controller.abort();
  }, [deferredFilters]);

  const updateFilter = (field: keyof ShowFilters, value: string) => {
    startTransition(() => {
      setFilters((current) => ({
        ...current,
        [field]: value
      }));
    });
  };

  const upcomingShows = shows.slice().sort((left, right) => left.startTime.localeCompare(right.startTime));
  const totalAvailableSeats = shows.reduce((sum, show) => sum + show.availableSeats, 0);
  const middleIndex = shows.length > 0 ? Math.floor(shows.length / 2) : -1;
  const medianTicket = middleIndex >= 0 ? shows[middleIndex].price : null;

  return (
    <div className="page-grid">
      <div className="page-grid__main">
        <PageHeader
          eyebrow="Discovery desk"
          title="Keep discovery fast, precise, and gentle on the backend."
          description="Search the live show inventory with deferred filters and stale-request cancellation so the UI remains usable even when discovery traffic spikes."
        />

        <SectionCard
          title="Show search"
          description="Filter by city, performer, title, or a time window. Requests are cached for a short TTL to reduce duplicate hot-path reads."
        >
          <div className="filter-grid">
            <label>
              <span>City</span>
              <input value={filters.city ?? ""} onChange={(event) => updateFilter("city", event.target.value)} placeholder="Bengaluru" />
            </label>
            <label>
              <span>Artist or title</span>
              <input value={filters.query ?? ""} onChange={(event) => updateFilter("query", event.target.value)} placeholder="Coldplay, comedy, arena" />
            </label>
            <label>
              <span>From</span>
              <input type="datetime-local" value={filters.from ?? ""} onChange={(event) => updateFilter("from", event.target.value)} />
            </label>
            <label>
              <span>To</span>
              <input type="datetime-local" value={filters.to ?? ""} onChange={(event) => updateFilter("to", event.target.value)} />
            </label>
          </div>
        </SectionCard>

        {feedback ? <FeedbackMessage variant="error" message={feedback} /> : null}

        <section className="show-grid" aria-live="polite">
          {isLoading ? <p className="muted-panel">Loading the freshest show inventory...</p> : null}
          {!isLoading && shows.length === 0 ? (
            <p className="muted-panel">No shows matched this slice. Try widening the city or time filters.</p>
          ) : null}
          {upcomingShows.map((show) => (
            <ShowCard key={show.id} show={show} />
          ))}
        </section>
      </div>

      <aside className="page-grid__sidebar">
        <SectionCard title="Inventory snapshot" description="Quick operating signals from the current result set.">
          <dl className="stat-list">
            <div>
              <dt>Visible shows</dt>
              <dd>{shows.length}</dd>
            </div>
            <div>
              <dt>Open seats</dt>
              <dd>{totalAvailableSeats}</dd>
            </div>
            <div>
              <dt>Median ticket</dt>
              <dd>{formatCurrency(medianTicket)}</dd>
            </div>
          </dl>
        </SectionCard>

        <SectionCard title="Why this scales" description="Frontend choices tuned for higher read traffic.">
          <ul className="insight-list">
            <li>Typed API wrappers keep contract drift localized when backend modules evolve.</li>
            <li>Discovery reads are cancellable, so fast typists do not flood the service with stale requests.</li>
            <li>Hot-path GETs are cached briefly in memory, which reduces repeated reads from the same operator session.</li>
          </ul>
        </SectionCard>

        <SectionCard title="Earliest result" description="Useful when support teams need the next visible show at a glance.">
          {upcomingShows[0] ? (
            <div className="highlight-card">
              <strong>{upcomingShows[0].eventTitle}</strong>
              <p>{upcomingShows[0].performer}</p>
              <p>{formatDateTime(upcomingShows[0].startTime)}</p>
              <p>{upcomingShows[0].venueName}</p>
            </div>
          ) : (
            <p className="muted-copy">No live result yet.</p>
          )}
        </SectionCard>
      </aside>
    </div>
  );
}
