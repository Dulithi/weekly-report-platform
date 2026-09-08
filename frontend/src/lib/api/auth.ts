import { ApiError, apiFetch } from "./client";
import type {
  AccessTokenResponse,
  CsrfProof,
  LoginInput,
  RegisterInput,
  RegisteredUser,
} from "./types";

let csrfProof: CsrfProof | undefined;
let csrfRequest: Promise<CsrfProof> | undefined;
let refreshRequest: Promise<AccessTokenResponse> | undefined;

async function getCsrfProof(): Promise<CsrfProof> {
  if (csrfProof) {
    return csrfProof;
  }

  csrfRequest ??= apiFetch<CsrfProof>("/auth/csrf");
  try {
    csrfProof = await csrfRequest;
    return csrfProof;
  } finally {
    csrfRequest = undefined;
  }
}

async function postAuthentication<T>(
  path: string,
  body?: unknown,
): Promise<T> {
  for (let attempt = 0; attempt < 2; attempt++) {
    const proof = await getCsrfProof();
    try {
      return await apiFetch<T>(path, {
        method: "POST",
        headers: { [proof.headerName]: proof.token },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch (error) {
      // The CSRF cookie can change after a server restart or another tab's
      // request. A 403 means the POST was rejected before its action ran, so it
      // is safe to fetch a fresh proof and retry exactly once.
      if (!(error instanceof ApiError) || error.status !== 403 || attempt === 1) {
        throw error;
      }
      csrfProof = undefined;
    }
  }

  throw new Error("Authentication request could not be completed.");
}

async function withSessionLock<T>(operation: () => Promise<T>): Promise<T> {
  if (typeof navigator !== "undefined" && navigator.locks) {
    return await navigator.locks.request("weekly-report-session-refresh", operation);
  }

  return await operation();
}

async function performRefresh(): Promise<AccessTokenResponse> {
  // Web Locks serializes refresh-cookie rotation between tabs. The module-level
  // promise also combines simultaneous refresh attempts inside this tab.
  return withSessionLock(() =>
    postAuthentication<AccessTokenResponse>("/auth/refresh"),
  );
}

export function refreshCredentials(): Promise<AccessTokenResponse> {
  refreshRequest ??= performRefresh().finally(() => {
    refreshRequest = undefined;
  });
  return refreshRequest;
}

export function loginRequest(input: LoginInput): Promise<AccessTokenResponse> {
  return postAuthentication<AccessTokenResponse>("/auth/login", input);
}

export function registerRequest(input: RegisterInput): Promise<RegisteredUser> {
  return postAuthentication<RegisteredUser>("/auth/register", input);
}

export function acceptInvitationRequest(input: {
  acceptanceToken: string;
  password: string;
  firstName: string;
  lastName: string;
}): Promise<RegisteredUser> {
  return postAuthentication<RegisteredUser>("/auth/invitation-acceptances", input);
}

export function logoutRequest(): Promise<void> {
  // Waiting on the same lock as refresh ensures logout sees and revokes the
  // newest rotated cookie instead of racing an in-flight refresh.
  return withSessionLock(() => postAuthentication<void>("/auth/logout"));
}
