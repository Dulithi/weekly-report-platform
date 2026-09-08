"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import { formatDateRange, formatDateTime, StatusBadge } from "@/components/reports/report-history";
import type { ManagerReportSummary, Page, TeamMemberProfile } from "@/lib/reports/model";

const PAGE_SIZE = 10;

export function TeamMemberProfileView({ memberId }: { memberId: string }) {
  const { user, request } = useAuth();
  const [profile, setProfile] = useState<TeamMemberProfile | null>(null);
  const [reports, setReports] = useState<Page<ManagerReportSummary> | null>(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const loadGeneration = useRef(0);
  const allowed = user?.role === "MANAGER" || user?.role === "ADMIN";

  const load = useCallback(async () => {
    const generation = ++loadGeneration.current;
    setLoading(true);
    setError("");
    const query = new URLSearchParams({
      memberId,
      page: String(pageNumber),
      size: String(PAGE_SIZE),
      sort: "weekStart,desc",
    });
    try {
      const [loadedProfile, loadedReports] = await Promise.all([
        request<TeamMemberProfile>("/manager/team-members/" + memberId),
        request<Page<ManagerReportSummary>>("/manager/reports?" + query.toString()),
      ]);
      if (generation === loadGeneration.current) {
        setProfile(loadedProfile);
        setReports(loadedReports);
      }
    } catch (caught) {
      if (generation === loadGeneration.current) {
        setError(caught instanceof Error ? caught.message : "Could not load the team member profile.");
      }
    } finally {
      if (generation === loadGeneration.current) setLoading(false);
    }
  }, [memberId, pageNumber, request]);

  useEffect(() => {
    if (allowed) void Promise.resolve().then(load);
    return () => { loadGeneration.current += 1; };
  }, [allowed, load]);

  if (!allowed) return <AccessDenied />;
  if (loading && !profile) return <ProfileSkeleton />;
  if (!profile) return <ErrorCard message={error || "Team member not found."} onRetry={load} />;

  const { member, statistics } = profile;
  const stats = [
    { label: "All reports", value: statistics.totalReports, color: "text-slate-900" },
    { label: "Draft", value: statistics.draftReports, color: "text-slate-700" },
    { label: "Submitted", value: statistics.submittedReports, color: "text-blue-700" },
    { label: "Needs correction", value: statistics.needsCorrectionReports, color: "text-amber-700" },
    { label: "Approved", value: statistics.approvedReports, color: "text-emerald-700" },
  ];

  return <article className="mx-auto max-w-6xl pb-16">
    <Link href="/manager/team-members" className="text-sm font-semibold text-brand hover:text-brand-strong">← Team members</Link>
    <header className="mt-5 flex flex-col gap-5 border-b border-slate-200 pb-7 sm:flex-row sm:items-end sm:justify-between">
      <div className="flex items-center gap-4">
        <span aria-hidden="true" className="grid size-16 shrink-0 place-items-center rounded-2xl bg-brand-soft text-xl font-bold text-brand-strong">{initials(member.firstName, member.lastName)}</span>
        <div><div className="flex flex-wrap items-center gap-3"><h1 className="text-3xl font-semibold tracking-tight">{member.firstName} {member.lastName}</h1><AccountState active={member.active} /></div><p className="mt-2 break-all text-sm text-slate-500">{member.email}</p></div>
      </div>
      <Link href="/manager/reports" className="report-secondary">Team report workspace</Link>
    </header>

    {error && <div role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"><p>{error}</p><button type="button" className="mt-2 font-semibold underline" onClick={() => void load()}>Try again</button></div>}

    <section aria-labelledby="report-summary-title" className="mt-6">
      <div className="mb-3 flex items-end justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">Reporting overview</p><h2 id="report-summary-title" className="mt-1 text-xl font-semibold text-slate-900">Workflow totals</h2></div><p className="text-xs text-slate-400">Current status of each weekly report</p></div>
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">{stats.map(stat => <div key={stat.label} className="rounded-2xl border border-slate-200 bg-white p-5"><dt className="text-xs font-semibold text-slate-400">{stat.label}</dt><dd className={`mt-3 text-3xl font-semibold ${stat.color}`}>{stat.value}</dd></div>)}</dl>
    </section>

    <section aria-labelledby="member-history-title" className="mt-8">
      <div className="mb-4"><p className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">Weekly timeline</p><h2 id="member-history-title" className="mt-1 text-xl font-semibold text-slate-900">Report history</h2><p className="mt-1 text-sm text-slate-500">Draft status is visible for tracking, while draft content stays private until submission.</p></div>

      {loading ? <HistorySkeleton /> : reports?.content.length ? <>
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <ul className="divide-y divide-slate-100">{reports.content.map(report => <ProfileReportRow key={report.id} report={report} />)}</ul>
        </div>
        <nav className="mt-5 flex flex-col items-center justify-between gap-3 sm:flex-row" aria-label="Member report pages">
          <button type="button" className="report-secondary" disabled={reports.first || loading} onClick={() => setPageNumber(value => Math.max(0, value - 1))}>← Previous</button>
          <p className="text-sm text-slate-500">Page {reports.number + 1} of {reports.totalPages} · {reports.totalElements} reports</p>
          <button type="button" className="report-secondary" disabled={reports.last || loading} onClick={() => setPageNumber(value => value + 1)}>Next →</button>
        </nav>
      </> : !error && <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center"><h3 className="text-lg font-semibold text-slate-900">No weekly reports yet</h3><p className="mt-2 text-sm text-slate-500">This member has not started a report.</p></div>}
    </section>
  </article>;
}

function ProfileReportRow({ report }: { report: ManagerReportSummary }) {
  const canOpen = report.status !== "DRAFT";
  const content = <><div><div className="flex flex-wrap items-center gap-3"><h3 className="font-semibold text-slate-900">{formatDateRange(report.weekStart, report.weekEnd)}</h3><StatusBadge status={report.status} /></div><p className="mt-2 text-xs text-slate-400">{report.submittedAt ? `Submitted ${formatDateTime(report.submittedAt)}` : `Updated ${formatDateTime(report.updatedAt)}`}</p></div><span className={`text-sm font-semibold ${canOpen ? "text-brand group-hover:text-brand-strong" : "text-slate-400"}`}>{canOpen ? "Open report →" : "Draft content is private"}</span></>;
  return <li>{canOpen ? <Link href={`/manager/reports/${report.id}`} className="group grid gap-4 p-5 transition hover:bg-slate-50 sm:grid-cols-[1fr_auto] sm:items-center sm:px-6">{content}</Link> : <div className="grid gap-4 p-5 sm:grid-cols-[1fr_auto] sm:items-center sm:px-6">{content}</div>}</li>;
}

function initials(firstName: string, lastName: string) {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}

function AccountState({ active }: { active: boolean }) {
  return <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>{active ? "Active" : "Inactive"}</span>;
}

function ProfileSkeleton() {
  return <div className="mx-auto max-w-6xl space-y-6" aria-label="Loading team member profile"><div className="h-28 animate-pulse rounded-2xl bg-white" /><div className="grid grid-cols-2 gap-3 sm:grid-cols-5">{[0, 1, 2, 3, 4].map(item => <div key={item} className="h-28 animate-pulse rounded-2xl bg-white" />)}</div><div className="h-72 animate-pulse rounded-2xl bg-white" /></div>;
}

function HistorySkeleton() {
  return <div className="space-y-3" aria-label="Loading member reports">{[0, 1, 2].map(item => <div key={item} className="h-24 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}</div>;
}

function ErrorCard({ message, onRetry }: { message: string; onRetry(): Promise<void> }) {
  return <div className="mx-auto max-w-xl rounded-2xl border border-red-200 bg-red-50 p-6"><h1 className="font-semibold text-red-900">Could not open profile</h1><p className="mt-2 text-sm text-red-700">{message}</p><div className="mt-4 flex gap-4"><button type="button" className="font-semibold text-red-800 underline" onClick={() => void onRetry()}>Try again</button><Link href="/manager/team-members" className="font-semibold text-brand">Return to team members</Link></div></div>;
}

function AccessDenied() {
  return <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6"><h1 className="font-semibold text-amber-950">Manager access required</h1><p className="mt-2 text-sm text-amber-800">Team member profiles are available to managers and administrators.</p><Link href="/dashboard" className="mt-4 inline-block font-semibold text-brand">Return to dashboard</Link></div>;
}
