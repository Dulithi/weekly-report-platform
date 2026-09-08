import type { ProblemDetails } from "./types";

const API_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1"
).replace(/\/$/, "");

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly title?: string,
    readonly fieldErrors: Record<string, string> = {},
    readonly retryAfterSeconds?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

function apiUrl(path: string): string {
  return `${API_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

async function readResponseBody(response: Response): Promise<unknown> {
  if (response.status === 204) {
    return undefined;
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("json")) {
    return response.json();
  }

  const text = await response.text();
  return text || undefined;
}

function toApiError(response: Response, body: unknown): ApiError {
  const problem =
    body && typeof body === "object" ? (body as ProblemDetails) : undefined;
  const fallback =
    response.status === 401
      ? "Your session is not valid. Please sign in again."
      : "The request could not be completed.";
  const retryAfter = response.headers.get("retry-after");

  return new ApiError(
    problem?.detail ?? (typeof body === "string" ? body : fallback),
    response.status,
    problem?.title,
    problem?.errors,
    retryAfter ? Number.parseInt(retryAfter, 10) : undefined,
  );
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
  accessToken?: string,
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response: Response;
  try {
    response = await fetch(apiUrl(path), {
      ...init,
      headers,
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw new ApiError(
      "The API could not be reached. Check that the backend is running and try again.",
      0,
      "Connection failed",
    );
  }

  const body = await readResponseBody(response);
  if (!response.ok) {
    throw toApiError(response, body);
  }

  return body as T;
}
