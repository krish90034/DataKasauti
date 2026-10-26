import { apiBlobRequest, apiRequest } from "./apiClient";

import type { CleaningRule, CleanPreviewResponse } from "../types/clean";

function createCleaningForm(file: File, rules: CleaningRule[]): FormData {
  const formData = new FormData();

  formData.append("file", file);
  formData.append("rules", JSON.stringify({ rules }));

  return formData;
}

export async function previewCleaning(
  file: File,
  rules: CleaningRule[],
): Promise<CleanPreviewResponse> {
  return apiRequest<CleanPreviewResponse>("/api/v1/clean/preview", {
    method: "POST",
    body: createCleaningForm(file, rules),
  });
}

export async function exportCleanedCsv(
  file: File,
  rules: CleaningRule[],
): Promise<{ blob: Blob; filename: string }> {
  return apiBlobRequest("/api/v1/clean/export", {
    method: "POST",
    body: createCleaningForm(file, rules),
  });
}
