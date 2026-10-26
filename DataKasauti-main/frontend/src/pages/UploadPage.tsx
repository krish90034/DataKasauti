import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowRight,
  CheckCircle2,
  CloudUpload,
  Database,
  FileSpreadsheet,
  FileUp,
  GitCompareArrows,
  LockKeyhole,
  ScanSearch,
  ShieldCheck,
  Sparkles,
  X,
} from "lucide-react";

import { profileCsv } from "../services/profileService";
import { compareDatasets } from "../services/compareService";
import { ApiError } from "../services/apiClient";
import { useDataKasauti } from "../context/DataKasautiContext";

type Mode = "profile" | "compare";

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

interface FileDropzoneProps {
  id: string;
  label: string;
  helper: string;
  file: File | null;
  disabled: boolean;
  onFile: (file: File | null) => void;
}

function FileDropzone({
  id,
  label,
  helper,
  file,
  disabled,
  onFile,
}: FileDropzoneProps) {
  const [dragging, setDragging] = useState(false);

  return (
    <div className="min-w-0">
      <div className="mb-2 flex items-center justify-between gap-3">
        <label htmlFor={id} className="text-sm font-semibold text-[#344A43]">
          {label}
        </label>
        {file && (
          <span className="inline-flex items-center gap-1 text-xs font-medium text-[#0F766E]">
            <CheckCircle2 size={14} /> Selected
          </span>
        )}
      </div>

      <div
        onDragOver={(event) => {
          event.preventDefault();
          if (!disabled) setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => {
          event.preventDefault();
          setDragging(false);
          if (disabled) return;
          const droppedFile = event.dataTransfer.files?.[0];
          if (droppedFile) onFile(droppedFile);
        }}
        className={`rounded-2xl border-2 border-dashed transition duration-200 ${
          dragging
            ? "border-[#0F766E] bg-[#DDF1E8]"
            : file
              ? "border-[#92C9B8] bg-[#F1F8F3]"
              : "border-[#D9CBB5] bg-[#FBF7EF] hover:border-[#77B7A5] hover:bg-[#F6F5EC]"
        } ${disabled ? "opacity-60" : ""}`}
      >
        <input
          id={id}
          type="file"
          accept=".csv,text/csv"
          disabled={disabled}
          className="sr-only"
          onChange={(event) => {
            onFile(event.currentTarget.files?.[0] ?? null);
            event.currentTarget.value = "";
          }}
        />
        <label
          htmlFor={id}
          className={`flex min-h-[174px] cursor-pointer flex-col items-center justify-center px-4 py-5 text-center focus-within:outline-none ${disabled ? "cursor-not-allowed" : ""}`}
        >
          <span
            className={`mb-3 flex h-12 w-12 items-center justify-center rounded-2xl ${file ? "bg-[#DDF1E8] text-[#0F766E]" : "bg-white text-[#0F766E] shadow-sm"}`}
          >
            {file ? <FileSpreadsheet size={24} /> : <CloudUpload size={25} />}
          </span>
          <span className="max-w-full break-words text-sm font-semibold text-[#253532]">
            {file ? file.name : "Drop your CSV file here"}
          </span>
          <span className="mt-1 text-xs text-[#728078]">
            {file
              ? `${formatBytes(file.size)} · Ready to scan`
              : "or click to browse your computer"}
          </span>
          {!file && (
            <span className="mt-4 inline-flex items-center gap-2 rounded-xl bg-[#0F766E] px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-[#115E59]">
              <FileUp size={16} /> Choose CSV file
            </span>
          )}
          <span className="mt-3 text-[11px] text-[#8A938A]">{helper}</span>
        </label>
      </div>

      {file && (
        <div className="mt-2 flex items-center justify-between gap-3 rounded-xl border border-[#D9E7DF] bg-white px-3 py-2.5">
          <div className="flex min-w-0 items-center gap-2">
            <FileSpreadsheet size={17} className="shrink-0 text-[#0F766E]" />
            <span className="truncate text-xs font-medium text-[#465A54]">
              {file.name}
            </span>
          </div>
          <button
            type="button"
            disabled={disabled}
            onClick={() => onFile(null)}
            aria-label={`Remove ${label.toLowerCase()}`}
            className="rounded-lg p-1.5 text-[#728078] transition hover:bg-[#F5EFE5] hover:text-[#0B403D] disabled:opacity-50"
          >
            <X size={15} />
          </button>
        </div>
      )}
    </div>
  );
}

export default function UploadPage() {
  const navigate = useNavigate();
  const {
    setProfileFile,
    setProfileResult,
    setCompareBaselineFile,
    setCompareCurrentFile,
    setCompareResult,
  } = useDataKasauti();

  const [mode, setMode] = useState<Mode>("profile");
  const [primaryFile, setPrimaryFile] = useState<File | null>(null);
  const [comparisonFile, setComparisonFile] = useState<File | null>(null);
  const [keyColumn, setKeyColumn] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  function validateFile(file: File): string | null {
    if (!file.name.toLowerCase().endsWith(".csv"))
      return "Please select a CSV file.";
    if (file.size === 0) return "The selected file is empty.";
    if (file.size > 10 * 1024 * 1024) return "File size must be 10 MB or less.";
    return null;
  }

  function changeMode(nextMode: Mode) {
    setMode(nextMode);
    setMessage("");
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
    }

    setLoading(true);
    try {
      if (mode === "profile") {
        setProfileResult(null);
        const result = await profileCsv(primaryFile);
        setProfileFile(primaryFile);
        setProfileResult(result);
        navigate("/profile");
        return;
      }

      if (!comparisonFile) return;
      setCompareResult(null);
      const result = await compareDatasets(
        primaryFile,
        comparisonFile,
        keyColumn,
      );
      setCompareBaselineFile(primaryFile);
      setCompareCurrentFile(comparisonFile);
      setCompareResult(result);
      navigate("/compare");
    } catch (error) {
      setMessage(
        error instanceof ApiError
          ? error.message
          : "Could not reach DataKasauti. Check that the backend is running and try again.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto w-full max-w-[1440px] space-y-5">
      {/* Compact, high-contrast workspace banner */}
      <section className="relative isolate overflow-hidden rounded-[28px] bg-gradient-to-br from-[#115E59] via-[#0F766E] to-[#0B403D] px-6 py-7 text-[#FFFCF6] shadow-[0_18px_45px_rgba(11,64,61,0.15)] sm:px-8 lg:flex lg:min-h-[190px] lg:items-center lg:justify-between lg:gap-8 lg:px-10">
        <div className="pointer-events-none absolute -right-16 -top-24 -z-10 h-64 w-64 rounded-full border-[36px] border-white/5" />
        <div className="pointer-events-none absolute -bottom-28 right-44 -z-10 h-64 w-64 rounded-full bg-[#B8E1D3]/10 blur-2xl" />
        <div className="relative max-w-3xl">
          <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1.5 text-[11px] font-bold tracking-[0.16em] text-[#DFF5EA]">
            <span className="h-1.5 w-1.5 rounded-full bg-[#B8E1D3]" /> DATA
            QUALITY WORKSPACE
          </div>
          <h2 className="text-3xl font-bold leading-tight tracking-[-0.035em] sm:text-4xl lg:text-[42px]">
            Better data.{" "}
            <span className="text-[#C1E9D9]">Better decisions.</span>
          </h2>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-[#E1F0E9] sm:text-base">
            Profile, compare and clean your CSV datasets in one careful
            workflow—before the numbers reach your reports.
          </p>
        </div>
        <div className="relative mt-6 grid grid-cols-3 gap-2 lg:mt-0 lg:w-[360px] lg:shrink-0">
          <div className="rounded-2xl border border-white/15 bg-white/10 p-3 backdrop-blur-sm">
            <FileSpreadsheet size={18} className="text-[#C1E9D9]" />
            <p className="mt-3 text-lg font-bold">CSV</p>
            <p className="mt-0.5 text-[10px] leading-4 text-[#E1F0E9]">
              Supported file
            </p>
          </div>
          <div className="rounded-2xl border border-white/15 bg-white/10 p-3 backdrop-blur-sm">
            <Database size={18} className="text-[#C1E9D9]" />
            <p className="mt-3 text-lg font-bold">10 MB</p>
            <p className="mt-0.5 text-[10px] leading-4 text-[#E1F0E9]">
              Per-file limit
            </p>
          </div>
          <div className="rounded-2xl border border-white/15 bg-white/10 p-3 backdrop-blur-sm">
            <ShieldCheck size={18} className="text-[#C1E9D9]" />
            <p className="mt-3 text-lg font-bold">Review</p>
            <p className="mt-0.5 text-[10px] leading-4 text-[#E1F0E9]">
              Before export
            </p>
          </div>
        </div>
      </section>

      <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1.7fr)_minmax(300px,0.8fr)]">
        {/* Main workflow */}
        <section className="overflow-hidden rounded-[24px] border border-[#E3D8C6] bg-[#FFFCF6] shadow-[0_8px_28px_rgba(72,61,43,0.045)]">
          <div className="border-b border-[#EEE5D6] px-5 py-5 sm:px-7">
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-[#0F766E]">
                  Start here
                </p>
                <h3 className="mt-1 text-xl font-bold tracking-tight text-[#253532] sm:text-2xl">
                  What would you like to do?
                </h3>
                <p className="mt-1 text-sm text-[#728078]">
                  Choose a workflow, then add your data.
                </p>
              </div>
              <div className="inline-flex w-full rounded-xl border border-[#E3D8C6] bg-[#F5EFE5] p-1 sm:w-auto">
                <button
                  type="button"
                  disabled={loading}
                  onClick={() => changeMode("profile")}
                  aria-pressed={mode === "profile"}
                  className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition sm:flex-none ${mode === "profile" ? "bg-[#FFFCF6] text-[#0F766E] shadow-sm ring-1 ring-[#DED5C5]" : "text-[#728078] hover:text-[#253532]"}`}
                >
                  <ScanSearch size={17} /> Profile data
                </button>
                <button
                  type="button"
                  disabled={loading}
                  onClick={() => changeMode("compare")}
                  aria-pressed={mode === "compare"}
                  className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold transition sm:flex-none ${mode === "compare" ? "bg-[#FFFCF6] text-[#0F766E] shadow-sm ring-1 ring-[#DED5C5]" : "text-[#728078] hover:text-[#253532]"}`}
                >
                  <GitCompareArrows size={17} /> Compare files
                </button>
              </div>
            </div>
          </div>

          <div className="p-5 sm:p-7">
            <div
              className={`grid gap-4 ${mode === "compare" ? "md:grid-cols-2" : "grid-cols-1"}`}
            >
              <FileDropzone
                id="primary-file"
                label={
                  mode === "compare" ? "01 · Baseline dataset" : "Your dataset"
                }
                helper="CSV only · Up to 10 MB"
                file={primaryFile}
                disabled={loading}
                onFile={(file) => {
                  setPrimaryFile(file);
                  setMessage("");
                }}
              />
              {mode === "compare" && (
                <FileDropzone
                  id="comparison-file"
                  label="02 · Current dataset"
                  helper="CSV only · Up to 10 MB"
                  file={comparisonFile}
                  disabled={loading}
                  onFile={(file) => {
                    setComparisonFile(file);
                    setMessage("");
                  }}
                />
              )}
            </div>

            {mode === "compare" && (
              <div className="mt-5 rounded-2xl bg-[#F7F2E8] p-4 sm:p-5">
                <label
                  htmlFor="key-column"
                  className="block text-sm font-semibold text-[#344A43]"
                >
                  Unique key column{" "}
                  <span className="font-normal text-[#8A938A]">(optional)</span>
                </label>
                <input
                  id="key-column"
                  type="text"
                  value={keyColumn}
                  disabled={loading}
                  onChange={(event) => setKeyColumn(event.target.value)}
                  placeholder="e.g. order_id or customer_id"
                  className="mt-2 block w-full rounded-xl border border-[#E3D8C6] bg-[#FFFCF6] px-4 py-3 text-sm text-[#253532] outline-none transition placeholder:text-[#9A9F98] focus:border-[#0F766E] focus:ring-4 focus:ring-[#DDF1E8] disabled:bg-[#F5EFE5]"
                />
                <p className="mt-2 text-xs leading-5 text-[#728078]">
                  Use a column that is unique and non-empty in both files to
                  compare row-level changes.
                </p>
              </div>
            )}

            {message && (
              <div
                role="alert"
                className="mt-5 flex items-start gap-2 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800"
              >
                <span className="font-bold">!</span>
                <span>{message}</span>
              </div>
            )}

            <div className="mt-6 flex flex-col gap-3 border-t border-[#EEE5D6] pt-5 sm:flex-row sm:items-center sm:justify-between">
              <p className="max-w-lg text-xs leading-5 text-[#8A938A]">
                Your original upload is not modified. Cleaning changes are
                reviewed before a separate file is exported.
              </p>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={loading}
                className="inline-flex min-h-12 w-full shrink-0 items-center justify-center gap-2 rounded-xl bg-[#0F766E] px-5 py-3 text-sm font-bold text-white shadow-[0_6px_16px_rgba(15,118,110,0.18)] transition hover:-translate-y-0.5 hover:bg-[#115E59] hover:shadow-[0_10px_22px_rgba(15,118,110,0.22)] focus:outline-none focus:ring-4 focus:ring-[#DDF1E8] disabled:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
              >
                {loading
                  ? "Processing your data…"
                  : mode === "profile"
                    ? "Scan dataset"
                    : "Compare datasets"}
                {!loading && <ArrowRight size={17} />}
              </button>
            </div>
          </div>
        </section>

        {/* Right-hand guidance column */}
        <aside className="space-y-5">
          <section className="rounded-[24px] border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)] sm:p-6">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#DDF1E8] text-[#0F766E]">
                <Sparkles size={20} />
              </div>
              <div>
                <h3 className="font-bold text-[#253532]">Your workflow</h3>
                <p className="text-xs text-[#728078]">Three simple steps</p>
              </div>
            </div>
            <div className="mt-6 space-y-5">
              {[
                {
                  n: "01",
                  title: "Upload your data",
                  body: "Select a CSV file from your device.",
                },
                {
                  n: "02",
                  title:
                    mode === "profile"
                      ? "Inspect quality"
                      : "Compare differences",
                  body:
                    mode === "profile"
                      ? "Review missing values, duplicates and detected types."
                      : "Review column changes and numeric statistics.",
                },
                {
                  n: "03",
                  title: "Take the next step",
                  body: "Choose cleaning options or export after reviewing.",
                },
              ].map((step, index) => (
                <div key={step.n} className="relative flex gap-3">
                  {index < 2 && (
                    <span className="absolute left-[17px] top-10 h-7 w-px bg-[#D9E5DC]" />
                  )}
                  <span className="flex h-[35px] w-[35px] shrink-0 items-center justify-center rounded-xl bg-[#E0F0E9] text-xs font-extrabold text-[#0F766E]">
                    {step.n}
                  </span>
                  <div className="pt-0.5">
                    <p className="text-sm font-semibold text-[#344A43]">
                      {step.title}
                    </p>
                    <p className="mt-1 text-xs leading-5 text-[#728078]">
                      {step.body}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </section>

          <section className="rounded-[24px] border border-[#C9DED4] bg-gradient-to-br from-[#E6F3EC] to-[#F8F4E8] p-5 sm:p-6">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-[#0F766E] shadow-sm">
                <LockKeyhole size={19} />
              </div>
              <div>
                <h3 className="font-bold text-[#174C44]">Privacy by design</h3>
                <p className="mt-2 text-sm leading-6 text-[#536C61]">
                  Uploads are intended for temporary processing. The MVP does
                  not keep raw CSV files as permanent records.
                </p>
              </div>
            </div>
          </section>

          <div className="flex items-start gap-3 px-1 text-xs leading-5 text-[#8A938A]">
            <ShieldCheck size={16} className="mt-0.5 shrink-0 text-[#0F766E]" />
            <p>
              File checks in your browser are a first step. The backend must
              validate the CSV before processing.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
