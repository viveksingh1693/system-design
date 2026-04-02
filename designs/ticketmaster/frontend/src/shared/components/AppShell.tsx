import { NavLink, Outlet } from "react-router-dom";

const navigation = [
  { to: "/", label: "Discovery" },
  { to: "/operations", label: "Operations" },
  { to: "/bookings", label: "Bookings" }
];

const operatingNotes = [
  "Debounced, abortable searches reduce duplicate reads during high-traffic discovery spikes.",
  "Immutable static assets are build-hashed so a CDN or edge cache can absorb most UI traffic.",
  "Writes stay narrowly scoped to bookings and inventory updates, which keeps the client extensible."
];

export function AppShell() {
  return (
    <div className="app-shell">
      <div className="ambient ambient--one" />
      <div className="ambient ambient--two" />

      <header className="hero">
        <div>
          <p className="eyebrow">Ticket Master frontend</p>
          <h1>Control the fan journey without coupling the entire system together.</h1>
          <p className="hero-copy">
            A modular React client for catalog discovery, show operations, and booking workflows. Built to pair
            cleanly with the Spring Boot backend and stay operable as throughput rises.
          </p>
        </div>

        <div className="hero-panel">
          <div className="hero-metric">
            <span>Target profile</span>
            <strong>1000 QPS ready</strong>
          </div>
          <div className="hero-metric">
            <span>Delivery mode</span>
            <strong>Vite in dev, Spring static assets in prod</strong>
          </div>
          <div className="hero-metric">
            <span>Frontend posture</span>
            <strong>Abort stale reads, cache hot paths, isolate writes</strong>
          </div>
        </div>
      </header>

      <nav className="top-nav" aria-label="Primary">
        {navigation.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `nav-link${isActive ? " nav-link--active" : ""}`}
            end={item.to === "/"}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <main className="layout">
        <Outlet />
      </main>

      <aside className="ops-ribbon">
        {operatingNotes.map((note) => (
          <p key={note}>{note}</p>
        ))}
      </aside>
    </div>
  );
}
