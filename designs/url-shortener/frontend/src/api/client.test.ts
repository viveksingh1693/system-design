import { ApiError, buildApiUrl, createShortUrl } from "./client";

describe("API client", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("builds relative URLs by default", () => {
    expect(buildApiUrl("/api/v1/urls")).toBe("/api/v1/urls");
  });

  it("throws a typed ApiError when the backend returns an error payload", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 409,
        json: async () => ({
          message: "customAlias is already in use",
          details: ["customAlias: already exists"],
          path: "/api/v1/urls"
        })
      } satisfies Partial<Response>)
    );

    await expect(createShortUrl({ originalUrl: "https://example.com", customAlias: "docs2026" })).rejects.toEqual(
      expect.objectContaining<ApiError>({
        name: "ApiError",
        status: 409,
        message: "customAlias is already in use",
        details: ["customAlias: already exists"],
        path: "/api/v1/urls"
      })
    );
  });
});
