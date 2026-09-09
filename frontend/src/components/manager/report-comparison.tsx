"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import { formatDateRange } from "@/components/reports/report-history";
import { WeekPicker } from "@/components/ui/week-picker";
import {
  isMonday,
  label,
  type MemberSectionComparison,
  type SubmissionTiming,
  type SubmissionTrackingStatus,
  type WeeklySectionComparison,
} from "@/lib/reports/model";

type Section = "blockers" | "achievements";

const statusStyles: Record<SubmissionTrackingStatus, string> = {
  NOT_STARTED: "bg-slate-100 text-slate-600",
  DRAFT: "bg-amber-50 text-amber-700",
  SUBMITTED: "bg-blue-50 text-blue-700",
  NEEDS_CORRECTION: "bg-orange-50 text-orange-700",
  APPROVED: "bg-emerald-50 text-emerald-700",
};

const timingStyles: Record<SubmissionTiming, string> = {
  ON_TIME: "text-emerald-700",
  PENDING: "text-slate-500",
  LATE: "text-red-700",
};

export function ReportComparison() {
  const { user, request } = useAuth();
  const [comparison, setComparison] = useState<WeeklySectionComparison | null>(null);
  const [section, setSection] = useState<Section>("blockers");
  const [weekInput, setWeekInput] = useState("");
  const [selectedWeek, setSelectedWeek] = useState("");
  const [hideEmpty, setHideEmpty] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [weekError, setWeekError] = useState("");
  const generation = useRef(0);
  const allowed = user?.role === "MANAGER" || user?.role === "ADMIN";

  const load = useCallback(async (week: string) => {
    const currentGeneration = ++generation.current;
    setLoading(true);
    setError("");
    try {
      const path = week
        ? `/manager/report-comparisons?weekStart=${encodeURIComponent(week)}`
        : "/manager/report-comparisons";
      const loaded = await request<WeeklySectionComparison>(path);
      if (currentGeneration !== generation.current) return;
      setComparison(loaded);
      setSelectedWeek(loaded.weekStart);
      setWeekInput(loaded.weekStart);
    } catch (caught) {
      if (currentGeneration === generation.current) {
        setError(caught instanceof Error ? caught.message : "Could not load report comparisons.");
      }
    } finally {
      if (currentGeneration === generation.current) setLoading(false);
    }
  }, [request]);

  useEffect(() => {
    if (allowed) void Promise.resolve().then(() => load(""));
    return () => { generation.current += 1; };
  }, [allowed, load]);

  if (!allowed) return <AccessDenied />;

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
    if (!selectedWeek) return;
    const date = new Date(`${selectedWeek}T12:00:00Z`);
    date.setUTCDate(date.getUTCDate() + offset * 7);
    const nextWeek = date.toISOString().slice(0, 10);
    setWeekError("");
    setWeekInput(nextWeek);
    void load(nextWeek);
  }

  const members = comparison?.members ?? [];
  const membersWithItems = members.filter(member => itemsFor(member, section).length > 0).length;
  const visibleMembers = hideEmpty
    ? members.filter(member => itemsFor(member, section).length > 0)
    : members;

  return <section className="mx-auto max-w-7xl animate-soft-in">
    <header className="flex flex-col gap-6 border-b border-slate-200 pb-7 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p className="text-sm font-semibold text-brand">Manager workspace</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-[-0.04em] text-slate-950 sm:text-4xl">Compare weekly sections</h1>
        <p className="mt-3 max-w-2xl leading-7 text-slate-500">Scan one fixed report section across the whole team without opening reports individually.</p>
      </div>
      <form onSubmit={chooseWeek} className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:items-end" aria-label="Choose comparison week">
        <div className="flex items-end gap-2">
          <button type="button" className="report-secondary" disabled={loading || !selectedWeek} onClick={() => moveWeek(-1)} aria-label="Previous week">←</button>
          <WeekPicker label="Week starting" value={weekInput} onChange={setWeekInput} disabled={loading} id="comparison-week" />
          <button type="button" className="report-secondary" disabled={loading || !selectedWeek} onClick={() => moveWeek(1)} aria-label="Next week">→</button>
        </div>
        <button type="submit" className="report-primary w-full sm:w-auto" disabled={loading}>View week</button>
      </form>
    </header>

    {weekError && <p role="alert" className="mt-3 text-sm font-medium text-red-700">{weekError}</p>}
    {error && <ErrorNotice message={error} onRetry={() => load(selectedWeek)} />}
    {loading && !comparison ? <ComparisonSkeleton /> : comparison && <>
      <div className="mt-6 rounded-2xl border border-slate-200 bg-white p-4 sm:flex sm:items-center sm:justify-between sm:gap-5 sm:p-5">
        <div>
          <p className="text-sm font-semibold text-slate-900">{formatDateRange(comparison.weekStart, comparison.weekEnd)}</p>
          <p className="mt-1 text-xs text-slate-400">{membersWithItems} of {members.length} members reported {section}</p>
        </div>
        <div className="mt-4 flex flex-col gap-4 sm:mt-0 sm:flex-row sm:items-center">
          <div role="tablist" aria-label="Report section" className="grid grid-cols-2 rounded-xl bg-slate-100 p-1">
            <SectionTab selected={section === "blockers"} onSelect={() => setSection("blockers")}>Blockers</SectionTab>
            <SectionTab selected={section === "achievements"} onSelect={() => setSection("achievements")}>Achievements</SectionTab>
          </div>
          <label className="flex items-center gap-2 text-sm font-medium text-slate-600"><input type="checkbox" checked={hideEmpty} onChange={event => setHideEmpty(event.target.checked)} className="size-4 accent-brand" />Hide empty</label>
        </div>
      </div>

      {loading && <p role="status" className="mt-4 text-sm font-medium text-brand">Updating comparison…</p>}
      <div id="comparison-panel" role="tabpanel" className="mt-6">
        {visibleMembers.length ? <>
          <div className="grid snap-x snap-mandatory grid-flow-col auto-cols-[minmax(280px,85vw)] gap-4 overflow-x-auto pb-4 sm:auto-cols-[340px]" aria-label={`${label(section)} by team member`}>
            {visibleMembers.map(member => <MemberCard key={member.member.id} member={member} section={section} />)}
          </div>
          <p className="mt-1 text-xs text-slate-400">Scroll horizontally to compare every active team member.</p>
        </> : <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center"><h2 className="text-lg font-semibold text-slate-800">No {section} reported</h2><p className="mt-2 text-sm text-slate-500">Show empty members or choose another reporting week.</p></div>}
      </div>
    </>}
  </section>;
}

