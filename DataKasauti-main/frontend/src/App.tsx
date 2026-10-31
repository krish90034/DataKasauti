import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";

import AppErrorBoundary from "./components/common/AppErrorBoundary";
import AppShell from "./components/layout/AppShell";
import { DataKasautiProvider } from "./context/DataKasautiContext";

// Load each page only when its route is opened.
const UploadPage = lazy(() => import("./pages/UploadPage"));
const ProfileResultPage = lazy(() => import("./pages/ProfileResultPage"));
const ComparePage = lazy(() => import("./pages/ComparePage"));
const CleaningPage = lazy(() => import("./pages/CleaningPage"));
const NotFoundPage = lazy(() => import("./pages/NotFoundPage"));

function RouteLoadingFallback() {
  return (
    <div
      role="status"
      aria-live="polite"
      style={{
        display: "grid",
        minHeight: "40vh",
        placeItems: "center",
        padding: "2rem",
      }}
    >
      Loading page...
    </div>
  );
}

export default function App() {
  return (
    <AppErrorBoundary>
      <DataKasautiProvider>
        <Suspense fallback={<RouteLoadingFallback />}>
          <Routes>
            <Route element={<AppShell />}>
              <Route path="/" element={<UploadPage />} />
              <Route path="/profile" element={<ProfileResultPage />} />
              <Route path="/compare" element={<ComparePage />} />
              <Route path="/clean" element={<CleaningPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </Suspense>
      </DataKasautiProvider>
    </AppErrorBoundary>
  );
}
