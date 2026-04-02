import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, vi } from "vitest";
import App from "./App";

describe("App routes", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn((input: RequestInfo | URL) => {
        const url = typeof input === "string" ? input : input.toString();

        if (url.includes("/shows/14")) {
          return Promise.resolve(
            new Response(
              JSON.stringify({
                id: 14,
                eventId: 4,
                eventTitle: "Arena Night",
                performer: "The Weekenders",
                city: "Bengaluru",
                venueName: "Central Dome",
                startTime: "2026-04-01T19:30:00",
                price: 2499,
                totalSeats: 12000,
                availableSeats: 4200
              }),
              { status: 200, headers: { "Content-Type": "application/json" } }
            )
          );
        }

        return Promise.resolve(
          new Response(JSON.stringify([]), {
            status: 200,
            headers: { "Content-Type": "application/json" }
          })
        );
      })
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders the discovery page on the home route", () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByRole("heading", { name: /keep discovery fast, precise, and gentle on the backend/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /operations/i })).toBeInTheDocument();
  });

  it("renders the booking desk route", () => {
    render(
      <MemoryRouter initialEntries={["/bookings?showId=14"]}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByRole("heading", { name: /work the hold, confirm, and cancel flow from one place/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /hold seats/i })).toBeInTheDocument();
  });
});
