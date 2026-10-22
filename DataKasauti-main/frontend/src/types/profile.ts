export type ColumnType = "INTEGER" | "DECIMAL" | "BOOLEAN" | "DATE" | "STRING";

export interface ColumnStats {
  min: number | null;
  max: number | null;
  mean: number | null;
  median: number | null;
}

export interface ColumnProfile {
  name: string;
  detected_type: ColumnType;
  missing_count: number;
  missing_percent: number;
  unique_count: number;
  stats: ColumnStats | null;
}

export interface ProfileWarning {
  code: string;
  severity: "LOW" | "MEDIUM" | "HIGH";
  message: string;
}

export interface ProfileResponse {
  filename: string;
  file_size_bytes: number;
  row_count: number;
  column_count: number;
  duplicate_rows: number;
  total_missing_values: number;
  missing_values: Record<string, number>;
  columns: ColumnProfile[];
  preview_rows: string[][];
  warnings: ProfileWarning[];
}
