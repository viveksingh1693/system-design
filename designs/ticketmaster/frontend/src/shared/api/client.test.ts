import { beforeEach, describe, expect, it, vi } from "vitest";
import { holdBooking, resetApiCacheForTests, searchShows } from "./client";

function createJsonResponse(payload: unknown, status = 200): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      "Content-Type": "application/json"
    }
  });
}

describe("api client", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    resetApiCacheForTests();
  });

  it("caches hot show-search responses for a short TTL", async () => {
    const fetchMock = vi.fn().mockResolvedValue(createJsonResponse([]));
    vi.stubGlobal("fetch", fetchMock);

    await searchShows({ city: "Bengaluru" });
    await searchShows({ city: "Bengaluru" });

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("invalidates show caches after a booking hold", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(createJsonResponse([]))
      .mockResolvedValueOnce(
        createJsonResponse({
          id: 1,
          showId: 12,
          eventTitle: "Coldplay Night",
          startTime: "2026-04-01T19:30:00",
          customerEmail: "fan@example.com",
          seatCount: 2,
          totalAmount: 4998,
          status: "HELD",
          holdExpiresAt: "2026-04-01T19:40:00",
          paymentReference: null
        })
      )
      .mockResolvedValueOnce(createJsonResponse([]));

    vi.stubGlobal("fetch", fetchMock);

    await searchShows({ city: "Bengaluru" });
    await holdBooking({ showId: 12, customerEmail: "fan@example.com", seatCount: 2 });
    await searchShows({ city: "Bengaluru" });

    expect(fetchMock).toHaveBeenCalledTimes(3);
  });
});
