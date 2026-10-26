import { apiRequest } from "./apiClient";
import type { CompareResponse } from "../types/compare";

export async function compareDatasets(
  baselineFile: File,
  currentFile: File,
  keyColumn?: string,
): Promise<CompareResponse> {
  const formData = new FormData();

  formData.append("baselineFile", baselineFile);
  formData.append("currentFile", currentFile);

  if (keyColumn?.trim()) {
    formData.append("key_column", keyColumn.trim());
  }

  return apiRequest<CompareResponse>("/api/v1/compare", {
    method: "POST",
    body: formData,
  });
}