function MemberCard({ member, section }: { member: MemberSectionComparison; section: Section }) {
  const items = itemsFor(member, section);
  return <article className="flex min-h-72 snap-start flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.03)]">
    <div className="border-b border-slate-100 pb-4">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0"><Link href={`/manager/team-members/${member.member.id}`} className="font-semibold text-slate-900 hover:text-brand hover:underline">{member.member.firstName} {member.member.lastName}</Link><p className="mt-1 truncate text-xs text-slate-400">{member.member.email}</p></div>
        <span className={`shrink-0 rounded-full px-2 py-1 text-[10px] font-semibold ${statusStyles[member.status]}`}>{label(member.status)}</span>
      </div>
      <p className={`mt-3 text-xs font-semibold ${timingStyles[member.timing]}`}>{label(member.timing)}{member.submittedVersionNumber ? ` · Version ${member.submittedVersionNumber}` : ""}</p>
    </div>

    <div className="flex-1 pt-4">
      {items.length ? <ul className="space-y-3">{section === "blockers"
        ? member.blockers.map(blocker => <li key={blocker.id} className={`rounded-xl border p-3 ${blocker.keyBlocker ? "border-orange-200 bg-orange-50/60" : "border-slate-100 bg-slate-50"}`}><div className="flex flex-wrap gap-1.5">{blocker.keyBlocker && <ItemTag tone="orange">Key blocker</ItemTag>}<ItemTag tone={blocker.resolved ? "green" : "red"}>{blocker.resolved ? "Resolved" : "Open"}</ItemTag></div><p className={`mt-2 text-sm leading-6 ${blocker.resolved ? "text-slate-500 line-through decoration-slate-300" : "text-slate-700"}`}>{blocker.description}</p></li>)
        : member.achievements.map(achievement => <li key={achievement.id} className={`rounded-xl border p-3 ${achievement.keyAchievement ? "border-emerald-200 bg-emerald-50/60" : "border-slate-100 bg-slate-50"}`}>{achievement.keyAchievement && <ItemTag tone="green">Key achievement</ItemTag>}<p className="mt-2 text-sm leading-6 text-slate-700">{achievement.description}</p></li>)}</ul>
        : <EmptyMember member={member} section={section} />}
    </div>

    {member.reportId && member.submittedVersionNumber && <Link href={`/manager/reports/${member.reportId}`} className="mt-5 text-sm font-semibold text-brand hover:text-brand-strong">Open submitted report →</Link>}
  </article>;
}

function EmptyMember({ member, section }: { member: MemberSectionComparison; section: Section }) {
  const message = member.status === "DRAFT"
    ? "Draft content stays private until submission."
    : member.status === "NOT_STARTED"
      ? "This member has not started a report."
      : `No ${section} were included in the submitted report.`;
  return <div className="grid min-h-32 place-items-center rounded-xl border border-dashed border-slate-200 bg-slate-50 p-4 text-center text-sm leading-6 text-slate-400">{message}</div>;
}

function itemsFor(member: MemberSectionComparison, section: Section) { return section === "blockers" ? member.blockers : member.achievements; }
function SectionTab({ selected, onSelect, children }: { selected: boolean; onSelect(): void; children: React.ReactNode }) { return <button type="button" role="tab" aria-selected={selected} aria-controls="comparison-panel" onClick={onSelect} className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${selected ? "bg-white text-brand-strong shadow-sm" : "text-slate-500 hover:text-slate-800"}`}>{children}</button>; }
function ItemTag({ tone, children }: { tone: "orange" | "red" | "green"; children: React.ReactNode }) { const style = tone === "orange" ? "bg-orange-100 text-orange-700" : tone === "red" ? "bg-red-100 text-red-700" : "bg-emerald-100 text-emerald-700"; return <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${style}`}>{children}</span>; }
function ErrorNotice({ message, onRetry }: { message: string; onRetry(): Promise<void> }) { return <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"><p>{message}</p><button type="button" className="mt-2 font-semibold underline" onClick={() => void onRetry()}>Try again</button></div>; }
function ComparisonSkeleton() { return <div className="mt-6" aria-label="Loading report comparison"><div className="h-24 animate-pulse rounded-2xl border border-slate-200 bg-white" /><div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">{[0, 1, 2].map(item => <div key={item} className="h-72 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}</div></div>; }
function AccessDenied() { return <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6"><h1 className="font-semibold text-amber-950">Manager access required</h1><p className="mt-2 text-sm text-amber-800">Cross-member report comparison is available to managers and administrators.</p><Link href="/dashboard" className="mt-4 inline-block font-semibold text-brand">Return to dashboard</Link></div>; }
