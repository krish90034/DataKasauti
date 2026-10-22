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

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, options);
  } catch {
    throw new ApiError(
      "Cannot connect to the backend. Check whether the server is running.",
      0,
      "NETWORK_ERROR",
    );
  }

  if (!response.ok) {
    let errorData: ErrorResponse = {};

    try {
      errorData = (await response.json()) as ErrorResponse;
    } catch {
      // The server did not return a JSON error body.
    }

    throw new ApiError(
      errorData.message || "The request failed.",
      response.status,
      errorData.error_code || "REQUEST_FAILED",
      errorData.details || [],
    );
  }

  return (await response.json()) as T;
}
