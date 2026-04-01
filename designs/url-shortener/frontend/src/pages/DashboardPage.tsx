import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, createShortUrl, disableShortUrl, getShortUrl } from "../api/client";
import type { CreateShortUrlRequest, ShortUrlResponse } from "../types";

const DOCS_URL = import.meta.env.VITE_DOCS_URL ?? "http://localhost:8081/swagger-ui.html";

const initialForm: CreateShortUrlRequest = {
  originalUrl: "",
  customAlias: "",
  expiresAt: ""
};

function toInputDateTime(value?: string | null): string {
  if (!value) {
    return "";
  }

  return value.slice(0, 16);
}

function formatDate(value?: string | null): string {
  if (!value) {
    return "Not set";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

function formatError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.details.length > 0) {
      return `${error.message}: ${error.details.join(", ")}`;
    }

    return error.message;
  }

  return "Something unexpected happened. Please try again.";
}

export function DashboardPage() {
  const [createForm, setCreateForm] = useState<CreateShortUrlRequest>(initialForm);
  const [lookupCode, setLookupCode] = useState("");
  const [currentUrl, setCurrentUrl] = useState<ShortUrlResponse | null>(null);
  const [notice, setNotice] = useState<string>("Ready to create your next short link.");
  const [errorMessage, setErrorMessage] = useState<string>("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLookupRunning, setIsLookupRunning] = useState(false);
  const [isDisabling, setIsDisabling] = useState(false);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage("");

    try {
      const payload: CreateShortUrlRequest = {
        originalUrl: createForm.originalUrl.trim(),
        customAlias: createForm.customAlias?.trim() || undefined,
        expiresAt: createForm.expiresAt ? new Date(createForm.expiresAt).toISOString() : undefined
      };

      const response = await createShortUrl(payload);
      setCurrentUrl(response);
      setLookupCode(response.shortCode);
      setNotice(`Created ${response.shortCode} and loaded its management details.`);
      setCreateForm({
        originalUrl: "",
        customAlias: "",
        expiresAt: ""
      });
    } catch (error) {
      setErrorMessage(formatError(error));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleLookup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!lookupCode.trim()) {
      setErrorMessage("Enter a short code to look it up.");
      return;
    }

    setIsLookupRunning(true);
    setErrorMessage("");

    try {
      const response = await getShortUrl(lookupCode.trim());
      setCurrentUrl(response);
      setNotice(`Loaded details for ${response.shortCode}.`);
    } catch (error) {
      setCurrentUrl(null);
      setErrorMessage(formatError(error));
    } finally {
      setIsLookupRunning(false);
    }
  }

  async function handleDisable() {
    if (!currentUrl) {
      return;
    }

    setIsDisabling(true);
    setErrorMessage("");

    try {
      const response = await disableShortUrl(currentUrl.shortCode);
      setCurrentUrl(response);
      setNotice(`Disabled ${response.shortCode}. Future redirects will now be blocked.`);
    } catch (error) {
      setErrorMessage(formatError(error));
    } finally {
      setIsDisabling(false);
    }
  }

  return (
    <main className="page-shell">
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Viv URL Shortener</p>
          <h1>Short links with a calmer control room.</h1>
          <p className="hero-text">
            Create branded aliases, inspect redirect activity, disable links, and jump straight into the
            backend API docs when you need the raw contract.
          </p>
        </div>
        <div className="hero-card">
          <p className="hero-card-label">Backend docs</p>
          <a href={DOCS_URL} target="_blank" rel="noreferrer">
            Open Swagger UI
          </a>
          <p className="hero-card-help">OpenAPI JSON is also available at `/v3/api-docs`.</p>
        </div>
      </section>

      <section className="status-strip" aria-live="polite">
        <span>{notice}</span>
        <Link to="/redirect-help?reason=inactive&code=demo123">Preview redirect helper</Link>
      </section>

      <section className="workspace-grid">
        <form className="panel" onSubmit={handleCreate}>
          <div className="panel-header">
            <h2>Create short URL</h2>
            <p>Generate a new short link with an optional custom alias and expiration.</p>
          </div>

          <label>
            Original URL
            <input
              name="originalUrl"
              type="url"
              placeholder="https://example.com/launch"
              value={createForm.originalUrl}
              onChange={(event) => setCreateForm((current) => ({ ...current, originalUrl: event.target.value }))}
              required
            />
          </label>

          <label>
            Custom alias
            <input
              name="customAlias"
              type="text"
              placeholder="launch2026"
              value={createForm.customAlias ?? ""}
              onChange={(event) => setCreateForm((current) => ({ ...current, customAlias: event.target.value }))}
            />
          </label>

          <label>
            Expiration
            <input
              name="expiresAt"
              type="datetime-local"
              value={toInputDateTime(createForm.expiresAt)}
              onChange={(event) => setCreateForm((current) => ({ ...current, expiresAt: event.target.value }))}
            />
          </label>

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? "Creating..." : "Create short URL"}
          </button>
        </form>

        <form className="panel" onSubmit={handleLookup}>
          <div className="panel-header">
            <h2>Lookup and manage</h2>
            <p>Pull the latest metadata for any short code and disable it when needed.</p>
          </div>

          <label>
            Short code
            <input
              name="lookupCode"
              type="text"
              placeholder="docs2026"
              value={lookupCode}
              onChange={(event) => setLookupCode(event.target.value)}
            />
          </label>

          <div className="panel-actions">
            <button className="secondary-button" type="submit" disabled={isLookupRunning}>
              {isLookupRunning ? "Loading..." : "Fetch details"}
            </button>
            <button
              className="danger-button"
              type="button"
              disabled={!currentUrl || currentUrl.status === "DISABLED" || isDisabling}
              onClick={handleDisable}
            >
              {isDisabling ? "Disabling..." : "Disable link"}
            </button>
          </div>

          {errorMessage ? <p className="feedback error">{errorMessage}</p> : null}
        </form>
      </section>

      <section className="panel details-panel">
        <div className="panel-header">
          <h2>Current selection</h2>
          <p>Live metadata for the short URL you most recently created or fetched.</p>
        </div>

        {currentUrl ? (
          <div className="details-grid">
            <article>
              <span className="detail-label">Short code</span>
              <strong>{currentUrl.shortCode}</strong>
            </article>
            <article>
              <span className="detail-label">Short URL</span>
              <a href={currentUrl.shortUrl} target="_blank" rel="noreferrer">
                {currentUrl.shortUrl}
              </a>
            </article>
            <article>
              <span className="detail-label">Original URL</span>
              <a href={currentUrl.originalUrl} target="_blank" rel="noreferrer">
                {currentUrl.originalUrl}
              </a>
            </article>
            <article>
              <span className="detail-label">Status</span>
              <strong>{currentUrl.status}</strong>
            </article>
            <article>
              <span className="detail-label">Created</span>
              <strong>{formatDate(currentUrl.createdAt)}</strong>
            </article>
            <article>
              <span className="detail-label">Expires</span>
              <strong>{formatDate(currentUrl.expiresAt)}</strong>
            </article>
            <article>
              <span className="detail-label">Last accessed</span>
              <strong>{formatDate(currentUrl.lastAccessedAt)}</strong>
            </article>
            <article>
              <span className="detail-label">Redirect count</span>
              <strong>{currentUrl.redirectCount}</strong>
            </article>
          </div>
        ) : (
          <div className="empty-state">
            <p>No short URL loaded yet.</p>
            <p>Create one or fetch an existing code to populate this panel.</p>
          </div>
        )}
      </section>
    </main>
  );
}
