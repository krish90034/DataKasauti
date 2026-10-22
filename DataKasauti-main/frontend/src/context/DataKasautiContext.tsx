import {
  createContext,
  useContext,
  useState,
  type Dispatch,
  type PropsWithChildren,
  type SetStateAction,
} from "react";

import type { ProfileResponse } from "../types/profile";

interface DataKasautiContextValue {
  profileFile: File | null;
  setProfileFile: Dispatch<SetStateAction<File | null>>;
  profileResult: ProfileResponse | null;
  setProfileResult: Dispatch<SetStateAction<ProfileResponse | null>>;
}

const DataKasautiContext = createContext<DataKasautiContextValue | null>(null);

export function DataKasautiProvider({ children }: PropsWithChildren) {
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [profileResult, setProfileResult] = useState<ProfileResponse | null>(
    null,
  );

  return (
    <DataKasautiContext.Provider
      value={{
        profileFile,
        setProfileFile,
        profileResult,
        setProfileResult,
      }}
    >
      {children}
    </DataKasautiContext.Provider>
  );
}

export function useDataKasauti() {
  const context = useContext(DataKasautiContext);

  if (!context) {
    throw new Error("useDataKasauti must be used inside DataKasautiProvider");
  }

  return context;
}
