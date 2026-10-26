import { Component } from "react";
import type { ErrorInfo, ReactNode } from "react";

interface AppErrorBoundaryProps {
  children: ReactNode;
}

interface AppErrorBoundaryState {
  hasError: boolean;
}

export default class AppErrorBoundary extends Component<
  AppErrorBoundaryProps,
  AppErrorBoundaryState
> {
  state: AppErrorBoundaryState = {
    hasError: false,
  };

  static getDerivedStateFromError(): AppErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error("DataKasauti rendering error:", error, info);
  }

  private handleReload = (): void => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
          <section className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
            <div className="mb-4 text-4xl" aria-hidden="true">
              ⚠️
            </div>

            <h1 className="text-2xl font-bold text-slate-900">
              Something went wrong
            </h1>

            <p className="mt-3 text-sm leading-6 text-slate-600">
              DataKasauti encountered an unexpected error. Reload the
              application to try again.
            </p>

            <button
              type="button"
              onClick={this.handleReload}
              className="mt-6 w-full rounded-lg bg-emerald-800 px-5 py-3 font-semibold text-white transition hover:bg-emerald-900"
            >
              Reload application
            </button>
          </section>
        </main>
      );
    }

    return this.props.children;
  }
}
