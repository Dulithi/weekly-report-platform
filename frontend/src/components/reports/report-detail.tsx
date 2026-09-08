"use client";

import Link from "next/link";
import { useEffect, useState, type ReactNode } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import { label, type Review, type WeeklyReport } from "@/lib/reports/model";
import { formatDateRange, formatDateTime, StatusBadge } from "./report-history";

export function ReportDetail({ reportId }: { reportId: string }) {
  const { user, request } = useAuth();
  const [report, setReport] = useState<WeeklyReport | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    void Promise.all([
      request<WeeklyReport>(`/reports/${reportId}`),
      request<Review[]>(`/reports/${reportId}/reviews`),
    ]).then(([loadedReport, loadedReviews]) => {
      if (!active) return;
      setReport(loadedReport);
      setReviews([...loadedReviews].sort((left, right) => right.createdAt.localeCompare(left.createdAt)));
    }).catch(caught => {
      if (active) setError(caught instanceof Error ? caught.message : "Could not load this report.");
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [reportId, request]);

  if (user?.role !== "TEAM_MEMBER") return <p>Personal reports are available to team members.</p>;
  if (loading) return <DetailSkeleton />;
  if (!report) return <ErrorCard message={error || "Report not found."} />;

  const version = report.currentVersion;
  const editable = report.status === "DRAFT" || report.status === "NEEDS_CORRECTION";
  const totalMinutes = version.timeEntries.reduce((sum, item) => sum + item.minutes, 0);

  return <article className="mx-auto max-w-5xl pb-16">
    <Link href="/reports" className="text-sm font-semibold text-brand hover:text-brand-strong">← Report history</Link>
    <header className="mt-5 flex flex-col gap-5 border-b border-slate-200 pb-7 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <div className="flex flex-wrap items-center gap-3"><p className="text-sm font-semibold text-brand">{formatDateRange(report.weekStart, report.weekEnd)}</p><StatusBadge status={report.status} /></div>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Weekly report</h1>
        <p className="mt-2 text-sm text-slate-500">Version {version.versionNumber} · Updated {formatDateTime(version.createdAt)}</p>
      </div>
      {editable && <Link href={`/reports/${report.id}/edit`} className="report-primary">Continue editing</Link>}
    </header>

    <div className="mt-6 grid gap-4 sm:grid-cols-3">
      <Meta label="Current status" value={label(report.status)} />
      <Meta label="Submitted" value={report.submittedAt ? formatDateTime(report.submittedAt) : "Not submitted"} />
      <Meta label="Approved" value={report.approvedAt ? formatDateTime(report.approvedAt) : "Not approved"} />
    </div>

    <div className="mt-6 space-y-6">
      <ReadSection number="01" title="Tasks completed" empty={!version.completedTasks.length}>
        {version.completedTasks.map((task, index) => <div key={`${task.taskName}-${index}`} className="rounded-xl border border-slate-100 bg-slate-50/60 p-4">
          <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-semibold text-slate-900">{task.taskName}</h3><p className="mt-1 text-sm text-slate-500">{task.projectName || "No project"}</p></div><span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600">{label(task.status)}</span></div>
          {task.description && <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{task.description}</p>}
          <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4"><Fact term="Priority" value={label(task.priority)} /><Fact term="Progress" value={`${task.actualPercentage}% actual / ${task.plannedPercentage}% planned`} /><Fact term="Time planned" value={formatMinutes(task.plannedMinutes)} /><Fact term="Time spent" value={formatMinutes(task.spentMinutes)} /></dl>
          {task.deliverable && <div className="mt-4 border-t border-slate-200 pt-3 text-sm"><span className="font-medium text-slate-500">Output: </span><SafeDeliverable value={task.deliverable} /></div>}
        </div>)}
      </ReadSection>

      <ReadSection number="02" title="Plans for next week" empty={!version.plannedTasks.length}>
        {version.plannedTasks.map((task, index) => <div key={`${task.taskName}-${index}`} className="rounded-xl border border-slate-100 bg-slate-50/60 p-4">
          <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-semibold text-slate-900">{task.taskName}</h3><p className="mt-1 text-sm text-slate-500">{task.projectName || "No project"}</p></div><span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600">{label(task.priority)}</span></div>
          {task.description && <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{task.description}</p>}
          <p className="mt-3 text-sm text-slate-500">Estimated time: <span className="font-medium text-slate-700">{formatMinutes(task.estimatedMinutes)}</span></p>
        </div>)}
      </ReadSection>

      <ReadSection number="03" title="Blockers and challenges" empty={!version.blockers.length}>
        {version.blockers.map((item, index) => <div key={index} className="rounded-xl border border-slate-100 bg-slate-50/60 p-4"><div className="flex flex-wrap gap-2">{item.keyBlocker && <Tag>Key blocker</Tag>}<Tag>{item.resolved ? "Resolved" : "Open"}</Tag></div><p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{item.description}</p></div>)}
      </ReadSection>

      <ReadSection number="04" title="Achievements and highlights" empty={!version.achievements.length}>
        {version.achievements.map((item, index) => <div key={index} className="rounded-xl border border-slate-100 bg-slate-50/60 p-4">{item.keyAchievement && <Tag>Key achievement</Tag>}<p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{item.description}</p></div>)}
      </ReadSection>

      <ReadSection number="05" title="Time by task type" empty={!version.timeEntries.length}>
        {!!version.timeEntries.length && <><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{version.timeEntries.map(item => <div key={item.taskType} className="rounded-xl bg-slate-50 p-4"><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label(item.taskType)}</p><p className="mt-2 text-lg font-semibold text-slate-900">{formatMinutes(item.minutes)}</p></div>)}</div><p className="text-right text-sm font-semibold text-slate-700">Total: {formatMinutes(totalMinutes)}</p></>}
      </ReadSection>

      <ReadSection number="06" title="Notes or links" empty={!version.notes?.trim()}>
        {version.notes?.trim() && <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">{version.notes}</p>}
      </ReadSection>
    </div>

    <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
      <h2 className="font-semibold text-slate-900">Review history</h2>
      {reviews.length ? <ol className="mt-4 space-y-4">{reviews.map(review => <li key={review.id} className="border-l-2 border-slate-200 pl-4"><div className="flex flex-wrap items-center gap-2"><Tag>{review.action === "APPROVE" ? "Approved" : "Changes requested"}</Tag><span className="text-xs text-slate-400">Version {review.reportVersionNumber} · {formatDateTime(review.createdAt)}</span></div><p className="mt-2 text-sm font-medium text-slate-700">{review.reviewer.firstName} {review.reviewer.lastName}</p>{review.comment && <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-slate-600">{review.comment}</p>}</li>)}</ol> : <p className="mt-3 text-sm text-slate-500">No manager reviews yet.</p>}
    </section>
  </article>;
}

function ReadSection({ number, title, empty, children }: { number: string; title: string; empty: boolean; children: ReactNode }) {
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6"><div className="mb-5 flex gap-4"><span className="text-xs font-bold text-brand">{number}</span><h2 className="font-semibold text-slate-900">{title}</h2></div><div className="space-y-4">{empty ? <p className="text-sm text-slate-400">Nothing recorded.</p> : children}</div></section>;
}
function Meta({ label: name, value }: { label: string; value: string }) { return <div className="rounded-xl border border-slate-200 bg-white p-4"><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{name}</p><p className="mt-2 text-sm font-semibold text-slate-800">{value}</p></div>; }
function Fact({ term, value }: { term: string; value: string }) { return <div><dt className="text-xs font-medium text-slate-400">{term}</dt><dd className="mt-1 text-slate-700">{value}</dd></div>; }
function Tag({ children }: { children: ReactNode }) { return <span className="inline-block rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600 ring-1 ring-slate-200">{children}</span>; }
function formatMinutes(minutes: number | null) { if (minutes === null) return "Not recorded"; return `${Math.floor(minutes / 60)}h ${minutes % 60}m`; }
function SafeDeliverable({ value }: { value: string }) {
  const safeUrl = parseHttpUrl(value);
  return safeUrl
    ? <a href={safeUrl} target="_blank" rel="noopener noreferrer" className="font-medium text-brand hover:underline">{value}</a>
    : <span className="whitespace-pre-wrap text-slate-700">{value}</span>;
}
function parseHttpUrl(value: string): string | null {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
  } catch {
    return null;
  }
}
function DetailSkeleton() { return <div className="mx-auto max-w-5xl space-y-5" aria-label="Loading report"><div className="h-24 animate-pulse rounded-2xl bg-white" />{[0, 1, 2].map(item => <div key={item} className="h-48 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}</div>; }
function ErrorCard({ message }: { message: string }) { return <div className="mx-auto max-w-xl rounded-2xl border border-red-200 bg-red-50 p-6"><h1 className="font-semibold text-red-900">Could not open report</h1><p className="mt-2 text-sm text-red-700">{message}</p><Link href="/reports" className="mt-4 inline-block font-semibold text-brand">Return to report history</Link></div>; }
