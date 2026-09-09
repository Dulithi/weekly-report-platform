"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import { formatDateRange, formatDateTime } from "@/components/reports/report-history";
import { WeekPicker } from "@/components/ui/week-picker";
import {
  isMonday,
  label,
  type ManagerDashboard,
  type ReportActivityType,
  type SubmissionTiming,
  type SubmissionTracking,
  type SubmissionTrackingStatus,
} from "@/lib/reports/model";

const TREND_WEEKS = 8;

const statusStyles: Record<SubmissionTrackingStatus, string> = {
  NOT_STARTED: "bg-slate-100 text-slate-600",
  DRAFT: "bg-amber-50 text-amber-700",
  SUBMITTED: "bg-blue-50 text-blue-700",
  NEEDS_CORRECTION: "bg-orange-50 text-orange-700",
  APPROVED: "bg-emerald-50 text-emerald-700",
};

const timingStyles: Record<SubmissionTiming, string> = {
  ON_TIME: "bg-emerald-50 text-emerald-700",
  PENDING: "bg-slate-100 text-slate-600",
  LATE: "bg-red-50 text-red-700",
};

const activityLabels: Record<ReportActivityType, string> = {
  REPORT_CREATED: "started a weekly report",
  REPORT_SUBMITTED: "submitted a weekly report",
  REPORT_RESUBMITTED: "resubmitted a corrected report",
  REPORT_CHANGES_REQUESTED: "requested report changes",
  REPORT_APPROVED: "approved a weekly report",
};

export function ManagerDashboardView() {
  const { request } = useAuth();
  const [dashboard, setDashboard] = useState<ManagerDashboard | null>(null);
  const [weekInput, setWeekInput] = useState("");
  const [selectedWeek, setSelectedWeek] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [weekError, setWeekError] = useState("");
  const generation = useRef(0);

  const load = useCallback(async (week: string) => {
    const currentGeneration = ++generation.current;
    setLoading(true);
    setError("");
    const query = new URLSearchParams({ trendWeeks: String(TREND_WEEKS) });
    if (week) query.set("weekStart", week);
    try {
      const loaded = await request<ManagerDashboard>(`/manager/dashboard?${query}`);
      if (currentGeneration !== generation.current) return;
      setDashboard(loaded);
      setSelectedWeek(loaded.weekStart);
      setWeekInput(loaded.weekStart);
    } catch (caught) {
      if (currentGeneration === generation.current) {
        setError(caught instanceof Error ? caught.message : "Could not load the dashboard.");
      }
    } finally {
      if (currentGeneration === generation.current) setLoading(false);
    }
  }, [request]);

  useEffect(() => {
    void Promise.resolve().then(() => load(""));
    return () => { generation.current += 1; };
  }, [load]);

  function chooseWeek(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!isMonday(weekInput)) {
      setWeekError("Choose a Monday so the date matches the reporting week.");
      return;
    }
    setWeekError("");
    void load(weekInput);
  }

  function moveWeek(offset: number) {
    const base = selectedWeek || dashboard?.weekStart;
    if (!base) return;
    const date = new Date(`${base}T12:00:00Z`);
    date.setUTCDate(date.getUTCDate() + offset * 7);
    const nextWeek = date.toISOString().slice(0, 10);
    setWeekError("");
    setWeekInput(nextWeek);
    void load(nextWeek);
  }

  return <section className="mx-auto max-w-7xl animate-soft-in">
    <header className="flex flex-col gap-6 border-b border-slate-200 pb-7 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p className="text-sm font-semibold text-brand">Manager workspace</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-[-0.04em] text-slate-950 sm:text-4xl">Team overview</h1>
        <p className="mt-3 max-w-2xl leading-7 text-slate-500">See submission health, completed work, workload and blockers for one reporting week.</p>
      </div>
      <form onSubmit={chooseWeek} className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:items-end" aria-label="Choose dashboard week">
        <div className="flex items-end gap-2">
          <button type="button" className="report-secondary" disabled={loading || !selectedWeek} onClick={() => moveWeek(-1)} aria-label="Previous week">←</button>
          <WeekPicker label="Week starting" value={weekInput} onChange={setWeekInput} disabled={loading} id="dashboard-week" />
          <button type="button" className="report-secondary" disabled={loading || !selectedWeek} onClick={() => moveWeek(1)} aria-label="Next week">→</button>
        </div>
        <button type="submit" className="report-primary w-full sm:w-auto" disabled={loading}>View week</button>
      </form>
    </header>

    {weekError && <p role="alert" className="mt-3 text-sm font-medium text-red-700">{weekError}</p>}
    {error && <ErrorNotice message={error} onRetry={() => load(selectedWeek)} />}

    {loading && !dashboard ? <DashboardSkeleton /> : dashboard && <>
      <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-900">{formatDateRange(dashboard.weekStart, dashboard.weekEnd)}</p>
          <p className="mt-1 text-xs text-slate-400">Deadline {formatDateTime(dashboard.dueAt)}</p>
        </div>
        {loading && <span role="status" className="text-sm font-medium text-brand">Updating…</span>}
      </div>

      <SummaryCards dashboard={dashboard} />

      <div className="mt-6 grid gap-6 xl:grid-cols-2">
        <ChartCard title="Completed tasks" description={`Weekly trend across the last ${TREND_WEEKS} reporting weeks.`}>
          <CompletedTrend data={dashboard.completedTaskTrend} />
        </ChartCard>
        <ChartCard title="Submission status by member" description="Current workflow state and deadline timing for every active member.">
          <MemberStatusChart submissions={dashboard.submissionsByMember} />
        </ChartCard>
        <ChartCard title="Workload by project" description="Task entries in the latest submitted report versions for this week.">
          <HorizontalBars data={dashboard.projectTaskDistribution.map(item => ({ key: item.projectId ?? "uncategorized", name: item.projectName, value: item.taskCount, display: `${item.taskCount} ${item.taskCount === 1 ? "task" : "tasks"}` }))} empty="No submitted project work for this week." />
        </ChartCard>
        <ChartCard title="Time by task type" description="Team-wide time entries from the latest submitted report versions.">
          <HorizontalBars data={dashboard.timeByTaskType.map(item => ({ key: item.taskType, name: label(item.taskType), value: item.minutes, display: formatMinutes(item.minutes) }))} empty="No submitted time entries for this week." accent />
        </ChartCard>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1.5fr)_minmax(320px,0.75fr)]">
        <ComplianceTable submissions={dashboard.submissionsByMember} />
        <ActivityFeed activities={dashboard.recentActivity} />
      </div>
    </>}
  </section>;
}

