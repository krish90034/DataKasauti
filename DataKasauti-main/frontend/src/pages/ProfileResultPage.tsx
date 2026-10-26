import { Link } from "react-router-dom";
import {
  ArrowLeft,
  AlertTriangle,
  CheckCircle2,
  FileSpreadsheet,
  Rows3,
  ScanSearch,
  TriangleAlert,
} from "lucide-react";

import { useDataKasauti } from "../context/DataKasautiContext";
import MissingValuesChart from "../components/charts/MissingValuesChart";

import type {
  ColumnProfile,
  ColumnStats,
  ProfileWarning,
} from "../types/profile";

/* =====================================================
   HELPERS
===================================================== */

function formatNumber(value: number | null | undefined): string {
  if (value == null || !Number.isFinite(value)) {
    return "—";
  }

  return new Intl.NumberFormat("en-US", {
    maximumFractionDigits: 2,
  }).format(value);
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

/* =====================================================
   SUMMARY CARD
===================================================== */

interface SummaryCardProps {
  label: string;
  value: string;
  description: string;
  icon: typeof Rows3;
}

function SummaryCard({
  label,
  value,
  description,
  icon: Icon,
}: SummaryCardProps) {
  return (
    <div className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-medium text-[#728078]">{label}</p>

        <Icon
          size={20}
          className="shrink-0 text-[#0F766E]"
          aria-hidden="true"
        />
      </div>

      <p className="mt-3 text-2xl font-bold tracking-tight text-[#253532]">
        {value}
      </p>

      <p className="mt-1 text-xs text-[#728078]">{description}</p>
    </div>
  );
}

/* =====================================================
   NUMERIC STATISTICS CELL
===================================================== */

interface StatsCellProps {
  stats: ColumnStats | null;
  field: keyof ColumnStats;
}

function StatsCell({ stats, field }: StatsCellProps) {
  return (
    <td className="whitespace-nowrap px-4 py-3 text-sm text-[#596B64]">
      {formatNumber(stats?.[field])}
    </td>
  );
}

/* =====================================================
   WARNING ITEM
===================================================== */

function WarningItem({ warning }: { warning: ProfileWarning }) {
  const severityStyles: Record<string, string> = {
    HIGH: "bg-rose-100 text-rose-800",
    MEDIUM: "bg-amber-100 text-amber-800",
    LOW: "bg-sky-100 text-sky-800",
  };

  return (
    <div className="flex items-start gap-3 border-b border-[#EDE4D6] py-3 last:border-b-0">
      <AlertTriangle
        size={19}
        className="mt-0.5 shrink-0 text-amber-600"
        aria-hidden="true"
      />

      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-sm font-semibold text-[#344943]">
            {warning.code.replace(/_/g, " ")}
          </span>

          <span
            className={`rounded-full px-2 py-1 text-xs font-semibold ${
              severityStyles[warning.severity] ?? "bg-[#EFE7DB] text-[#465A54]"
            }`}
          >
            {warning.severity}
          </span>
        </div>

        <p className="mt-1 break-words text-sm text-[#596B64]">
          {warning.message}
        </p>
      </div>
    </div>
  );
}

/* =====================================================
   PROFILE RESULTS PAGE
===================================================== */

export default function ProfileResultPage() {
  const { profileResult } = useDataKasauti();

  /* No scan result available */

  if (!profileResult) {
    return (
      <section className="mx-auto max-w-xl rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-8 text-center">
        <FileSpreadsheet
          size={42}
          className="mx-auto text-[#0F766E]"
          aria-hidden="true"
        />

        <h2 className="mt-4 text-xl font-bold text-[#253532]">
          No scan results yet
        </h2>

        <p className="mt-2 text-sm leading-6 text-[#728078]">
          Upload a CSV file and run a data-quality scan to see its statistics,
          missing values, duplicates, and preview.
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

  /* Prepare API data */

  const columns = profileResult.columns ?? [];
  const warnings = profileResult.warnings ?? [];
  const previewRows = profileResult.preview_rows ?? [];

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* PAGE HEADER */}

      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <Link
            to="/"
            className="inline-flex items-center gap-2 text-sm font-medium text-[#0F766E] hover:text-[#153B38]"
          >
            <ArrowLeft size={16} />
            Back to Upload
          </Link>

          <p className="mt-4 text-sm font-medium text-[#0F766E]">
            DATAKASAUTI WORKSPACE
          </p>

          <h2 className="mt-1 break-words text-2xl font-bold tracking-tight text-[#253532] sm:text-3xl">
            Profile Results
          </h2>

          <p className="mt-2 break-all text-sm text-[#728078]">
            {profileResult.filename}
          </p>
        </div>

        <div className="flex items-center gap-2 self-start rounded-lg border border-[#B7D8CC] bg-[#E0F0E9] px-3 py-2 text-sm font-medium text-[#115E59]">
          <CheckCircle2 size={17} />
          Scan completed
        </div>
      </div>

      {/* SUMMARY CARDS */}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          label="Total Rows"
          value={formatNumber(profileResult.row_count)}
          description="Data records, excluding the header"
          icon={Rows3}
        />

        <SummaryCard
          label="Total Columns"
          value={formatNumber(profileResult.column_count)}
          description="Columns detected in the file"
          icon={FileSpreadsheet}
        />

        <SummaryCard
          label="Missing Values"
          value={formatNumber(profileResult.total_missing_values)}
          description="Empty or whitespace-only cells"
          icon={TriangleAlert}
        />

        <SummaryCard
          label="Duplicate Rows"
          value={formatNumber(profileResult.duplicate_rows)}
          description="Repeated rows after first occurrence"
          icon={ScanSearch}
        />
      </div>

      {/* FILE INFORMATION */}

      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
        <h3 className="font-semibold text-[#253532]">File Information</h3>

        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-[#728078]">
              File name
            </p>

            <p className="mt-1 break-all text-sm font-medium text-[#344943]">
              {profileResult.filename}
            </p>
          </div>

          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-[#728078]">
              File size
            </p>

            <p className="mt-1 text-sm font-medium text-[#344943]">
              {formatBytes(profileResult.file_size_bytes)}
            </p>
          </div>
        </div>
      </section>

      {/* PLOTLY MISSING VALUES CHART */}

      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
        <div>
          <h3 className="text-lg font-semibold text-[#253532]">
            Missing Values by Column
          </h3>

          <p className="mt-1 text-sm text-[#728078]">
            Number of missing cells detected in each column.
          </p>
        </div>

        <div className="mt-4">
          <MissingValuesChart columns={columns} />
        </div>
      </section>

      {/* COLUMN STATISTICS */}

      <section className="overflow-hidden rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6]">
        <div className="border-b border-[#E3D8C6] p-5">
          <h3 className="text-lg font-semibold text-[#253532]">
            Column Statistics
          </h3>

          <p className="mt-1 text-sm text-[#728078]">
            Data types, missing percentages, unique values, and numerical
            statistics.
          </p>
        </div>

        {columns.length === 0 ? (
          <p className="p-5 text-sm text-[#728078]">
            No column statistics were returned by the API.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[950px] border-collapse text-left">
              <thead className="bg-[#F5EFE5]">
                <tr>
                  {[
                    "Column",
                    "Detected Type",
                    "Missing",
                    "Missing %",
                    "Unique",
                    "Min",
                    "Max",
                    "Mean",
                    "Median",
                  ].map((heading) => (
                    <th
                      key={heading}
                      className="whitespace-nowrap px-4 py-3 text-xs font-semibold uppercase tracking-wide text-[#728078]"
                    >
                      {heading}
                    </th>
                  ))}
                </tr>
              </thead>

              <tbody className="divide-y divide-[#EEE5D6]">
                {columns.map((column: ColumnProfile) => (
                  <tr key={column.name} className="hover:bg-[#F5EFE5]">
                    <td className="max-w-48 break-all px-4 py-3 text-sm font-medium text-[#253532]">
                      {column.name}
                    </td>

                    <td className="px-4 py-3">
                      <span className="rounded-md bg-[#EFE7DB] px-2 py-1 text-xs font-medium text-[#465A54]">
                        {column.detected_type}
                      </span>
                    </td>

                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {formatNumber(column.missing_count)}
                    </td>

                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {formatNumber(column.missing_percent)}%
                    </td>

                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {formatNumber(column.unique_count)}
                    </td>

                    <StatsCell stats={column.stats} field="min" />
                    <StatsCell stats={column.stats} field="max" />
                    <StatsCell stats={column.stats} field="mean" />
                    <StatsCell stats={column.stats} field="median" />
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* WARNINGS */}

      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
        <h3 className="text-lg font-semibold text-[#253532]">
          Data Quality Warnings
        </h3>

        <p className="mt-1 text-sm text-[#728078]">
          Issues reported by the backend scanner.
        </p>

        {warnings.length === 0 ? (
          <div className="mt-4 flex items-center gap-2 rounded-lg bg-[#E0F0E9] p-4 text-sm text-[#115E59]">
            <CheckCircle2 size={18} />
            No warnings were returned by the scanner.
          </div>
        ) : (
          <div className="mt-3">
            {warnings.map((warning, index) => (
              <WarningItem key={`${warning.code}-${index}`} warning={warning} />
            ))}
          </div>
        )}
      </section>

      {/* DATA PREVIEW */}

      <section className="overflow-hidden rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6]">
        <div className="border-b border-[#E3D8C6] p-5">
          <h3 className="text-lg font-semibold text-[#253532]">Data Preview</h3>

          <p className="mt-1 text-sm text-[#728078]">
            Showing up to 20 rows returned by the backend.
          </p>
        </div>

        {previewRows.length === 0 ? (
          <p className="p-5 text-sm text-[#728078]">
            No preview rows were returned by the API.
          </p>
        ) : columns.length === 0 ? (
          <p className="p-5 text-sm text-[#728078]">
            Preview rows are available, but column metadata is missing.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-max border-collapse text-left">
              <thead className="bg-[#F5EFE5]">
                <tr>
                  <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                    #
                  </th>

                  {columns.map((column) => (
                    <th
                      key={column.name}
                      className="whitespace-nowrap px-4 py-3 text-xs font-semibold uppercase text-[#728078]"
                    >
                      {column.name}
                    </th>
                  ))}
                </tr>
              </thead>

              <tbody className="divide-y divide-[#EEE5D6]">
                {previewRows.map((row, rowIndex) => (
                  <tr key={rowIndex} className="hover:bg-[#F5EFE5]">
                    <td className="px-4 py-3 text-xs text-[#8C958F]">
                      {rowIndex + 1}
                    </td>

                    {columns.map((column, columnIndex) => (
                      <td
                        key={`${column.name}-${columnIndex}`}
                        className="max-w-56 truncate px-4 py-3 text-sm text-[#596B64]"
                        title={row[columnIndex] ?? ""}
                      >
                        {row[columnIndex] ?? ""}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* NEXT ACTION */}

      <div className="flex justify-end">
        <Link
          to="/clean"
          className="inline-flex items-center gap-2 rounded-lg bg-[#0F766E] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#115E59]"
        >
          Review Cleaning Options
          <ArrowLeft size={16} className="rotate-180" />
        </Link>
      </div>
    </div>
  );
}
