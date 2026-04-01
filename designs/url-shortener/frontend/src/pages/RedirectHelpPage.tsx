import { Link, useSearchParams } from "react-router-dom";

const reasonCopy: Record<string, { title: string; body: string }> = {
  inactive: {
    title: "This short link is inactive",
    body: "The link exists, but it has been disabled and can no longer redirect traffic."
  },
  expired: {
    title: "This short link has expired",
    body: "The link reached its expiration time, so redirects are now blocked."
  },
  missing: {
    title: "We could not find that short link",
    body: "The short code may be incorrect, deleted, or never created in this environment."
  }
};

export function RedirectHelpPage() {
  const [searchParams] = useSearchParams();
  const reason = searchParams.get("reason") ?? "missing";
  const code = searchParams.get("code") ?? "unknown";
  const message = searchParams.get("message");
  const content = reasonCopy[reason] ?? reasonCopy.missing;

  return (
    <main className="page-shell helper-shell">
      <section className="panel helper-panel">
        <p className="eyebrow">Redirect helper</p>
        <h1>{content.title}</h1>
        <p className="hero-text">{content.body}</p>
        <div className="helper-card">
          <span className="detail-label">Short code</span>
          <strong>{code}</strong>
        </div>
        {message ? (
          <div className="feedback error">
            <strong>Backend message:</strong> {message}
          </div>
        ) : null}
        <div className="panel-actions">
          <Link className="primary-button link-button" to="/">
            Return to dashboard
          </Link>
          <a
            className="secondary-button link-button"
            href="http://localhost:8081/swagger-ui.html"
            target="_blank"
            rel="noreferrer"
          >
            Open API docs
          </a>
        </div>
      </section>
    </main>
  );
}
