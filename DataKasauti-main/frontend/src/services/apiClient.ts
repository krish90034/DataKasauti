const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1"
).replace(/\/+$/, "");

interface ErrorResponse {
  timestamp?: string;
  status?: number;
  error_code?: string;
  message?: string;
  details?: unknown;
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

function normalizeDetails(details: unknown): string[] {
  if (Array.isArray(details)) {
    return details.map((item) =>
      typeof item === "string" ? item : JSON.stringify(item),
    );
  }

  if (typeof details === "string") {
    return details ? [details] : [];
  }

  if (details !== null && typeof details === "object") {
    return Object.entries(details as Record<string, unknown>).map(
      ([key, value]) => {
        const formattedValue =
          typeof value === "string" ? value : JSON.stringify(value);

        return `${key}: ${formattedValue}`;
      },
    );
  }

  return [];
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
    normalizeDetails(errorData.details),
  );
}

async function sendRequest(
  path: string,
  options: RequestInit = {},
): Promise<Response> {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;

  try {
    return await fetch(`${API_BASE_URL}${normalizedPath}`, options);
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

function getDownloadFilename(disposition: string | null): string {
  if (!disposition) {
    return "cleaned.csv";
  }

  // Prefer RFC 5987 encoded filenames, used for UTF-8 downloads.
  const encodedMatch = disposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);

  if (encodedMatch?.[1]) {
    const encodedFilename = encodedMatch[1].trim().replace(/^"|"$/g, "");

    try {
      return decodeURIComponent(encodedFilename);
    } catch {
      return encodedFilename;
    }
  }

  // Fall back to an ordinary filename parameter.
  const plainMatch = disposition.match(
    /filename\s*=\s*"([^"]+)"|filename\s*=\s*([^;]+)/i,
  );

  const filename = plainMatch?.[1] ?? plainMatch?.[2];

  return filename?.trim() || "cleaned.csv";
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

  return {
    blob: await response.blob(),
    filename: getDownloadFilename(disposition),
  };
}