function SummaryCards({ dashboard }: { dashboard: ManagerDashboard }) {
  const { summary } = dashboard;
  const cards = [
    { label: "Reports submitted", value: `${summary.submittedReports} / ${summary.totalActiveMembers}`, detail: `${summary.submissionRatePercent.toFixed(1)}% submitted · ${summary.pendingSubmissions} pending · ${summary.lateSubmissions} late`, tone: "text-brand" },
    { label: "On-time compliance", value: `${summary.onTimeComplianceRatePercent.toFixed(1)}%`, detail: `${summary.onTimeSubmissions} submitted before the deadline`, tone: "text-emerald-700" },
    { label: "Needs correction", value: String(summary.needsCorrectionReports), detail: "Reports waiting for a member update", tone: "text-orange-700" },
    { label: "Open blockers", value: String(summary.openBlockers), detail: "Unresolved entries in submitted reports", tone: "text-red-700" },
  ];
  return <section aria-label="Weekly summary" className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
    {cards.map(card => <article key={card.label} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.03)]">
      <p className="text-sm font-medium text-slate-500">{card.label}</p>
      <p className={`mt-3 text-3xl font-semibold tracking-tight ${card.tone}`}>{card.value}</p>
      <p className="mt-2 text-xs leading-5 text-slate-400">{card.detail}</p>
    </article>)}
  </section>;
}

function ChartCard({ title, description, children }: { title: string; description: string; children: React.ReactNode }) {
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6" aria-labelledby={`chart-${title.replaceAll(" ", "-").toLowerCase()}`}>
    <h2 id={`chart-${title.replaceAll(" ", "-").toLowerCase()}`} className="text-lg font-semibold tracking-tight text-slate-900">{title}</h2>
    <p className="mt-1 text-sm leading-6 text-slate-500">{description}</p>
    <div className="mt-6">{children}</div>
  </section>;
}

