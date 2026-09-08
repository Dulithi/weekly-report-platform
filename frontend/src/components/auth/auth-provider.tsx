"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { ApiError, apiFetch } from "@/lib/api/client";
import {
  loginRequest,
  logoutRequest,
  refreshCredentials,
  registerRequest,
} from "@/lib/api/auth";
import type {
  AccessTokenResponse,
  CurrentUser,
  LoginInput,
  RegisterInput,
} from "@/lib/api/types";

type AuthStatus = "loading" | "authenticated" | "unauthenticated" | "error";

interface AuthContextValue {
  status: AuthStatus;
  user: CurrentUser | null;
  sessionError: string | null;
  login(input: LoginInput): Promise<void>;
  register(input: RegisterInput): Promise<void>;
  logout(): Promise<void>;
  retrySession(): Promise<void>;
  request<T>(path: string, init?: RequestInit): Promise<T>;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const AUTH_CHANNEL = "weekly-report-auth";
const AUTH_STORAGE_EVENT = "weekly-report-auth-event";
const SIGNED_OUT_KEY = "weekly-report-explicitly-signed-out";
const AUTH_SOURCE = globalThis.crypto?.randomUUID?.() ?? Math.random().toString(36);

class SessionSupersededError extends Error {
  constructor() {
    super("A newer authentication action replaced this one.");
    this.name = "SessionSupersededError";
  }
}

function broadcastSessionChange(type: "login" | "logout") {
  if (typeof window === "undefined") return;

  if (typeof BroadcastChannel !== "undefined") {
    const channel = new BroadcastChannel(AUTH_CHANNEL);
    channel.postMessage({ source: AUTH_SOURCE, type });
    channel.close();
    return;
  }

  // The value contains no account or token data. It exists only to trigger the
  // storage event in older browsers that do not implement BroadcastChannel.
  localStorage.setItem(AUTH_STORAGE_EVENT, `${AUTH_SOURCE}:${type}:${Date.now()}`);
  localStorage.removeItem(AUTH_STORAGE_EVENT);
}

async function fetchCurrentUser(accessToken: string): Promise<CurrentUser> {
  return apiFetch<CurrentUser>("/users/me", {}, accessToken);
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [sessionError, setSessionError] = useState<string | null>(null);
  const accessToken = useRef<string | null>(null);
  const expiresAt = useRef<number>(0);
  const sessionGeneration = useRef(0);
  const sessionRefresh = useRef<{
    generation: number;
    promise: Promise<string>;
  } | null>(null);

  const clearSession = useCallback(() => {
    accessToken.current = null;
    expiresAt.current = 0;
    setUser(null);
    setSessionError(null);
    setStatus("unauthenticated");
  }, []);

  const invalidateSession = useCallback(() => {
    sessionGeneration.current += 1;
    clearSession();
  }, [clearSession]);

  const acceptCredentials = useCallback(async (
    credentials: AccessTokenResponse,
    generation: number,
  ) => {
    if (generation !== sessionGeneration.current) {
      throw new SessionSupersededError();
    }
    accessToken.current = credentials.accessToken;
    expiresAt.current = Date.parse(credentials.expiresAt);
    const currentUser = await fetchCurrentUser(credentials.accessToken);
    if (generation !== sessionGeneration.current) {
      throw new SessionSupersededError();
    }
    setUser(currentUser);
    setSessionError(null);
    setStatus("authenticated");
    return credentials.accessToken;
  }, []);

  const refreshSession = useCallback((): Promise<string> => {
    const generation = sessionGeneration.current;
    if (sessionRefresh.current?.generation === generation) {
      return sessionRefresh.current.promise;
    }

    const pending = refreshCredentials()
      .then(credentials => acceptCredentials(credentials, generation))
      .catch((error: unknown) => {
        if (
          generation === sessionGeneration.current &&
          error instanceof ApiError &&
          error.status === 401
        ) {
          clearSession();
        }
        throw error;
      })
      .finally(() => {
        if (sessionRefresh.current?.promise === pending) {
          sessionRefresh.current = null;
        }
      });

    sessionRefresh.current = { generation, promise: pending };
    return pending;
  }, [acceptCredentials, clearSession]);

  const restoreSession = useCallback(async () => {
    // An explicit local sign-out remains authoritative across reloads, even if
    // the network failed before the server could clear its HttpOnly cookie.
    if (localStorage.getItem(SIGNED_OUT_KEY) === "true") {
      clearSession();
      return;
    }
    const generation = sessionGeneration.current;
    try {
      await refreshSession();
    } catch (error) {
      if (
        generation !== sessionGeneration.current ||
        error instanceof SessionSupersededError
      ) {
        return;
      }
      if (error instanceof ApiError && error.status === 401) {
        return;
      }
      setStatus("error");
      setSessionError(
        error instanceof Error ? error.message : "The session could not be restored.",
      );
    }
  }, [clearSession, refreshSession]);

  useEffect(() => {
    // Start after the effect setup completes. This keeps state changes in the
    // asynchronous result path and avoids a redundant render during hydration.
    void Promise.resolve().then(restoreSession);
  }, [restoreSession]);

  useEffect(() => {
    const handleBroadcast = (event: MessageEvent<unknown>) => {
      const message = event.data as { source?: unknown } | null;
      if (message?.source !== AUTH_SOURCE) invalidateSession();
    };
    const handleStorage = (event: StorageEvent) => {
      if (
        event.key === AUTH_STORAGE_EVENT &&
        event.newValue &&
        !event.newValue.startsWith(`${AUTH_SOURCE}:`)
      ) {
        invalidateSession();
      }
    };
    const channel = typeof BroadcastChannel === "undefined"
      ? null
      : new BroadcastChannel(AUTH_CHANNEL);

    channel?.addEventListener("message", handleBroadcast);
    window.addEventListener("storage", handleStorage);
    return () => {
      channel?.removeEventListener("message", handleBroadcast);
      channel?.close();
      window.removeEventListener("storage", handleStorage);
    };
  }, [invalidateSession]);

  const retrySession = useCallback(async () => {
    setStatus("loading");
    setSessionError(null);
    await restoreSession();
  }, [restoreSession]);

  const login = useCallback(
    async (input: LoginInput) => {
      setSessionError(null);
      const generation = sessionGeneration.current + 1;
      sessionGeneration.current = generation;
      accessToken.current = null;
      expiresAt.current = 0;
      setUser(null);
      setStatus("loading");
      try {
        const credentials = await loginRequest(input);
        await acceptCredentials(credentials, generation);
        localStorage.removeItem(SIGNED_OUT_KEY);
        broadcastSessionChange("login");
      } catch (error) {
        if (generation === sessionGeneration.current) {
          clearSession();
        }
        throw error;
      }
    },
    [acceptCredentials, clearSession],
  );

  const register = useCallback(
    async (input: RegisterInput) => {
      setSessionError(null);
      await registerRequest(input);
      await login({ email: input.email, password: input.password });
    },
    [login],
  );

  const logout = useCallback(async () => {
    // Invalidate local credentials before any network wait. The generation
    // prevents a restore/refresh that started earlier from signing the user
    // back in after its response arrives.
    localStorage.setItem(SIGNED_OUT_KEY, "true");
    invalidateSession();
    broadcastSessionChange("logout");
    try {
      await logoutRequest();
    } catch (error) {
      // Local sign-out is complete, but callers still need to know that the
      // server could not revoke the refresh-token family.
      throw error;
    }
  }, [invalidateSession]);

  const request = useCallback(
    async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
      let token = accessToken.current;
      if (!token) {
        throw new ApiError("Please sign in to continue.", 401, "Authentication required");
      }

      // Refresh shortly before expiry so a request is not sent with a token that
      // will expire while it is being processed.
      if (expiresAt.current <= Date.now() + 30_000) {
        token = await refreshSession();
      }

      try {
        return await apiFetch<T>(path, init, token);
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 401) {
          throw error;
        }

        const refreshedToken = await refreshSession();
        return apiFetch<T>(path, init, refreshedToken);
      }
    },
    [refreshSession],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      sessionError,
      login,
      register,
      logout,
      retrySession,
      request,
    }),
    [status, user, sessionError, login, register, logout, retrySession, request],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
