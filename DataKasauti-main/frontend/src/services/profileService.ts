import { apiRequest } from "./apiClient";
import type { ProfileResponse } from "../types/profile";

export async function profileCsv(file: File): Promise<ProfileResponse> {
  const formData = new FormData();

  formData.append("file", file);

  return apiRequest<ProfileResponse>("/profile", {
    method: "POST",
    body: formData,
  });
}
