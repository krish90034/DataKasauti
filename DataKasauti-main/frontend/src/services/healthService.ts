import { apiRequest } from "./apiClient";
import type { HealthResponse } from "../types/health";

export function checkBackendHealth(): Promise<HealthResponse> {
  return apiRequest<HealthResponse>("/api/v1/health");
}
