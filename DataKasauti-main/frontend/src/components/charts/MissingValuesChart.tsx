import Plot from "react-plotly.js";
import type { ColumnProfile } from "../../types/profile";

interface MissingValuesChartProps {
  columns: ColumnProfile[];
}

export default function MissingValuesChart({
  columns,
}: MissingValuesChartProps) {
  const entries = columns
    .filter((column) => column.missing_count > 0)
    .sort((a, b) => b.missing_count - a.missing_count);

  if (entries.length === 0) {
    return (
      <div className="rounded-xl border border-[#B7D8CC] bg-[#E0F0E9] px-4 py-6 text-sm text-[#115E59]">
        No missing values were reported in this dataset.
      </div>
    );
  }

  return (
    <div className="min-w-0">
      <Plot
        data={[
          {
            type: "bar",
            orientation: "h",
            x: entries.map((column) => column.missing_count),
            y: entries.map((column) => column.name),
            marker: {
              color: entries.map((column) =>
                column.missing_percent >= 30 ? "#C28B54" : "#0F766E",
              ),
              line: { color: "#FFFCF6", width: 1 },
            },
            hovertemplate: "%{y}<br>Missing values: %{x}<extra></extra>",
          },
        ]}
        layout={{
          autosize: true,
          height: Math.max(240, Math.min(560, entries.length * 42 + 80)),
          margin: { l: 125, r: 20, t: 12, b: 45 },
          paper_bgcolor: "rgba(0,0,0,0)",
          plot_bgcolor: "rgba(0,0,0,0)",
          showlegend: false,
          xaxis: {
            title: { text: "Missing cells" },
            rangemode: "tozero",
            gridcolor: "#E3D8C6",
            zerolinecolor: "#D7CBB8",
            tickfont: { color: "#60716A" },
            titlefont: { color: "#60716A" },
          },
          yaxis: {
            autorange: "reversed",
            automargin: true,
            tickfont: { color: "#344A43" },
          },
          font: {
            family: "Inter, ui-sans-serif, system-ui, sans-serif",
            color: "#253532",
          },
        }}
        config={{ responsive: true, displayModeBar: false }}
        useResizeHandler
        style={{ width: "100%" }}
      />
    </div>
  );
}
