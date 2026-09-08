"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";

import { ApiError } from "@/lib/api/client";
import { useAuth } from "./auth-provider";

export function RegisterForm() {
  const router = useRouter();
  const { register, status } = useAuth();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/dashboard");
    }
  }, [router, status]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setFieldErrors({});

    const form = new FormData(event.currentTarget);
    const password = String(form.get("password") ?? "");
    if (password !== String(form.get("confirmPassword") ?? "")) {
      setFieldErrors({ confirmPassword: "Passwords do not match" });
      return;
    }

    setPending(true);
    try {
      await register({
        firstName: String(form.get("firstName") ?? "").trim(),
        lastName: String(form.get("lastName") ?? "").trim(),
        email: String(form.get("email") ?? "").trim(),
        password,
      });
      router.replace("/dashboard");
    } catch (caught) {
      if (caught instanceof ApiError) {
        setFieldErrors(caught.fieldErrors);
        setError(Object.keys(caught.fieldErrors).length ? null : caught.message);
      } else {
        setError(caught instanceof Error ? caught.message : "Account creation failed. Please try again.");
      }
    } finally {
      setPending(false);
    }
  }

  const inputClass = "h-12 w-full rounded-xl border border-slate-200 bg-white px-4 text-slate-950 shadow-sm outline-none transition placeholder:text-slate-350 focus:border-brand focus:ring-4 focus:ring-brand/10";

  return (
    <>
      <div className="mb-8">
        <p className="mb-3 text-sm font-semibold text-brand">Get started</p>
        <h2 className="text-4xl font-semibold tracking-[-0.045em] text-slate-950">Create your account</h2>
        <p className="mt-4 leading-7 text-slate-500">Join your team and make each week easier to understand.</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="First name" name="firstName" error={fieldErrors.firstName}>
            <input id="firstName" name="firstName" autoComplete="given-name" required maxLength={100} aria-invalid={Boolean(fieldErrors.firstName)} aria-describedby={fieldErrors.firstName ? "firstName-description" : undefined} className={inputClass} />
          </Field>
          <Field label="Last name" name="lastName" error={fieldErrors.lastName}>
            <input id="lastName" name="lastName" autoComplete="family-name" required maxLength={100} aria-invalid={Boolean(fieldErrors.lastName)} aria-describedby={fieldErrors.lastName ? "lastName-description" : undefined} className={inputClass} />
          </Field>
        </div>
        <Field label="Email address" name="email" error={fieldErrors.email}>
          <input id="email" name="email" type="email" autoComplete="email" required maxLength={320} placeholder="you@company.com" aria-invalid={Boolean(fieldErrors.email)} aria-describedby={fieldErrors.email ? "email-description" : undefined} className={inputClass} />
        </Field>
        <Field label="Password" name="password" error={fieldErrors.password} hint="Use 12–128 characters.">
          <input id="password" name="password" type="password" autoComplete="new-password" required minLength={12} maxLength={128} aria-invalid={Boolean(fieldErrors.password)} aria-describedby="password-description" className={inputClass} />
        </Field>
        <Field label="Confirm password" name="confirmPassword" error={fieldErrors.confirmPassword}>
          <input id="confirmPassword" name="confirmPassword" type="password" autoComplete="new-password" required minLength={12} maxLength={128} aria-invalid={Boolean(fieldErrors.confirmPassword)} aria-describedby={fieldErrors.confirmPassword ? "confirmPassword-description" : undefined} className={inputClass} />
        </Field>

        {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-700">{error}</div>}

        <button
          type="submit"
          disabled={pending || status === "loading" || status === "authenticated"}
          className="mt-2 flex h-12 w-full items-center justify-center rounded-xl bg-brand px-5 font-semibold text-white shadow-sm transition hover:bg-brand-strong focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand disabled:cursor-not-allowed disabled:opacity-55"
        >
          {pending ? "Creating your account…" : "Create account"}
        </button>
      </form>

      <p className="mt-7 text-center text-sm text-slate-500">
        Already have an account?{" "}
        <Link href="/login" className="font-semibold text-brand hover:text-brand-strong hover:underline">Sign in</Link>
      </p>
    </>
  );
}

function Field({
  label,
  name,
  error,
  hint,
  children,
}: {
  label: string;
  name: string;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}) {
  const descriptionId = error || hint ? `${name}-description` : undefined;
  return (
    <div>
      <label htmlFor={name} className="mb-2 block text-sm font-medium text-slate-700">{label}</label>
      {children}
      {(error || hint) && (
        <p id={descriptionId} className={`mt-1.5 text-xs ${error ? "text-red-600" : "text-slate-400"}`}>
          {error ?? hint}
        </p>
      )}
    </div>
  );
}
