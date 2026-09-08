"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { BrandMark } from "@/components/brand-mark";
import { useAuth } from "./auth-provider";

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { status, sessionError, retrySession } = useAuth();

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace("/login");
    }
  }, [router, status]);

  if (status === "authenticated") {
    return children;
  }

  if (status === "error") {
    return (
      <main className="grid min-h-screen place-items-center bg-stone-50 px-5">
        <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
          <div className="mb-7 flex justify-center"><BrandMark /></div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-950">We could not restore your session</h1>
          <p className="mt-3 leading-7 text-slate-500">{sessionError}</p>
          <div className="mt-7 flex flex-col gap-3 sm:flex-row sm:justify-center">
            <button onClick={() => void retrySession()} className="rounded-xl bg-brand px-5 py-3 font-semibold text-white hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand">
              Try again
            </button>
            <Link href="/login" className="rounded-xl border border-slate-200 px-5 py-3 font-semibold text-slate-700 hover:bg-slate-50">
              Go to sign in
            </Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="grid min-h-screen place-items-center bg-stone-50" aria-live="polite">
      <div className="flex flex-col items-center gap-5">
        <span className="grid size-12 place-items-center rounded-2xl bg-brand text-white shadow-sm" aria-hidden="true">
          <svg viewBox="0 0 24 24" className="size-6 animate-pulse" fill="none">
            <path d="M6 7.5h12M6 12h7M6 16.5h5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </span>
        <p className="text-sm font-medium text-slate-500">Opening your workspace…</p>
      </div>
    </main>
  );
}