function CompletedTrend({ data }: { data: ManagerDashboard["completedTaskTrend"] }) {
  const maximum = Math.max(1, ...data.map(item => item.completedTasks));
  return <div>
    <div className="flex h-48 items-end gap-2 border-b border-slate-200" aria-label="Completed tasks by week">
      {data.map(item => <div key={item.weekStart} className="flex h-full min-w-0 flex-1 flex-col justify-end text-center">
        <span className="mb-2 text-xs font-semibold text-slate-600">{item.completedTasks}</span>
        <span aria-hidden="true" className="mx-auto w-full max-w-10 rounded-t-md bg-brand transition-[height]" style={{ height: item.completedTasks ? `${Math.max(8, item.completedTasks / maximum * 100)}%` : "3px" }} />
      </div>)}
    </div>
    <div className="mt-2 flex gap-2">
      {data.map(item => <span key={item.weekStart} className="min-w-0 flex-1 text-center text-[10px] text-slate-400 sm:text-xs">{shortDate(item.weekStart)}</span>)}
    </div>
  </div>;
}

function MemberStatusChart({ submissions }: { submissions: SubmissionTracking[] }) {
  if (!submissions.length) return <EmptyChart>There are no active team members.</EmptyChart>;
  return <ul className="max-h-64 space-y-3 overflow-y-auto pr-1">
    {submissions.map(item => <li key={item.member.id} className="grid grid-cols-[minmax(110px,0.8fr)_minmax(80px,1fr)_auto] items-center gap-3 text-sm">
      <Link href={`/manager/team-members/${item.member.id}`} className="truncate font-medium text-slate-700 hover:text-brand hover:underline">{item.member.firstName} {item.member.lastName}</Link>
      <span className={`h-2.5 rounded-full ${statusBar(item.status)}`} aria-hidden="true" />
      <span className="text-right">
        <span className={`inline-flex rounded-full px-2 py-1 text-[11px] font-semibold ${statusStyles[item.status]}`}>{label(item.status)}</span>
        <span className={`mt-1 block text-[10px] font-semibold ${item.timing === "LATE" ? "text-red-600" : "text-slate-400"}`}>{label(item.timing)}</span>
      </span>
    </li>)}
  </ul>;
}

function HorizontalBars({ data, empty, accent = false }: { data: Array<{ key: string; name: string; value: number; display: string }>; empty: string; accent?: boolean }) {
  if (!data.length) return <EmptyChart>{empty}</EmptyChart>;
  const maximum = Math.max(1, ...data.map(item => item.value));
  return <ul className="space-y-4">
    {data.map(item => <li key={item.key}>
      <div className="mb-1.5 flex items-center justify-between gap-4 text-sm"><span className="truncate font-medium text-slate-700">{item.name}</span><span className="shrink-0 text-xs font-semibold text-slate-500">{item.display}</span></div>
      <div className="h-2.5 overflow-hidden rounded-full bg-slate-100" aria-hidden="true"><div className={`h-full rounded-full ${accent ? "bg-accent" : "bg-brand"}`} style={{ width: `${item.value / maximum * 100}%` }} /></div>
    </li>)}
  </ul>;
}

