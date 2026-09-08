"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import { label, type Page, type ReportStatus, type WeeklyReportSummary } from "@/lib/reports/model";

const PAGE_SIZE = 10;

export function ReportHistory() {
  const { user, request } = useAuth();
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<WeeklyReportSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const loadGeneration = useRef(0);

  const load = useCallback(async () => {
    const generation = ++loadGeneration.current;
    setLoading(true);
    setError("");
    try {
      const loadedPage = await request<Page<WeeklyReportSummary>>(
        `/reports/me?page=${pageNumber}&size=${PAGE_SIZE}&sort=weekStart,desc`,
      );
      if (generation === loadGeneration.current) setPage(loadedPage);
    } catch (caught) {
      if (generation === loadGeneration.current) {
        setError(caught instanceof Error ? caught.message : "Could not load report history.");
      }
    } finally {
      if (generation === loadGeneration.current) setLoading(false);
    }
  }, [pageNumber, request]);

  useEffect(() => {
    void Promise.resolve().then(load);
    return () => { loadGeneration.current += 1; };
  }, [load]);

  if (user?.role !== "TEAM_MEMBER") {
    return <p>Personal reports are available to team members.</p>;
  }

  return <section className="mx-auto max-w-5xl">
    <header className="mb-7 flex flex-col gap-4 border-b border-slate-200 pb-7 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p className="text-sm font-semibold text-brand">Personal reporting</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Report history</h1>
        <p className="mt-2 text-slate-500">Open a draft, respond to requested changes, or review a submitted week.</p>
      </div>
      <Link href="/reports/new" className="report-primary">Open a week</Link>
    </header>

    {error && <div role="alert" className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
      <p>{error}</p>
      <button type="button" onClick={() => void load()} className="mt-2 font-semibold underline">Try again</button>
    </div>}

    {loading ? <HistorySkeleton /> : page?.content.length ? <>
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <ul className="divide-y divide-slate-100">
          {page.content.map(report => {
            const editable = report.status === "DRAFT" || report.status === "NEEDS_CORRECTION";
            return <li key={report.id}>
              <Link href={editable ? `/reports/${report.id}/edit` : `/reports/${report.id}`} className="group grid gap-3 p-5 transition hover:bg-slate-50 sm:grid-cols-[1fr_auto] sm:items-center sm:px-6">
                <div>
                  <div className="flex flex-wrap items-center gap-3">
                    <h2 className="font-semibold text-slate-900">{formatDateRange(report.weekStart, report.weekEnd)}</h2>
                    <StatusBadge status={report.status} />
                  </div>
                  <p className="mt-2 text-sm text-slate-500">Version {report.currentVersionNumber} · {report.submittedAt ? `Submitted ${formatDateTime(report.submittedAt)}` : `Updated ${formatDateTime(report.updatedAt)}`}</p>
                </div>
                <span className="text-sm font-semibold text-brand group-hover:text-brand-strong">{editable ? "Continue report" : "View report"} →</span>
              </Link>
            </li>;
          })}
        </ul>
      </div>

      <nav className="mt-5 flex items-center justify-between" aria-label="Report history pages">
        <button type="button" className="report-secondary" disabled={page.first} onClick={() => setPageNumber(value => Math.max(0, value - 1))}>← Previous</button>
        <p className="text-sm text-slate-500">Page {page.number + 1} of {page.totalPages} · {page.totalElements} reports</p>
        <button type="button" className="report-secondary" disabled={page.last} onClick={() => setPageNumber(value => value + 1)}>Next →</button>
      </nav>
    </> : !error && <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
      <h2 className="text-xl font-semibold text-slate-900">No weekly reports yet</h2>
      <p className="mt-2 text-slate-500">Choose a Monday to start your first private draft.</p>
      <Link href="/reports/new" className="report-primary mt-6">Create your first report</Link>
    </div>}
  </section>;
}

export function StatusBadge({ status }: { status: ReportStatus }) {
  const colors: Record<ReportStatus, string> = {
    DRAFT: "bg-slate-100 text-slate-700",
    SUBMITTED: "bg-blue-50 text-blue-700",
    NEEDS_CORRECTION: "bg-amber-50 text-amber-800",
    APPROVED: "bg-emerald-50 text-emerald-700",
  };
  return <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${colors[status]}`}>{label(status)}</span>;
}

export function formatDateRange(start: string, end: string) {
  return `${formatDate(start)} – ${formatDate(end)}`;
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { year: "numeric", month: "short", day: "numeric", timeZone: "UTC" })
    .format(new Date(`${value}T12:00:00Z`));
}

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function HistorySkeleton() {
  return <div className="space-y-3" aria-label="Loading report history">
    {[0, 1, 2].map(item => <div key={item} className="h-24 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}
  </div>;
}
