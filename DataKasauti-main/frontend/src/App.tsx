import { Route, Routes } from "react-router-dom";
import { DataKasautiProvider } from "./context/DataKasautiContext";

import AppShell from "./components/layout/AppShell";
import UploadPage from "./pages/UploadPage";
import ProfileResultPage from "./pages/ProfileResultPage";
import ComparePage from "./pages/ComparePage";
import CleaningPage from "./pages/CleaningPage";
import NotFoundPage from "./pages/NotFoundPage";

export default function App() {
  return (
    <DataKasautiProvider>
      <Routes>
        <Route element={<AppShell />}>
          <Route path="/" element={<UploadPage />} />
          <Route path="/profile" element={<ProfileResultPage />} />
          <Route path="/compare" element={<ComparePage />} />
          <Route path="/clean" element={<CleaningPage />} />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </DataKasautiProvider>
  );
}