function ComplianceTable({ submissions }: { submissions: SubmissionTracking[] }) {
  return <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white" aria-labelledby="compliance-heading">
    <div className="border-b border-slate-100 p-5 sm:px-6"><h2 id="compliance-heading" className="text-lg font-semibold tracking-tight text-slate-900">Submission compliance</h2><p className="mt-1 text-sm text-slate-500">Workflow and deadline timing stay separate for accurate follow-up.</p></div>
    <div className="overflow-x-auto">
      <table className="w-full min-w-[620px] text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-400"><tr><th className="px-6 py-3 font-semibold">Member</th><th className="px-4 py-3 font-semibold">Status</th><th className="px-4 py-3 font-semibold">Timing</th><th className="px-6 py-3 text-right font-semibold">Report</th></tr></thead>
        <tbody className="divide-y divide-slate-100">{submissions.map(item => <tr key={item.member.id}>
          <td className="px-6 py-4"><Link href={`/manager/team-members/${item.member.id}`} className="font-semibold text-slate-800 hover:text-brand hover:underline">{item.member.firstName} {item.member.lastName}</Link><span className="mt-0.5 block text-xs text-slate-400">{item.member.email}</span></td>
          <td className="px-4 py-4"><StatusPill status={item.status} /></td>
          <td className="px-4 py-4"><TimingPill timing={item.timing} /></td>
          <td className="px-6 py-4 text-right">{item.reportId && item.status !== "DRAFT" ? <Link href={`/manager/reports/${item.reportId}`} className="font-semibold text-brand hover:text-brand-strong">Open →</Link> : <span className="text-xs text-slate-400">{item.status === "DRAFT" ? "Private draft" : "Not started"}</span>}</td>
        </tr>)}</tbody>
      </table>
    </div>
  </section>;
}

function ActivityFeed({ activities }: { activities: ManagerDashboard["recentActivity"] }) {
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6" aria-labelledby="activity-heading">
    <h2 id="activity-heading" className="text-lg font-semibold tracking-tight text-slate-900">Recent activity</h2>
    <p className="mt-1 text-sm text-slate-500">The latest report and review events.</p>
    {activities.length ? <ol className="mt-5 space-y-5">{activities.map(activity => {
      const actor = activity.actor ? `${activity.actor.firstName} ${activity.actor.lastName}` : "System";
      const body = <><span className="font-semibold text-slate-800">{actor}</span> <span className="text-slate-600">{activityLabels[activity.type]}</span><span className="mt-1 block text-xs text-slate-400">{formatDateTime(activity.createdAt)}</span></>;
      const canOpen = activity.reportId && activity.type !== "REPORT_CREATED";
      return <li key={activity.id} className="flex gap-3"><span aria-hidden="true" className="mt-1 size-2.5 shrink-0 rounded-full bg-brand" /><div className="min-w-0 text-sm leading-5">{canOpen ? <Link href={`/manager/reports/${activity.reportId}`} className="group hover:underline">{body}</Link> : body}</div></li>;
    })}</ol> : <p className="mt-5 text-sm text-slate-400">No report activity has been recorded yet.</p>}
  </section>;
}

function StatusPill({ status }: { status: SubmissionTrackingStatus }) { return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${statusStyles[status]}`}>{label(status)}</span>; }
function TimingPill({ timing }: { timing: SubmissionTiming }) { return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${timingStyles[timing]}`}>{label(timing)}</span>; }
function EmptyChart({ children }: { children: React.ReactNode }) { return <div className="grid min-h-36 place-items-center rounded-xl border border-dashed border-slate-200 bg-slate-50 p-5 text-center text-sm text-slate-400">{children}</div>; }
function ErrorNotice({ message, onRetry }: { message: string; onRetry(): Promise<void> }) { return <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"><p>{message}</p><button type="button" onClick={() => void onRetry()} className="mt-2 font-semibold underline">Try again</button></div>; }
function DashboardSkeleton() { return <div className="mt-6 space-y-6" aria-label="Loading dashboard"><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{[0, 1, 2, 3].map(item => <div key={item} className="h-32 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}</div><div className="grid gap-6 xl:grid-cols-2">{[0, 1, 2, 3].map(item => <div key={item} className="h-72 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}</div></div>; }
function shortDate(value: string) { return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric", timeZone: "UTC" }).format(new Date(`${value}T12:00:00Z`)); }
function formatMinutes(minutes: number) { const hours = Math.floor(minutes / 60); const remainder = minutes % 60; return hours ? `${hours}h${remainder ? ` ${remainder}m` : ""}` : `${remainder}m`; }
function statusBar(status: SubmissionTrackingStatus) { return status === "APPROVED" ? "bg-emerald-500" : status === "SUBMITTED" ? "bg-blue-500" : status === "NEEDS_CORRECTION" ? "bg-orange-400" : status === "DRAFT" ? "bg-amber-400" : "bg-slate-300"; }
