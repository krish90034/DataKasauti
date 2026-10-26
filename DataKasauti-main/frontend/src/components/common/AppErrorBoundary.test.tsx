import { afterEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import AppErrorBoundary from "./AppErrorBoundary";

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

function BrokenComponent(): never {
  throw new Error("Test rendering failure");
}

describe("AppErrorBoundary", () => {
  it("renders children normally", () => {
    render(
      <AppErrorBoundary>
        <h1>DataKasauti dashboard</h1>
      </AppErrorBoundary>,
    );

    expect(
      screen.getByRole("heading", { name: "DataKasauti dashboard" }),
    ).toBeInTheDocument();
  });

  it("shows a recovery screen after a rendering error", () => {
    vi.spyOn(console, "error").mockImplementation(() => {});

    render(
      <AppErrorBoundary>
        <BrokenComponent />
      </AppErrorBoundary>,
    );

    expect(
      screen.getByRole("heading", { name: "Something went wrong" }),
    ).toBeInTheDocument();
  });
});
