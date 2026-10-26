import type { ColumnType, ProfileWarning } from "./profile";

export interface DatasetSummary {
  filename: string;
  row_count: number;
  column_count: number;
}

export interface NumericColumnStats {
  count: number;
  missing: number;
  min: number | null;
  max: number | null;
  mean: number | null;
}

export interface TypeChange {
  column: string;
  baseline_type: ColumnType;
  current_type: ColumnType;
}

export interface CommonColumnComparison {
  name: string;
  baseline: NumericColumnStats;
  current: NumericColumnStats;
  mean_delta: number | null;
}

export interface RowChanges {
  added_keys_count: number;
  removed_keys_count: number;
  unchanged_keys_count: number;
}

export interface CompareResponse {
  baseline: DatasetSummary;
  current: DatasetSummary;
  row_count_delta: number;
  added_columns: string[];
  removed_columns: string[];
  type_changes: TypeChange[];
  common_columns: CommonColumnComparison[];
  row_changes: RowChanges | null;
  warnings: ProfileWarning[];
}
