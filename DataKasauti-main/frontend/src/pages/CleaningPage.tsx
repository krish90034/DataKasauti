import { useState } from "react";
import { Link } from "react-router-dom";
import {
  ArrowLeft,
  CheckCircle2,
  Download,
  Eraser,
  FileSpreadsheet,
  LoaderCircle,
  ShieldCheck,
  WandSparkles,
} from "lucide-react";

import { useDataKasauti } from "../context/DataKasautiContext";
import { ApiError } from "../services/apiClient";
import { exportCleanedCsv, previewCleaning } from "../services/cleanService";

import type {
  CleaningRule,
  CleanPreviewResponse,
  FillStrategy,
} from "../types/clean";

const FILL_STRATEGIES: FillStrategy[] = ["MEAN", "MEDIAN", "MODE", "CONSTANT"];

function formatNumber(value: number): string {
  return new Intl.NumberFormat("en-US").format(value);
}

function formatRuleName(value: string): string {
  return value.replace(/_/g, " ");
}

export default function CleaningPage() {
  const { profileFile, profileResult } = useDataKasauti();

  const columns = profileResult?.columns ?? [];

  const [trimWhitespace, setTrimWhitespace] = useState(false);
  const [removeDuplicates, setRemoveDuplicates] = useState(false);
  const [dropMissing, setDropMissing] = useState(false);
  const [fillMissing, setFillMissing] = useState(false);

  const [dropMissingColumn, setDropMissingColumn] = useState("");
  const [fillColumn, setFillColumn] = useState("");
  const [fillStrategy, setFillStrategy] = useState<FillStrategy>("MODE");
  const [constantValue, setConstantValue] = useState("");

  const [preview, setPreview] = useState<CleanPreviewResponse | null>(null);
  const [previewRules, setPreviewRules] = useState<CleaningRule[] | null>(null);

  const [loadingPreview, setLoadingPreview] = useState(false);
  const [loadingExport, setLoadingExport] = useState(false);
  const [message, setMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  function invalidatePreview() {
    setPreview(null);
    setPreviewRules(null);
    setMessage("");
    setSuccessMessage("");
  }

  function buildRules(): CleaningRule[] {
    const rules: CleaningRule[] = [];

    // Keep this order consistent with the backend contract.
    if (trimWhitespace) {
      rules.push({ type: "TRIM_WHITESPACE" });
    }

    if (removeDuplicates) {
      rules.push({ type: "REMOVE_DUPLICATES" });
    }

    if (dropMissing) {
      rules.push({
        type: "DROP_ROWS_WITH_MISSING",
        columns: [dropMissingColumn || columns[0]?.name || ""],
      });
    }

    if (fillMissing) {
      rules.push({
        type: "FILL_MISSING",
        column: fillColumn || columns[0]?.name || "",
        strategy: fillStrategy,
        ...(fillStrategy === "CONSTANT" ? { value: constantValue } : {}),
      });
    }

    return rules;
  }

  function validateRules(rules: CleaningRule[]): string | null {
    if (rules.length === 0) {
      return "Select at least one cleaning rule.";
    }

    if (dropMissing && !(dropMissingColumn || columns[0]?.name)) {
      return "Select a column for the missing-value row rule.";
    }

    if (!(fillColumn || columns[0]?.name) && fillMissing) {
      return "Select a column to fill missing values.";
    }

    const selectedFillColumn = columns.find(
      (column) => column.name === (fillColumn || columns[0]?.name),
    );

    if (
      fillMissing &&
      (fillStrategy === "MEAN" || fillStrategy === "MEDIAN") &&
      selectedFillColumn &&
      !["INTEGER", "DECIMAL"].includes(selectedFillColumn.detected_type)
    ) {
      return "MEAN and MEDIAN can only be used with numeric columns.";
    }

    if (fillMissing && fillStrategy === "CONSTANT" && !constantValue.trim()) {
      return "Enter a constant value before generating the preview.";
    }

    return null;
  }

  async function handlePreview() {
    setMessage("");
    setSuccessMessage("");
    setPreview(null);
    setPreviewRules(null);

    if (!profileFile) {
      setMessage("Upload and profile a CSV file before cleaning.");
      return;
    }

    const rules = buildRules();
    const validationError = validateRules(rules);

    if (validationError) {
      setMessage(validationError);
      return;
    }

    setLoadingPreview(true);

    try {
      const result = await previewCleaning(profileFile, rules);

      setPreview(result);
      setPreviewRules(rules);
      setSuccessMessage(
        "Preview generated. Your original file has not been modified.",
      );
    } catch (error) {
      setMessage(
        error instanceof ApiError
          ? error.message
          : "Could not generate the cleaning preview.",
      );
    } finally {
      setLoadingPreview(false);
    }
  }

  async function handleExport() {
    if (!profileFile || !preview || !previewRules) {
      setMessage("Generate and review a preview before exporting.");
      return;
    }

    setMessage("");
    setSuccessMessage("");
    setLoadingExport(true);

    try {
      // Export exactly the same rules that produced the preview.
      const result = await exportCleanedCsv(profileFile, previewRules);

      const downloadUrl = URL.createObjectURL(result.blob);
      const anchor = document.createElement("a");

      anchor.href = downloadUrl;
      anchor.download = result.filename;

      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();

      window.setTimeout(() => {
        URL.revokeObjectURL(downloadUrl);
      }, 1000);

      setSuccessMessage(`Cleaned CSV downloaded as ${result.filename}.`);
    } catch (error) {
      setMessage(
        error instanceof ApiError
          ? error.message
          : "Could not export the cleaned CSV.",
      );
    } finally {
      setLoadingExport(false);
    }
  }

  if (!profileFile || !profileResult) {
    return (
      <section className="mx-auto max-w-xl rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-8 text-center">
        <FileSpreadsheet size={42} className="mx-auto text-[#0F766E]" />

        <h2 className="mt-4 text-xl font-bold text-[#253532]">
          Profile a dataset first
        </h2>

        <p className="mt-2 text-sm leading-6 text-[#728078]">
          DataKasauti needs a successfully profiled CSV before you can select
          cleaning rules.
        </p>

        <Link
          to="/"
          className="mt-6 inline-flex items-center gap-2 rounded-lg bg-[#0F766E] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#115E59]"
        >
          <ArrowLeft size={17} />
          Back to Upload
        </Link>
      </section>
    );
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* Header */}
      <div>
        <Link
          to="/profile"
          className="inline-flex items-center gap-2 text-sm font-medium text-[#0F766E] hover:text-[#153B38]"
        >
          <ArrowLeft size={16} />
          Back to Profile Results
        </Link>

        <p className="mt-4 text-sm font-medium text-[#0F766E]">
          DATAKASAUTI WORKSPACE
        </p>

        <h2 className="mt-1 text-2xl font-bold tracking-tight text-[#253532] sm:text-3xl">
          Data Cleaning
        </h2>

        <p className="mt-2 text-sm text-[#728078]">
          {profileFile.name} · Choose rules, review the preview, and approve a
          new cleaned file.
        </p>
      </div>

      {/* Safety notice */}
      <div className="flex items-start gap-3 rounded-xl border border-[#B7D8CC] bg-[#E0F0E9] p-4">
        <ShieldCheck size={22} className="mt-0.5 shrink-0 text-[#115E59]" />

        <div>
          <p className="font-semibold text-[#153B38]">
            Original data stays unchanged
          </p>

          <p className="mt-1 text-sm leading-6 text-[#115E59]">
            DataKasauti generates a preview first. The backend should export a
            separate cleaned CSV only after you approve it.
          </p>
        </div>
      </div>

      {/* File summary */}
      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
        <div className="flex items-center gap-3">
          <FileSpreadsheet size={24} className="text-[#0F766E]" />

          <div className="min-w-0">
            <h3 className="break-all font-semibold text-[#253532]">
              {profileFile.name}
            </h3>

            <p className="mt-1 text-sm text-[#728078]">
              {formatNumber(profileResult.row_count)} rows ·{" "}
              {formatNumber(profileResult.column_count)} columns ·{" "}
              {formatNumber(profileResult.duplicate_rows)} duplicate rows
            </p>
          </div>
        </div>
      </section>

      {/* Rules */}
      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)] md:p-6">
        <div className="flex items-center gap-3">
          <Eraser size={23} className="text-[#0F766E]" />

          <div>
            <h3 className="text-lg font-semibold text-[#253532]">
              Select Cleaning Rules
            </h3>

            <p className="mt-1 text-sm text-[#728078]">
              Only the rules you select will be applied.
            </p>
          </div>
        </div>

        <div className="mt-6 space-y-5">
          {/* Trim whitespace */}
          <label className="flex cursor-pointer items-start gap-3">
            <input
              type="checkbox"
              checked={trimWhitespace}
              disabled={loadingPreview || loadingExport}
              onChange={(event) => {
                setTrimWhitespace(event.currentTarget.checked);
                invalidatePreview();
              }}
              className="mt-1 h-4 w-4 accent-[#0F766E]"
            />

            <span>
              <span className="block text-sm font-semibold text-[#344943]">
                Trim whitespace
              </span>

              <span className="mt-1 block text-sm text-[#728078]">
                Remove leading and trailing whitespace from cell values.
              </span>
            </span>
          </label>

          {/* Remove duplicates */}
          <label className="flex cursor-pointer items-start gap-3">
            <input
              type="checkbox"
              checked={removeDuplicates}
              disabled={loadingPreview || loadingExport}
              onChange={(event) => {
                setRemoveDuplicates(event.currentTarget.checked);
                invalidatePreview();
              }}
              className="mt-1 h-4 w-4 accent-[#0F766E]"
            />

            <span>
              <span className="block text-sm font-semibold text-[#344943]">
                Remove duplicate rows
              </span>

              <span className="mt-1 block text-sm text-[#728078]">
                Keep the first occurrence of each exact duplicate row.
              </span>
            </span>
          </label>

          {/* Drop rows with missing values */}
          <div className="rounded-xl border border-[#E3D8C6] p-4">
            <label className="flex cursor-pointer items-start gap-3">
              <input
                type="checkbox"
                checked={dropMissing}
                disabled={loadingPreview || loadingExport}
                onChange={(event) => {
                  setDropMissing(event.currentTarget.checked);
                  invalidatePreview();
                }}
                className="mt-1 h-4 w-4 accent-[#0F766E]"
              />

              <span>
                <span className="block text-sm font-semibold text-[#344943]">
                  Drop rows with missing values
                </span>

                <span className="mt-1 block text-sm text-[#728078]">
                  Remove rows where the selected column is missing.
                </span>
              </span>
            </label>

            {dropMissing && (
              <div className="mt-4">
                <label
                  htmlFor="drop-missing-column"
                  className="block text-sm font-medium text-[#465A54]"
                >
                  Column to check
                </label>

                <select
                  id="drop-missing-column"
                  value={dropMissingColumn || columns[0]?.name || ""}
                  disabled={loadingPreview || loadingExport}
                  onChange={(event) => {
                    setDropMissingColumn(event.currentTarget.value);
                    invalidatePreview();
                  }}
                  className="mt-2 w-full rounded-lg border border-[#E3D8C6] bg-[#FFFCF6] px-3 py-2.5 text-sm"
                >
                  {columns.map((column) => (
                    <option key={column.name} value={column.name}>
                      {column.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {/* Fill missing values */}
          <div className="rounded-xl border border-[#E3D8C6] p-4">
            <label className="flex cursor-pointer items-start gap-3">
              <input
                type="checkbox"
                checked={fillMissing}
                disabled={loadingPreview || loadingExport}
                onChange={(event) => {
                  setFillMissing(event.currentTarget.checked);
                  invalidatePreview();
                }}
                className="mt-1 h-4 w-4 accent-[#0F766E]"
              />

              <span>
                <span className="block text-sm font-semibold text-[#344943]">
                  Fill missing values
                </span>

                <span className="mt-1 block text-sm text-[#728078]">
                  Replace missing values in one selected column using a chosen
                  strategy.
                </span>
              </span>
            </label>

            {fillMissing && (
              <div className="mt-4 grid gap-4 sm:grid-cols-2">
                <div>
                  <label
                    htmlFor="fill-column"
                    className="block text-sm font-medium text-[#465A54]"
                  >
                    Column
                  </label>

                  <select
                    id="fill-column"
                    value={fillColumn || columns[0]?.name || ""}
                    disabled={loadingPreview || loadingExport}
                    onChange={(event) => {
                      setFillColumn(event.currentTarget.value);
                      invalidatePreview();
                    }}
                    className="mt-2 w-full rounded-lg border border-[#E3D8C6] bg-[#FFFCF6] px-3 py-2.5 text-sm"
                  >
                    {columns.map((column) => (
                      <option key={column.name} value={column.name}>
                        {column.name} ({column.detected_type})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label
                    htmlFor="fill-strategy"
                    className="block text-sm font-medium text-[#465A54]"
                  >
                    Strategy
                  </label>

                  <select
                    id="fill-strategy"
                    value={fillStrategy}
                    disabled={loadingPreview || loadingExport}
                    onChange={(event) => {
                      setFillStrategy(
                        event.currentTarget.value as FillStrategy,
                      );
                      invalidatePreview();
                    }}
                    className="mt-2 w-full rounded-lg border border-[#E3D8C6] bg-[#FFFCF6] px-3 py-2.5 text-sm"
                  >
                    {FILL_STRATEGIES.map((strategy) => (
                      <option key={strategy} value={strategy}>
                        {strategy}
                      </option>
                    ))}
                  </select>
                </div>

                {fillStrategy === "CONSTANT" && (
                  <div className="sm:col-span-2">
                    <label
                      htmlFor="fill-constant"
                      className="block text-sm font-medium text-[#465A54]"
                    >
                      Constant value
                    </label>

                    <input
                      id="fill-constant"
                      type="text"
                      value={constantValue}
                      disabled={loadingPreview || loadingExport}
                      onChange={(event) => {
                        setConstantValue(event.currentTarget.value);
                        invalidatePreview();
                      }}
                      placeholder="Enter replacement value"
                      className="mt-2 w-full rounded-lg border border-[#E3D8C6] px-3 py-2.5 text-sm"
                    />
                  </div>
                )}

                <p className="text-xs leading-5 text-[#728078] sm:col-span-2">
                  MEAN and MEDIAN require a numeric column. MODE can be used
                  with categorical columns, subject to backend validation.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Messages */}
        {message && (
          <div
            role="alert"
            className="mt-5 rounded-lg bg-rose-50 p-3 text-sm text-rose-800"
          >
            {message}
          </div>
        )}

        {successMessage && (
          <div
            role="status"
            className="mt-5 flex items-start gap-2 rounded-lg bg-[#E0F0E9] p-3 text-sm text-[#115E59]"
          >
            <CheckCircle2 size={18} className="mt-0.5 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        <button
          type="button"
          onClick={handlePreview}
          disabled={loadingPreview || loadingExport}
          className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-[#0F766E] px-5 py-3 text-sm font-semibold text-white hover:bg-[#115E59] disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
        >
          {loadingPreview ? (
            <LoaderCircle size={17} className="animate-spin" />
          ) : (
            <WandSparkles size={17} />
          )}

          {loadingPreview ? "Generating preview..." : "Generate Preview"}
        </button>
      </section>

      {/* Preview result */}
      {preview && (
        <section className="space-y-5 rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)] md:p-6">
          <div>
            <p className="text-sm font-medium text-[#0F766E]">
              REVIEW BEFORE EXPORT
            </p>

            <h3 className="mt-1 text-xl font-bold text-[#253532]">
              Cleaning Preview
            </h3>

            <p className="mt-1 text-sm text-[#728078]">
              Review the changes before downloading the cleaned CSV.
            </p>
          </div>

          {/* Before/after rows */}
          <div className="grid gap-4 sm:grid-cols-3">
            <PreviewMetric label="Rows Before" value={preview.rows_before} />

            <PreviewMetric label="Rows After" value={preview.rows_after} />

            <PreviewMetric
              label="Rows Removed"
              value={preview.rows_before - preview.rows_after}
            />
          </div>

          {/* Per-rule results */}
          <div>
            <h4 className="font-semibold text-[#253532]">Rule Results</h4>

            <div className="mt-3 overflow-x-auto rounded-lg border border-[#E3D8C6]">
              <table className="w-full min-w-[500px] text-left">
                <thead className="bg-[#F5EFE5]">
                  <tr>
                    <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                      Rule
                    </th>
                    <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                      Rows Affected
                    </th>
                    <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                      Cells Changed
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-[#EEE5D6]">
                  {preview.rule_results.map((rule, index) => (
                    <tr key={`${rule.type}-${index}`}>
                      <td className="px-4 py-3 text-sm font-medium text-[#344943]">
                        {formatRuleName(rule.type)}
                        {rule.column ? ` — ${rule.column}` : ""}
                      </td>

                      <td className="px-4 py-3 text-sm text-[#596B64]">
                        {formatNumber(rule.rows_affected)}
                      </td>

                      <td className="px-4 py-3 text-sm text-[#596B64]">
                        {formatNumber(rule.cells_changed)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Sample changes */}
          <div>
            <h4 className="font-semibold text-[#253532]">Sample Changes</h4>

            {preview.sample_changes.length === 0 ? (
              <p className="mt-2 text-sm text-[#728078]">
                No individual cell changes were returned. Check the rule results
                above for row-level changes.
              </p>
            ) : (
              <div className="mt-3 overflow-x-auto rounded-lg border border-[#E3D8C6]">
                <table className="w-full min-w-[550px] text-left">
                  <thead className="bg-[#F5EFE5]">
                    <tr>
                      <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                        Row
                      </th>
                      <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                        Column
                      </th>
                      <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                        Before
                      </th>
                      <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                        After
                      </th>
                    </tr>
                  </thead>

                  <tbody className="divide-y divide-[#EEE5D6]">
                    {preview.sample_changes.map((change, index) => (
                      <tr
                        key={`${change.row_number}-${change.column}-${index}`}
                      >
                        <td className="px-4 py-3 text-sm text-[#596B64]">
                          {change.row_number}
                        </td>

                        <td className="px-4 py-3 text-sm font-medium text-[#344943]">
                          {change.column}
                        </td>

                        <td className="max-w-48 break-all px-4 py-3 text-sm text-rose-700">
                          {change.before ?? "—"}
                        </td>

                        <td className="max-w-48 break-all px-4 py-3 text-sm text-[#0F766E]">
                          {change.after ?? "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="flex items-start gap-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
            <ShieldCheck size={18} className="mt-0.5 shrink-0" />
            <p>
              Confirm the row counts and sample changes before approving the
              export. The original uploaded file must remain unchanged.
            </p>
          </div>

          <button
            type="button"
            onClick={handleExport}
            disabled={loadingExport || loadingPreview || !previewRules}
            className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-[#0F766E] px-5 py-3 text-sm font-semibold text-white hover:bg-[#115E59] disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
          >
            {loadingExport ? (
              <LoaderCircle size={17} className="animate-spin" />
            ) : (
              <Download size={17} />
            )}

            {loadingExport
              ? "Preparing download..."
              : "Approve & Download Cleaned CSV"}
          </button>
        </section>
      )}
    </div>
  );
}

function PreviewMetric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-[#E3D8C6] bg-[#F5EFE5] p-4">
      <p className="text-sm text-[#728078]">{label}</p>
      <p className="mt-2 text-2xl font-bold text-[#253532]">
        {formatNumber(value)}
      </p>
    </div>
  );
}
