const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/+$/, "");

interface ErrorResponse {
  timestamp?: string;
  status?: number;
  error_code?: string;
  message?: string;
  details?: string[];
}

export class ApiError extends Error {
  public readonly status: number;
  public readonly errorCode: string;
  public readonly details: string[];

  constructor(
    message: string,
    status: number,
    errorCode: string,
    details: string[] = [],
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errorCode = errorCode;
    this.details = details;
  }
}

async function parseApiError(response: Response): Promise<ApiError> {
  let errorData: ErrorResponse = {};

  try {
    errorData = (await response.json()) as ErrorResponse;
  } catch {
    // The server did not return a JSON error body.
  }

  return new ApiError(
    errorData.message || "The request failed.",
    response.status,
    errorData.error_code || "REQUEST_FAILED",
    errorData.details || [],
  );
}

async function sendRequest(
  path: string,
  options: RequestInit,
): Promise<Response> {
  try {
    return await fetch(`${API_BASE_URL}${path}`, options);
  } catch {
    throw new ApiError(
      "Cannot connect to the backend. Check whether the server is running.",
      0,
      "NETWORK_ERROR",
    );
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await sendRequest(path, options);

  if (!response.ok) {
    throw await parseApiError(response);
  }

  return (await response.json()) as T;
}

export async function apiBlobRequest(
  path: string,
  options: RequestInit = {},
): Promise<{ blob: Blob; filename: string }> {
  const response = await sendRequest(path, options);

  if (!response.ok) {
    throw await parseApiError(response);
  }

  const disposition = response.headers.get("content-disposition");
  const match = disposition?.match(/filename="?([^";]+)"?/i);

  return {
    blob: await response.blob(),
    filename: match?.[1] ?? "cleaned.csv",
  };
}
