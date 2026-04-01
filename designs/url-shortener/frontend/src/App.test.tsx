import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import App from "./App";

describe("App routes", () => {
  it("renders the dashboard on the home route", () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByRole("heading", { name: /short links with a calmer control room/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /create short url/i })).toBeInTheDocument();
  });

  it("renders the redirect helper page", () => {
    render(
      <MemoryRouter initialEntries={["/redirect-help?reason=expired&code=docs2026"]}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByRole("heading", { name: /this short link has expired/i })).toBeInTheDocument();
    expect(screen.getByText("docs2026")).toBeInTheDocument();
  });
});
