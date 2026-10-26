import { Link } from "react-router-dom";
import Plot from "react-plotly.js";
import {
  ArrowLeft,
  //ArrowDownRight,
  //ArrowUpRight,
  CheckCircle2,
  FileDiff,
  GitCompareArrows,
  TriangleAlert,
} from "lucide-react";

import { useDataKasauti } from "../context/DataKasautiContext";
import type { CommonColumnComparison } from "../types/compare";

function formatNumber(value: number | null | undefined): string {
  if (value == null || !Number.isFinite(value)) {
    return "—";
  }

  return new Intl.NumberFormat("en-US", {
    maximumFractionDigits: 2,
  }).format(value);
}

function formatDelta(value: number): string {
  return value > 0 ? `+${formatNumber(value)}` : formatNumber(value);
}

export default function ComparePage() {
  const { compareResult } = useDataKasauti();

  if (!compareResult) {
    return (
      <section className="mx-auto max-w-xl rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-8 text-center">
        <GitCompareArrows size={42} className="mx-auto text-[#0F766E]" />

        <h2 className="mt-4 text-xl font-bold text-[#253532]">
          No comparison results yet
        </h2>

        <p className="mt-2 text-sm leading-6 text-[#728078]">
          Select a baseline CSV and a current CSV to compare dataset changes.
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

  const result = compareResult;

  const schemaChangeCount =
    result.added_columns.length +
    result.removed_columns.length +
    result.type_changes.length;

  const numericColumns = result.common_columns.filter(
    (column: CommonColumnComparison) =>
      column.baseline.mean != null && column.current.mean != null,
  );

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* Header */}
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

        <h2 className="mt-1 text-2xl font-bold tracking-tight text-[#253532] sm:text-3xl">
          Dataset Comparison
        </h2>

        <p className="mt-2 text-sm text-[#728078]">
          Review schema changes and compare numerical statistics.
        </p>
      </div>

      {/* Compared files */}
      <div className="grid gap-4 md:grid-cols-2">
        {[
          {
            label: "Baseline dataset",
            file: result.baseline,
          },
          {
            label: "Current dataset",
            file: result.current,
          },
        ].map(({ label, file }) => (
          <section
            key={label}
            className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]"
          >
            <p className="text-sm font-medium text-[#728078]">{label}</p>

            <h3 className="mt-2 break-all font-semibold text-[#253532]">
              {file.filename}
            </h3>

            <div className="mt-4 flex gap-6 text-sm text-[#596B64]">
              <span>{formatNumber(file.row_count)} rows</span>
              <span>{formatNumber(file.column_count)} columns</span>
            </div>
          </section>
        ))}
      </div>

      {/* Summary */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
          <p className="text-sm text-[#728078]">Row Count Change</p>

          <p className="mt-3 text-2xl font-bold text-[#253532]">
            {formatDelta(result.row_count_delta)}
          </p>

          <p className="mt-1 text-xs text-[#728078]">
            Current minus baseline rows
          </p>
        </div>

        <div className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
          <p className="text-sm text-[#728078]">Schema Changes</p>

          <p className="mt-3 text-2xl font-bold text-[#253532]">
            {formatNumber(schemaChangeCount)}
          </p>

          <p className="mt-1 text-xs text-[#728078]">
            Added, removed, or type-changed columns
          </p>
        </div>

        <div className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
          <p className="text-sm text-[#728078]">Added Columns</p>

          <p className="mt-3 text-2xl font-bold text-[#0F766E]">
            {formatNumber(result.added_columns.length)}
          </p>

          <p className="mt-1 text-xs text-[#728078]">
            Present in current dataset only
          </p>
        </div>

        <div className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
          <p className="text-sm text-[#728078]">Warnings</p>

          <p className="mt-3 text-2xl font-bold text-[#253532]">
            {formatNumber(result.warnings.length)}
          </p>

          <p className="mt-1 text-xs text-[#728078]">
            Checks reported by the comparison engine
          </p>
        </div>
      </div>

      {/* Schema changes */}
      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
        <div className="flex items-center gap-3">
          <FileDiff size={22} className="text-[#0F766E]" />

          <div>
            <h3 className="text-lg font-semibold text-[#253532]">
              Schema Changes
            </h3>

            <p className="mt-1 text-sm text-[#728078]">
              Differences in column names and detected types.
            </p>
          </div>
        </div>

        {schemaChangeCount === 0 ? (
          <div className="mt-5 flex items-center gap-2 rounded-lg bg-[#E0F0E9] p-4 text-sm text-[#115E59]">
            <CheckCircle2 size={18} />
            No schema changes were reported.
          </div>
        ) : (
          <div className="mt-5 overflow-x-auto">
            <table className="w-full min-w-[550px] text-left">
              <thead className="bg-[#F5EFE5]">
                <tr>
                  <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                    Column
                  </th>

                  <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                    Change
                  </th>

                  <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                    Baseline Type
                  </th>

                  <th className="px-4 py-3 text-xs font-semibold uppercase text-[#728078]">
                    Current Type
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y divide-[#EEE5D6]">
                {result.added_columns.map((column) => (
                  <tr key={`added-${column}`}>
                    <td className="px-4 py-3 text-sm font-medium">{column}</td>
                    <td className="px-4 py-3">
                      <span className="rounded-full bg-[#E0F0E9] px-2 py-1 text-xs font-semibold text-[#115E59]">
                        Added
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-[#728078]">—</td>
                    <td className="px-4 py-3 text-sm text-[#728078]">—</td>
                  </tr>
                ))}

                {result.removed_columns.map((column) => (
                  <tr key={`removed-${column}`}>
                    <td className="px-4 py-3 text-sm font-medium">{column}</td>
                    <td className="px-4 py-3">
                      <span className="rounded-full bg-rose-50 px-2 py-1 text-xs font-semibold text-rose-800">
                        Removed
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-[#728078]">—</td>
                    <td className="px-4 py-3 text-sm text-[#728078]">—</td>
                  </tr>
                ))}

                {result.type_changes.map((change) => (
                  <tr key={`type-${change.column}`}>
                    <td className="px-4 py-3 text-sm font-medium">
                      {change.column}
                    </td>
                    <td className="px-4 py-3">
                      <span className="rounded-full bg-amber-50 px-2 py-1 text-xs font-semibold text-amber-800">
                        Type changed
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {change.baseline_type}
                    </td>
                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {change.current_type}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Numeric statistics */}
      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
        <h3 className="text-lg font-semibold text-[#253532]">
          Numeric Column Comparison
        </h3>

        <p className="mt-1 text-sm text-[#728078]">
          Compare the count, missing values, minimum, maximum, and mean for
          common numeric columns.
        </p>

        {result.common_columns.length === 0 ? (
          <p className="mt-5 text-sm text-[#728078]">
            No common numeric columns were returned.
          </p>
        ) : (
          <div className="mt-5 overflow-x-auto">
            <table className="w-full min-w-[700px] text-left">
              <thead className="bg-[#F5EFE5]">
                <tr>
                  {[
                    "Column",
                    "Baseline Mean",
                    "Current Mean",
                    "Mean Delta",
                    "Baseline Missing",
                    "Current Missing",
                  ].map((heading) => (
                    <th
                      key={heading}
                      className="whitespace-nowrap px-4 py-3 text-xs font-semibold uppercase text-[#728078]"
                    >
                      {heading}
                    </th>
                  ))}
                </tr>
              </thead>

              <tbody className="divide-y divide-[#EEE5D6]">
                {result.common_columns.map((column) => (
                  <tr key={column.name}>
                    <td className="px-4 py-3 text-sm font-semibold text-[#253532]">
                      {column.name}
                    </td>

                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {formatNumber(column.baseline.mean)}
                    </td>

                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {formatNumber(column.current.mean)}
                    </td>

                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {formatNumber(column.mean_delta)}
                    </td>

                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {formatNumber(column.baseline.missing)}
                    </td>

                    <td className="px-4 py-3 text-sm text-[#596B64]">
                      {formatNumber(column.current.missing)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Plotly charts */}
      {numericColumns.length > 0 && (
        <section className="space-y-4">
          <div>
            <h3 className="text-lg font-semibold text-[#253532]">
              Mean Comparison Charts
            </h3>

            <p className="mt-1 text-sm text-[#728078]">
              Each chart compares a numeric column's mean between datasets.
            </p>
          </div>

          <div className="grid gap-4 xl:grid-cols-2">
            {numericColumns.map((column) => (
              <div
                key={column.name}
                className="min-w-0 rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-4"
              >
                <h4 className="font-semibold text-[#253532]">{column.name}</h4>

                <Plot
                  data={[
                    {
                      type: "bar",
                      x: ["Baseline", "Current"],
                      y: [
                        column.baseline.mean as number,
                        column.current.mean as number,
                      ],
                      marker: {
                        color: ["#0F766E", "#C4A574"],
                      },
                      hovertemplate:
                        "Dataset: %{x}<br>Mean: %{y}<extra></extra>",
                    },
                  ]}
                  layout={{
                    autosize: true,
                    height: 280,
                    margin: { l: 55, r: 15, t: 15, b: 55 },
                    paper_bgcolor: "rgba(0,0,0,0)",
                    plot_bgcolor: "rgba(0,0,0,0)",
                    showlegend: false,
                    yaxis: {
                      title: { text: "Mean" },
                      automargin: true,
                    },
                  }}
                  config={{
                    responsive: true,
                    displayModeBar: false,
                  }}
                  useResizeHandler
                  style={{ width: "100%", height: "280px" }}
                />
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Optional key-based row comparison */}
      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
        <h3 className="text-lg font-semibold text-[#253532]">
          Row-Level Changes
        </h3>

        {result.row_changes ? (
          <div className="mt-4 grid gap-4 sm:grid-cols-3">
            <p className="text-sm text-[#596B64]">
              Added keys:{" "}
              <strong>
                {formatNumber(result.row_changes.added_keys_count)}
              </strong>
            </p>

            <p className="text-sm text-[#596B64]">
              Removed keys:{" "}
              <strong>
                {formatNumber(result.row_changes.removed_keys_count)}
              </strong>
            </p>

            <p className="text-sm text-[#596B64]">
              Unchanged keys:{" "}
              <strong>
                {formatNumber(result.row_changes.unchanged_keys_count)}
              </strong>
            </p>
          </div>
        ) : (
          <p className="mt-2 text-sm leading-6 text-[#728078]">
            Row-level changes were not calculated. The comparison requires an
            explicitly supplied key column that is unique in both datasets.
          </p>
        )}
      </section>

      {/* Warnings */}
      <section className="rounded-2xl border border-[#E3D8C6] bg-[#FFFCF6] p-5 shadow-[0_8px_28px_rgba(72,61,43,0.04)]">
        <div className="flex items-center gap-2">
          <TriangleAlert size={20} className="text-amber-600" />

          <h3 className="text-lg font-semibold text-[#253532]">
            Comparison Warnings
          </h3>
        </div>

        {result.warnings.length === 0 ? (
          <p className="mt-3 text-sm text-[#728078]">
            No warnings were returned by the comparison engine.
          </p>
        ) : (
          <div className="mt-4 space-y-3">
            {result.warnings.map((warning, index) => (
              <div
                key={`${warning.code}-${index}`}
                className="rounded-lg bg-amber-50 p-4"
              >
                <p className="text-sm font-semibold text-amber-900">
                  {warning.code.replace(/_/g, " ")}
                </p>

                <p className="mt-1 text-sm text-amber-800">{warning.message}</p>
              </div>
            ))}
          </div>
        )}
      </section>

      <div className="flex justify-end">
        <Link
          to="/"
          className="inline-flex items-center gap-2 rounded-lg border border-[#E3D8C6] bg-[#FFFCF6] px-5 py-3 text-sm font-semibold text-[#465A54] hover:bg-[#F5EFE5]"
        >
          <ArrowLeft size={16} />
          Compare another dataset
        </Link>
      </div>
    </div>
  );
}
