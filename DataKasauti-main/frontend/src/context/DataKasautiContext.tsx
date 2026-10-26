import {
  createContext,
  useContext,
  useState,
  type Dispatch,
  type PropsWithChildren,
  type SetStateAction,
} from "react";

import type { ProfileResponse } from "../types/profile";
import type { CompareResponse } from "../types/compare";

interface DataKasautiContextValue {
  profileFile: File | null;
  setProfileFile: Dispatch<SetStateAction<File | null>>;
  profileResult: ProfileResponse | null;
  setProfileResult: Dispatch<SetStateAction<ProfileResponse | null>>;

  compareBaselineFile: File | null;
  setCompareBaselineFile: Dispatch<SetStateAction<File | null>>;
  compareCurrentFile: File | null;
  setCompareCurrentFile: Dispatch<SetStateAction<File | null>>;
  compareResult: CompareResponse | null;
  setCompareResult: Dispatch<SetStateAction<CompareResponse | null>>;
}

const DataKasautiContext = createContext<DataKasautiContextValue | null>(null);

export function DataKasautiProvider({ children }: PropsWithChildren) {
  const [profileFile, setProfileFile] = useState<File | null>(null);
  const [profileResult, setProfileResult] = useState<ProfileResponse | null>(
    null,
  );

  const [compareBaselineFile, setCompareBaselineFile] = useState<File | null>(
    null,
  );
  const [compareCurrentFile, setCompareCurrentFile] = useState<File | null>(
    null,
  );
  const [compareResult, setCompareResult] = useState<CompareResponse | null>(
    null,
  );

  return (
    <DataKasautiContext.Provider
      value={{
        profileFile,
        setProfileFile,
        profileResult,
        setProfileResult,
        compareBaselineFile,
        setCompareBaselineFile,
        compareCurrentFile,
        setCompareCurrentFile,
        compareResult,
        setCompareResult,
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
