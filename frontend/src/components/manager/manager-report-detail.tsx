"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type ReactNode } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import { ApiError } from "@/lib/api/client";
import {
  label,
  type ManagerReportDetail as ManagerReportDetailData,
  type ReportVersion,
  type ReportVersionSummary,
  type Review,
} from "@/lib/reports/model";
import { formatDateRange, formatDateTime, StatusBadge } from "@/components/reports/report-history";

type ReviewAction = "APPROVED" | "CHANGES_REQUESTED";

export function ManagerReportDetail({ reportId }: { reportId: string }) {
  const { user, request } = useAuth();
  const [report, setReport] = useState<ManagerReportDetailData | null>(null);
  const [versions, setVersions] = useState<ReportVersionSummary[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [selectedVersion, setSelectedVersion] = useState<ReportVersion | null>(null);
  const [loading, setLoading] = useState(true);
  const [versionLoading, setVersionLoading] = useState(false);
  const [error, setError] = useState("");
  const [reviewError, setReviewError] = useState("");
  const [comment, setComment] = useState("");
  const [submittingAction, setSubmittingAction] = useState<ReviewAction | null>(null);
  const allowed = user?.role === "MANAGER" || user?.role === "ADMIN";

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [loadedReport, loadedVersions, loadedReviews] = await Promise.all([
        request<ManagerReportDetailData>(`/manager/reports/${reportId}`),
        request<ReportVersionSummary[]>(`/manager/reports/${reportId}/versions`),
        request<Review[]>(`/manager/reports/${reportId}/reviews`),
      ]);
      setReport(loadedReport);
      setVersions(loadedVersions);
      setReviews([...loadedReviews].sort((left, right) => right.createdAt.localeCompare(left.createdAt)));
      setSelectedVersion(loadedReport.submittedVersion);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not load this team report.");
    } finally {
      setLoading(false);
    }
  }, [reportId, request]);

  useEffect(() => {
    if (!allowed) return;
    void Promise.resolve().then(load);
  }, [allowed, load]);

  async function chooseVersion(versionNumber: number) {
    if (selectedVersion?.versionNumber === versionNumber) return;
    setVersionLoading(true);
    setError("");
    try {
      setSelectedVersion(await request<ReportVersion>(
        `/manager/reports/${reportId}/versions/${versionNumber}`,
      ));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not load that report version.");
    } finally {
      setVersionLoading(false);
    }
  }

  async function submitReview(action: ReviewAction) {
    const normalizedComment = comment.trim();
    if (action === "CHANGES_REQUESTED" && !normalizedComment) {
      setReviewError("Explain what the team member needs to change.");
      return;
    }
    if (action === "APPROVED" && !window.confirm("Approve this submitted report? This review cannot be replaced.")) {
      return;
    }

    setSubmittingAction(action);
    setReviewError("");
    try {
      await request(`/manager/reports/${reportId}/reviews`, {
        method: "POST",
        body: JSON.stringify({ action, comment: normalizedComment || null }),
      });
      setComment("");
      await load();
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 409) {
        setReviewError("This submission was already reviewed or changed. The latest report state has been reloaded.");
        await load();
      } else {
        setReviewError(caught instanceof Error ? caught.message : "Could not save the review.");
      }
    } finally {
      setSubmittingAction(null);
    }
  }

  if (!allowed) return <AccessDenied />;
  if (loading) return <DetailSkeleton />;
  if (!report || !selectedVersion) return <ErrorCard message={error || "Report not found."} />;

  return <article className="mx-auto max-w-6xl pb-16">
    <Link href="/manager/reports" className="text-sm font-semibold text-brand hover:text-brand-strong">← Team reports</Link>
    <header className="mt-5 flex flex-col gap-5 border-b border-slate-200 pb-7 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <div className="flex flex-wrap items-center gap-3">
          <p className="text-sm font-semibold text-brand">{formatDateRange(report.weekStart, report.weekEnd)}</p>
          <StatusBadge status={report.status} />
        </div>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight"><Link href={`/manager/team-members/${report.member.id}`} className="hover:text-brand hover:underline">{report.member.firstName} {report.member.lastName}</Link></h1>
        <p className="mt-2 text-sm text-slate-500">{report.member.email} · Submitted {formatDateTime(report.submittedAt!)}</p>
      </div>
      <label className="w-full text-sm font-medium text-slate-700 sm:w-64">Submitted version
        <select
          className="report-select mt-1"
          value={selectedVersion.versionNumber}
          disabled={versionLoading}
          onChange={event => void chooseVersion(Number(event.target.value))}
        >
          {versions.map(version => <option key={version.id} value={version.versionNumber}>
            Version {version.versionNumber}{version.current ? " · current" : ""}
          </option>)}
        </select>
      </label>
    </header>

    {error && <div role="alert" className="mt-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

    <div className="mt-6 grid gap-4 sm:grid-cols-3">
      <Meta name="Viewing" value={`Version ${selectedVersion.versionNumber}`} />
      <Meta name="Version submitted" value={selectedVersion.submittedAt ? formatDateTime(selectedVersion.submittedAt) : "Not submitted"} />
      <Meta name="Approved" value={report.approvedAt ? formatDateTime(report.approvedAt) : "Not approved"} />
    </div>

    <ReportVersionContent version={selectedVersion} busy={versionLoading} />

    {report.status === "SUBMITTED" && <form onSubmit={event => { event.preventDefault(); void submitReview("CHANGES_REQUESTED"); }} className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
      <h2 className="font-semibold text-slate-900">Review this submission</h2>
      <p className="mt-1 text-sm text-slate-500">A change request creates a new private correction draft. Approval closes the workflow.</p>
      <label className="mt-4 block text-sm font-medium text-slate-700">Review comment
        <textarea
          className="report-textarea mt-1"
          maxLength={2000}
          value={comment}
          disabled={submittingAction !== null}
          onChange={event => setComment(event.target.value)}
          placeholder="Required when requesting changes; optional when approving."
        />
      </label>
      <div className="mt-4 flex flex-wrap gap-3">
        <button type="submit" className="report-secondary" disabled={submittingAction !== null}>
          {submittingAction === "CHANGES_REQUESTED" ? "Requesting…" : "Request changes"}
        </button>
        <button type="button" className="report-primary" disabled={submittingAction !== null} onClick={() => void submitReview("APPROVED")}>
          {submittingAction === "APPROVED" ? "Approving…" : "Approve report"}
        </button>
      </div>
      {reviewError && <p role="alert" className="mt-3 text-sm font-medium text-red-700">{reviewError}</p>}
    </form>}

    <section className="mt-6 rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
      <h2 className="font-semibold text-slate-900">Review history</h2>
      {reviews.length ? <ol className="mt-4 space-y-4">{reviews.map(review => <li key={review.id} className="border-l-2 border-slate-200 pl-4">
        <div className="flex flex-wrap items-center gap-2"><Tag>{label(review.action)}</Tag><span className="text-xs text-slate-400">Version {review.reportVersionNumber} · {formatDateTime(review.createdAt)}</span></div>
        <p className="mt-2 text-sm font-medium text-slate-700">{review.reviewer.firstName} {review.reviewer.lastName}</p>
        {review.comment && <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-slate-600">{review.comment}</p>}
      </li>)}</ol> : <p className="mt-3 text-sm text-slate-500">No reviews yet.</p>}
    </section>
  </article>;
}

function ReportVersionContent({ version, busy }: { version: ReportVersion; busy: boolean }) {
  const totalMinutes = version.timeEntries.reduce((sum, entry) => sum + entry.minutes, 0);
  return <div className={`mt-6 space-y-6 transition-opacity ${busy ? "opacity-50" : "opacity-100"}`} aria-busy={busy}>
    <Section number="01" title="Tasks completed" empty={!version.completedTasks.length}>
      {version.completedTasks.map((task, index) => <div key={`${task.taskName}-${index}`} className="rounded-xl border border-slate-100 bg-slate-50/60 p-4">
        <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-semibold text-slate-900">{task.taskName}</h3><p className="mt-1 text-sm text-slate-500">{task.projectName || "No project"}</p></div><Tag>{label(task.status)}</Tag></div>
        {task.description && <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{task.description}</p>}
        <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4"><Fact term="Priority" value={label(task.priority)} /><Fact term="Progress" value={`${task.actualPercentage}% actual / ${task.plannedPercentage}% planned`} /><Fact term="Time planned" value={formatMinutes(task.plannedMinutes)} /><Fact term="Time spent" value={formatMinutes(task.spentMinutes)} /></dl>
        {task.deliverable && <div className="mt-4 border-t border-slate-200 pt-3 text-sm"><span className="font-medium text-slate-500">Output: </span><SafeDeliverable value={task.deliverable} /></div>}
      </div>)}
    </Section>
    <Section number="02" title="Plans for next week" empty={!version.plannedTasks.length}>
      {version.plannedTasks.map((task, index) => <div key={`${task.taskName}-${index}`} className="rounded-xl border border-slate-100 bg-slate-50/60 p-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-semibold text-slate-900">{task.taskName}</h3><p className="mt-1 text-sm text-slate-500">{task.projectName || "No project"}</p></div><Tag>{label(task.priority)}</Tag></div>{task.description && <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{task.description}</p>}<p className="mt-3 text-sm text-slate-500">Estimated time: <span className="font-medium text-slate-700">{formatMinutes(task.estimatedMinutes)}</span></p></div>)}
    </Section>
    <Section number="03" title="Blockers and challenges" empty={!version.blockers.length}>
      {version.blockers.map((item, index) => <div key={index} className="rounded-xl border border-slate-100 bg-slate-50/60 p-4"><div className="flex flex-wrap gap-2">{item.keyBlocker && <Tag>Key blocker</Tag>}<Tag>{item.resolved ? "Resolved" : "Open"}</Tag></div><p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{item.description}</p></div>)}
    </Section>
    <Section number="04" title="Achievements and highlights" empty={!version.achievements.length}>
      {version.achievements.map((item, index) => <div key={index} className="rounded-xl border border-slate-100 bg-slate-50/60 p-4">{item.keyAchievement && <Tag>Key achievement</Tag>}<p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{item.description}</p></div>)}
    </Section>
    <Section number="05" title="Time by task type" empty={!version.timeEntries.length}>
      {!!version.timeEntries.length && <><div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{version.timeEntries.map(entry => <div key={entry.taskType} className="rounded-xl bg-slate-50 p-4"><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label(entry.taskType)}</p><p className="mt-2 text-lg font-semibold text-slate-900">{formatMinutes(entry.minutes)}</p></div>)}</div><p className="text-right text-sm font-semibold text-slate-700">Total: {formatMinutes(totalMinutes)}</p></>}
    </Section>
    <Section number="06" title="Notes or links" empty={!version.notes?.trim()}>{version.notes?.trim() && <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">{version.notes}</p>}</Section>
  </div>;
}

function Section({ number, title, empty, children }: { number: string; title: string; empty: boolean; children: ReactNode }) { return <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6"><div className="mb-5 flex gap-4"><span className="text-xs font-bold text-brand">{number}</span><h2 className="font-semibold text-slate-900">{title}</h2></div><div className="space-y-4">{empty ? <p className="text-sm text-slate-400">Nothing recorded.</p> : children}</div></section>; }
function Meta({ name, value }: { name: string; value: string }) { return <div className="rounded-xl border border-slate-200 bg-white p-4"><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{name}</p><p className="mt-2 text-sm font-semibold text-slate-800">{value}</p></div>; }
function Fact({ term, value }: { term: string; value: string }) { return <div><dt className="text-xs font-medium text-slate-400">{term}</dt><dd className="mt-1 text-slate-700">{value}</dd></div>; }
function Tag({ children }: { children: ReactNode }) { return <span className="inline-block rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600 ring-1 ring-slate-200">{children}</span>; }
function formatMinutes(minutes: number | null) { if (minutes === null) return "Not recorded"; return `${Math.floor(minutes / 60)}h ${minutes % 60}m`; }
function SafeDeliverable({ value }: { value: string }) { const url = parseHttpUrl(value); return url ? <a href={url} target="_blank" rel="noopener noreferrer" className="font-medium text-brand hover:underline">{value}</a> : <span className="whitespace-pre-wrap text-slate-700">{value}</span>; }
function parseHttpUrl(value: string) { try { const url = new URL(value); return url.protocol === "http:" || url.protocol === "https:" ? url.href : null; } catch { return null; } }
function DetailSkeleton() { return <div className="mx-auto max-w-6xl space-y-5" aria-label="Loading team report"><div className="h-28 animate-pulse rounded-2xl bg-white" />{[0, 1, 2].map(item => <div key={item} className="h-48 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}</div>; }
function ErrorCard({ message }: { message: string }) { return <div className="mx-auto max-w-xl rounded-2xl border border-red-200 bg-red-50 p-6"><h1 className="font-semibold text-red-900">Could not open report</h1><p className="mt-2 text-sm text-red-700">{message}</p><Link href="/manager/reports" className="mt-4 inline-block font-semibold text-brand">Return to team reports</Link></div>; }
function AccessDenied() { return <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6"><h1 className="font-semibold text-amber-950">Manager access required</h1><p className="mt-2 text-sm text-amber-800">Only managers and administrators can review team reports.</p><Link href="/dashboard" className="mt-4 inline-block font-semibold text-brand">Return to dashboard</Link></div>; }
