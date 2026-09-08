"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";

import { ApiError } from "@/lib/api/client";
import { useAuth } from "./auth-provider";

export function LoginForm() {
  const router = useRouter();
  const { login, status } = useAuth();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/dashboard");
    }
  }, [router, status]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError(null);

    const form = new FormData(event.currentTarget);
    try {
      await login({
        email: String(form.get("email") ?? "").trim(),
        password: String(form.get("password") ?? ""),
      });
      router.replace("/dashboard");
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 429 && caught.retryAfterSeconds) {
        setError(`Too many attempts. Try again in ${caught.retryAfterSeconds} seconds.`);
      } else {
        setError(caught instanceof Error ? caught.message : "Sign in failed. Please try again.");
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <div className="mb-9">
        <p className="mb-3 text-sm font-semibold text-brand">Welcome back</p>
        <h2 className="text-4xl font-semibold tracking-[-0.045em] text-slate-950">Sign in to your workspace</h2>
        <p className="mt-4 leading-7 text-slate-500">Use the email and password linked to your team.</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="email" className="mb-2 block text-sm font-medium text-slate-700">Email address</label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            required
            maxLength={320}
            placeholder="you@company.com"
            className="h-12 w-full rounded-xl border border-slate-200 bg-white px-4 text-slate-950 shadow-sm outline-none transition placeholder:text-slate-350 focus:border-brand focus:ring-4 focus:ring-brand/10"
          />
        </div>
        <div>
          <label htmlFor="password" className="mb-2 block text-sm font-medium text-slate-700">Password</label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            maxLength={128}
            className="h-12 w-full rounded-xl border border-slate-200 bg-white px-4 text-slate-950 shadow-sm outline-none transition focus:border-brand focus:ring-4 focus:ring-brand/10"
          />
        </div>

        {error && (
          <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-700">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={pending || status === "loading" || status === "authenticated"}
          className="flex h-12 w-full items-center justify-center rounded-xl bg-brand px-5 font-semibold text-white shadow-sm transition hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-not-allowed disabled:opacity-55"
        >
          {pending || status === "loading" ? "Checking your account…" : "Sign in"}
        </button>
      </form>

      <p className="mt-8 text-center text-sm text-slate-500">
        New to Weekly?{" "}
        <Link href="/register" className="font-semibold text-brand hover:text-brand-strong hover:underline">
          Create an account
        </Link>
      </p>
    </>
  );
}
