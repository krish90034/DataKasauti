export type FillStrategy = "MEAN" | "MEDIAN" | "MODE" | "CONSTANT";

export type CleaningRule =
  | { type: "TRIM_WHITESPACE" }
  | { type: "REMOVE_DUPLICATES" }
  | {
      type: "DROP_ROWS_WITH_MISSING";
      columns: string[];
    }
  | {
      type: "FILL_MISSING";
      column: string;
      strategy: FillStrategy;
      value?: string;
    };

export interface RuleResult {
  type: CleaningRule["type"];
  column?: string;
  rows_affected: number;
  cells_changed: number;
}

export interface SampleChange {
  row_number: number;
  column: string;
  before: string | null;
  after: string | null;
}

export interface CleanPreviewResponse {
  rows_before: number;
  rows_after: number;
  rule_results: RuleResult[];
  sample_changes: SampleChange[];
  original_modified: boolean;
}
