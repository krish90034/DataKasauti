import Plot from "react-plotly.js";
import type { ColumnProfile } from "../../types/profile";

interface MissingValuesChartProps {
  columns: ColumnProfile[];
}

export default function MissingValuesChart({
  columns,
}: MissingValuesChartProps) {
  const missingColumns = columns
    .filter((column) => column.missing_count > 0)
    .sort((a, b) => b.missing_count - a.missing_count);

  if (missingColumns.length === 0) {
    return (
      <div className="rounded-lg bg-emerald-50 p-4 text-sm text-emerald-800">
        No missing values were detected.
      </div>
    );
  }

  return (
    <div className="w-full overflow-hidden">
      <Plot
        data={[
          {
            type: "bar",
            x: missingColumns.map((column) => column.name),
            y: missingColumns.map((column) => column.missing_count),
            marker: {
              color: "#0f766e",
              line: {
                color: "#115e59",
                width: 1,
              },
            },
            hovertemplate:
              "Column: %{x}<br>Missing values: %{y}<extra></extra>",
          },
        ]}
        layout={{
          autosize: true,
          height: 320,
          margin: {
            l: 55,
            r: 20,
            t: 20,
            b: 80,
          },
          paper_bgcolor: "rgba(0,0,0,0)",
          plot_bgcolor: "rgba(0,0,0,0)",
          font: {
            family: "Inter, sans-serif",
            color: "#475569",
          },
          xaxis: {
            title: { text: "Column" },
            automargin: true,
          },
          yaxis: {
            title: { text: "Missing values" },
            rangemode: "tozero",
            dtick: 1,
          },
          showlegend: false,
        }}
        config={{
          responsive: true,
          displayModeBar: false,
        }}
        useResizeHandler
        style={{
          width: "100%",
          height: "320px",
        }}
      />
    </div>
  );
}
