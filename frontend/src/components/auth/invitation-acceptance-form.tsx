"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";

import { acceptInvitationRequest } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "./auth-provider";

export function InvitationAcceptanceForm() {
  const router = useRouter();
  const { login, status } = useAuth();
  const [acceptanceToken, setAcceptanceToken] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    const readFragment = () => {
      const fragment = new URLSearchParams(window.location.hash.slice(1));
      setAcceptanceToken(fragment.get("token"));
      setReady(true);
    };
    window.addEventListener("hashchange", readFragment);
    void Promise.resolve().then(readFragment);
    return () => window.removeEventListener("hashchange", readFragment);
  }, []);

  useEffect(() => {
    if (status === "authenticated") router.replace("/dashboard");
  }, [router, status]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    if (!acceptanceToken) {
      setError("This invitation link is incomplete. Ask an administrator for a new link.");
      return;
    }

    const form = new FormData(event.currentTarget);
    const password = String(form.get("password") ?? "");
    if (password !== String(form.get("confirmPassword") ?? "")) {
      setFieldErrors({ confirmPassword: "Passwords do not match" });
      return;
    }

    setPending(true);
    try {
      const account = await acceptInvitationRequest({
        acceptanceToken,
        firstName: String(form.get("firstName") ?? "").trim(),
        lastName: String(form.get("lastName") ?? "").trim(),
        password,
      });
      window.history.replaceState(null, "", window.location.pathname);
      await login({ email: account.email, password });
      router.replace("/dashboard");
    } catch (caught) {
      if (caught instanceof ApiError) {
        setFieldErrors(caught.fieldErrors);
        setError(Object.keys(caught.fieldErrors).length ? null : caught.message);
      } else {
        setError(caught instanceof Error ? caught.message : "The invitation could not be accepted.");
      }
    } finally {
      setPending(false);
    }
  }

  const inputClass = "h-12 w-full rounded-xl border border-slate-200 bg-white px-4 text-slate-950 shadow-sm outline-none transition focus:border-brand focus:ring-4 focus:ring-brand/10";

  return <>
    <div className="mb-8">
      <p className="mb-3 text-sm font-semibold text-brand">Team invitation</p>
      <h1 className="text-4xl font-semibold tracking-[-0.045em] text-slate-950">Set up your account</h1>
      <p className="mt-4 leading-7 text-slate-500">Your role and email were chosen by your administrator. Add your name and a strong password to join.</p>
    </div>

    {ready && !acceptanceToken ? <div role="alert" className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-800">This invitation link has no acceptance token. Ask an administrator to create a new invitation.</div> :
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="First name" name="firstName" error={fieldErrors.firstName}><input id="firstName" name="firstName" autoComplete="given-name" required maxLength={100} className={inputClass} /></Field>
          <Field label="Last name" name="lastName" error={fieldErrors.lastName}><input id="lastName" name="lastName" autoComplete="family-name" required maxLength={100} className={inputClass} /></Field>
        </div>
        <Field label="Password" name="password" error={fieldErrors.password} hint="Use 12–128 characters."><input id="password" name="password" type="password" autoComplete="new-password" required minLength={12} maxLength={128} className={inputClass} /></Field>
        <Field label="Confirm password" name="confirmPassword" error={fieldErrors.confirmPassword}><input id="confirmPassword" name="confirmPassword" type="password" autoComplete="new-password" required minLength={12} maxLength={128} className={inputClass} /></Field>
        {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-700">{error}</div>}
        <button type="submit" disabled={!ready || pending || status === "loading" || status === "authenticated"} className="flex h-12 w-full items-center justify-center rounded-xl bg-brand px-5 font-semibold text-white transition hover:bg-brand-strong disabled:cursor-not-allowed disabled:opacity-55">{pending ? "Creating your account…" : "Accept invitation"}</button>
      </form>}

    <p className="mt-7 text-center text-sm text-slate-500">Already accepted? <Link href="/login" className="font-semibold text-brand hover:underline">Sign in</Link></p>
  </>;
}

function Field({ label, name, error, hint, children }: { label: string; name: string; error?: string; hint?: string; children: React.ReactNode }) {
  return <div><label htmlFor={name} className="mb-2 block text-sm font-medium text-slate-700">{label}</label>{children}{(error || hint) && <p className={`mt-1.5 text-xs ${error ? "text-red-600" : "text-slate-400"}`}>{error ?? hint}</p>}</div>;
}
