import { Route, Routes } from "react-router-dom";

import AppErrorBoundary from "./components/common/AppErrorBoundary";
import AppShell from "./components/layout/AppShell";
import { DataKasautiProvider } from "./context/DataKasautiContext";

import UploadPage from "./pages/UploadPage";
import ProfileResultPage from "./pages/ProfileResultPage";
import ComparePage from "./pages/ComparePage";
import CleaningPage from "./pages/CleaningPage";
import NotFoundPage from "./pages/NotFoundPage";

export default function App() {
  return (
    <AppErrorBoundary>
      <DataKasautiProvider>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="/" element={<UploadPage />} />
            <Route path="/profile" element={<ProfileResultPage />} />
            <Route path="/compare" element={<ComparePage />} />
            <Route path="/clean" element={<CleaningPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </DataKasautiProvider>
    </AppErrorBoundary>
  );
}
