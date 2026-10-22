import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FileUp, GitCompareArrows, ScanSearch } from "lucide-react";

import { profileCsv } from "../services/profileService";
import { ApiError } from "../services/apiClient";
import { useDataKasauti } from "../context/DataKasautiContext";

type Mode = "profile" | "compare";

export default function UploadPage() {
  const navigate = useNavigate();

  const { setProfileFile, setProfileResult } = useDataKasauti();

  const [mode, setMode] = useState<Mode>("profile");
  const [primaryFile, setPrimaryFile] = useState<File | null>(null);
  const [comparisonFile, setComparisonFile] = useState<File | null>(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  function validateFile(file: File): string | null {
    if (!file.name.toLowerCase().endsWith(".csv")) {
      return "Please select a CSV file.";
    }

    if (file.size === 0) {
      return "The selected file is empty.";
    }

    if (file.size > 10 * 1024 * 1024) {
      return "File size must be 10 MB or less.";
    }

    return null;
  }

  async function handleSubmit() {
    setMessage("");

    if (!primaryFile) {
      setMessage("Please select a CSV file.");
      return;
    }

    const primaryError = validateFile(primaryFile);

    if (primaryError) {
      setMessage(primaryError);
      return;
    }

    if (mode === "compare") {
      if (!comparisonFile) {
        setMessage("Please select the second CSV file.");
        return;
      }

      const comparisonError = validateFile(comparisonFile);

      if (comparisonError) {
        setMessage(comparisonError);
        return;
      }

      setMessage(
        "Dataset comparison API is not connected yet. Profile scanning is available.",
      );
      return;
    }

    setLoading(true);

    try {
      const result = await profileCsv(primaryFile);

      setProfileFile(primaryFile);
      setProfileResult(result);

      navigate("/profile");
    } catch (error) {
      if (error instanceof ApiError) {
        setMessage(error.message);
      } else {
        setMessage("An unexpected error occurred.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-8">
        <p className="text-sm font-medium text-teal-700">
          DATAKASAUTI WORKSPACE
        </p>

        <h2 className="mt-2 text-3xl font-bold tracking-tight">
          Understand your data.
        </h2>

        <p className="mt-2 text-slate-500">
          Inspect data quality and identify potential data issues.
        </p>
      </div>

      <div className="mb-6 grid gap-4 sm:grid-cols-2">
        <button
          type="button"
          onClick={() => {
            setMode("profile");
            setMessage("");
          }}
          className={`rounded-xl border bg-white p-5 text-left ${
            mode === "profile"
              ? "border-teal-600 ring-2 ring-teal-100"
              : "border-slate-200 hover:border-slate-300"
          }`}
        >
          <ScanSearch className="text-teal-700" size={25} />

          <h3 className="mt-4 font-semibold">Profile a dataset</h3>

          <p className="mt-1 text-sm text-slate-500">
            Check missing values, duplicate rows and column types.
          </p>
        </button>

        <button
          type="button"
          onClick={() => {
            setMode("compare");
            setMessage("");
          }}
          className={`rounded-xl border bg-white p-5 text-left ${
            mode === "compare"
              ? "border-teal-600 ring-2 ring-teal-100"
              : "border-slate-200 hover:border-slate-300"
          }`}
        >
          <GitCompareArrows className="text-teal-700" size={25} />

          <h3 className="mt-4 font-semibold">Compare datasets</h3>

          <p className="mt-1 text-sm text-slate-500">
            Identify schema changes and compare numeric columns.
          </p>
        </button>
      </div>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 md:p-8">
        <div className="flex items-center gap-3">
          <FileUp className="text-teal-700" size={25} />

          <div>
            <h3 className="font-semibold">
              {mode === "profile"
                ? "Upload your CSV file"
                : "Upload both CSV files"}
            </h3>

            <p className="text-sm text-slate-500">
              CSV only · Maximum 10 MB per file
            </p>
          </div>
        </div>

        <label
          htmlFor="primary-file"
          className="mt-6 block text-sm font-medium"
        >
          {mode === "compare" ? "Baseline dataset" : "Dataset"}
        </label>

        <input
          id="primary-file"
          type="file"
          accept=".csv,text/csv"
          disabled={loading}
          onChange={(event) => {
            setPrimaryFile(event.currentTarget.files?.[0] ?? null);
            setMessage("");
          }}
          className="mt-2 block w-full rounded-lg border border-slate-200 p-3 text-sm"
        />

        {mode === "compare" && (
          <>
            <label
              htmlFor="comparison-file"
              className="mt-6 block text-sm font-medium"
            >
              Current dataset
            </label>

            <input
              id="comparison-file"
              type="file"
              accept=".csv,text/csv"
              disabled={loading}
              onChange={(event) => {
                setComparisonFile(event.currentTarget.files?.[0] ?? null);
                setMessage("");
              }}
              className="mt-2 block w-full rounded-lg border border-slate-200 p-3 text-sm"
            />
          </>
        )}

        {message && (
          <div
            role="alert"
            className="mt-5 rounded-lg bg-rose-50 p-3 text-sm text-rose-800"
          >
            {message}
          </div>
        )}

        <button
          type="button"
          onClick={handleSubmit}
          disabled={loading}
          className="mt-6 rounded-lg bg-teal-700 px-5 py-3 text-sm font-semibold text-white hover:bg-teal-800 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {loading
            ? "Scanning dataset..."
            : mode === "profile"
              ? "Scan dataset"
              : "Validate comparison files"}
        </button>
      </section>
    </div>
  );
}
