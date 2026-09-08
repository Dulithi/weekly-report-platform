"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";

import { useAuth } from "@/components/auth/auth-provider";
import { ApiError } from "@/lib/api/client";
import {
  label,
  priorities,
  taskStatuses,
  taskTypes,
  updatePayload,
  type Achievement,
  type Blocker,
  type CompletedTask,
  type Page,
  type PlannedTask,
  type Project,
  type ReportContent,
  type Review,
  type WeeklyReport,
} from "@/lib/reports/model";

const emptyCompleted = (): CompletedTask => ({ projectId: null, taskName: "", description: null, priority: "MEDIUM", plannedPercentage: 100, actualPercentage: 100, status: "COMPLETED", plannedMinutes: null, spentMinutes: null, deliverable: null });
const emptyPlanned = (): PlannedTask => ({ projectId: null, taskName: "", description: null, priority: "MEDIUM", estimatedMinutes: null });
const emptyBlocker = (): Blocker => ({ description: "", keyBlocker: false, resolved: false });
const emptyAchievement = (): Achievement => ({ description: "", keyAchievement: false });

export function ReportEditor({ reportId }: { reportId: string }) {
  const { user, request } = useAuth();
  const [report, setReport] = useState<WeeklyReport | null>(null);
  const [content, setContent] = useState<ReportContent | null>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [confirmSubmit, setConfirmSubmit] = useState(false);
  const busy = useRef(false);

  const loadProjects = useCallback(async () => {
    const all: Project[] = [];
    for (let page = 0; ; page++) {
      const result = await request<Page<Project>>(`/projects?status=ACTIVE&page=${page}&size=100`);
      all.push(...result.content);
      if (result.last) return all;
    }
  }, [request]);

  useEffect(() => {
    let active = true;
    void Promise.all([request<WeeklyReport>(`/reports/${reportId}`), loadProjects()])
      .then(async ([loadedReport, loadedProjects]) => {
        const loadedReviews = loadedReport.status === "NEEDS_CORRECTION"
          ? await request<Review[]>(`/reports/${reportId}/reviews`)
          : [];
        if (!active) return;
        setReport(loadedReport);
        setContent(loadedReport.currentVersion);
        setProjects(loadedProjects);
        setReviews(loadedReviews);
      })
      .catch(caught => { if (active) setError(caught instanceof Error ? caught.message : "Could not load this report."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [loadProjects, reportId, request]);

  useEffect(() => {
    if (!dirty) return;
    const warnBeforeLeaving = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", warnBeforeLeaving);
    return () => window.removeEventListener("beforeunload", warnBeforeLeaving);
  }, [dirty]);

  const editable = report?.status === "DRAFT" || report?.status === "NEEDS_CORRECTION";
  const latestCorrection = useMemo(() => [...reviews]
    .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
    .find(review => review.action === "CHANGES_REQUESTED"), [reviews]);
  const totalMinutes = content?.timeEntries.reduce((sum, entry) => sum + entry.minutes, 0) ?? 0;

  function change(next: ReportContent) {
    setContent(next); setDirty(true); setNotice("");
  }

  async function save(): Promise<WeeklyReport | null> {
    if (!report || !content || !editable || busy.current) return report;
    const validationError = validateContent(content);
    if (validationError) { setError(validationError); return null; }
    busy.current = true; setSaving(true); setError(""); setNotice("");
    try {
      const updated = await request<WeeklyReport>(`/reports/${report.id}`, { method: "PUT", body: JSON.stringify(updatePayload(content, report.currentVersion.entityVersion)) });
      setReport(updated); setContent(updated.currentVersion); setDirty(false); setNotice("Draft saved.");
      return updated;
    } catch (caught) {
      setError(caught instanceof ApiError && caught.status === 409 ? `${caught.message} Your text is still here; reload only after copying any unsaved changes.` : caught instanceof Error ? caught.message : "Could not save this report.");
      return null;
    } finally { busy.current = false; setSaving(false); }
  }

  async function submit() {
    if (!content || !hasWorkInformation(content)) {
      setError("Add at least one task, blocker, achievement, or note before submitting.");
      setConfirmSubmit(false);
      return;
    }
    const saved = await save();
    if (!saved) return;
    busy.current = true; setSaving(true); setError("");
    try {
      const submitted = await request<WeeklyReport>(`/reports/${saved.id}/submit`, { method: "POST" });
      setReport(submitted); setContent(submitted.currentVersion); setConfirmSubmit(false); setNotice("Report submitted for manager review.");
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Could not submit this report."); }
    finally { busy.current = false; setSaving(false); }
  }

  if (user?.role !== "TEAM_MEMBER") return <p>Personal reports are available to team members.</p>;
  if (loading) return <p className="text-slate-500">Loading report…</p>;
  if (!report || !content) return <ErrorCard message={error || "Report not found."} />;

  return <div className="mx-auto max-w-5xl pb-20">
    <header className="mb-7 flex flex-col gap-5 border-b border-slate-200 pb-7 sm:flex-row sm:items-end sm:justify-between">
      <div><p className="text-sm font-semibold text-brand">{report.weekStart} – {report.weekEnd}</p><h1 className="mt-2 text-3xl font-semibold tracking-tight">Weekly report</h1><p className="mt-2 text-sm text-slate-500">Version {report.currentVersion.versionNumber} · {label(report.status)}</p></div>
      <div className="flex gap-3"><Link href="/reports/new" onClick={event => { if (dirty && !window.confirm("Leave without saving your changes?")) event.preventDefault(); }} className="report-secondary">Change week</Link>{editable && <button type="button" className="report-primary" onClick={() => void save()} disabled={saving || !dirty}>{saving ? "Saving…" : dirty ? "Save draft" : "Saved"}</button>}</div>
    </header>

    {latestCorrection?.comment && <aside className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 p-5"><p className="text-sm font-semibold text-amber-900">Manager requested changes</p><p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-amber-800">{latestCorrection.comment}</p></aside>}
    {!editable && <aside className="mb-6 rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-600">This report is {label(report.status).toLowerCase()} and is read-only.</aside>}
    {error && <p role="alert" className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</p>}
    {notice && <p role="status" className="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">{notice}</p>}

    <div className="space-y-6">
      <Section number="01" title="Tasks completed" description="What moved forward this week?">
        {content.completedTasks.map((task, index) => <CompletedRow key={index} task={task} projects={projects} disabled={!editable} onChange={task => change({ ...content, completedTasks: content.completedTasks.map((old, i) => i === index ? task : old) })} onRemove={() => change({ ...content, completedTasks: content.completedTasks.filter((_, i) => i !== index) })} />)}
        {editable && <AddButton onClick={() => change({ ...content, completedTasks: [...content.completedTasks, emptyCompleted()] })}>Add completed task</AddButton>}
      </Section>
      <Section number="02" title="Plans for next week" description="Make the next priorities explicit.">
        {content.plannedTasks.map((task, index) => <PlannedRow key={index} task={task} projects={projects} disabled={!editable} onChange={task => change({ ...content, plannedTasks: content.plannedTasks.map((old, i) => i === index ? task : old) })} onRemove={() => change({ ...content, plannedTasks: content.plannedTasks.filter((_, i) => i !== index) })} />)}
        {editable && <AddButton onClick={() => change({ ...content, plannedTasks: [...content.plannedTasks, emptyPlanned()] })}>Add planned task</AddButton>}
      </Section>
      <Section number="03" title="Blockers and challenges" description="Mark at most one item as the key blocker.">
        {content.blockers.map((item, index) => <TextFlagRow key={index} value={item.description} disabled={!editable} flagLabel="Key blocker" flagged={item.keyBlocker} secondaryLabel="Resolved" secondary={item.resolved} onChange={(description, flagged, secondary) => change({ ...content, blockers: content.blockers.map((old, i) => i === index ? { description, keyBlocker: flagged, resolved: secondary } : flagged ? { ...old, keyBlocker: false } : old) })} onRemove={() => change({ ...content, blockers: content.blockers.filter((_, i) => i !== index) })} />)}
        {editable && <AddButton onClick={() => change({ ...content, blockers: [...content.blockers, emptyBlocker()] })}>Add blocker</AddButton>}
      </Section>
      <Section number="04" title="Achievements and highlights" description="Mark at most one item as the key achievement.">
        {content.achievements.map((item, index) => <TextFlagRow key={index} value={item.description} disabled={!editable} flagLabel="Key achievement" flagged={item.keyAchievement} onChange={(description, flagged) => change({ ...content, achievements: content.achievements.map((old, i) => i === index ? { description, keyAchievement: flagged } : flagged ? { ...old, keyAchievement: false } : old) })} onRemove={() => change({ ...content, achievements: content.achievements.filter((_, i) => i !== index) })} />)}
        {editable && <AddButton onClick={() => change({ ...content, achievements: [...content.achievements, emptyAchievement()] })}>Add achievement</AddButton>}
      </Section>
      <Section number="05" title="Time by task type" description={`Optional · Total ${formatMinutes(totalMinutes)}`}>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{taskTypes.map(taskType => { const minutes = content.timeEntries.find(entry => entry.taskType === taskType)?.minutes ?? 0; return <label key={taskType} className="text-sm font-medium text-slate-700">{label(taskType)}<input className="report-input mt-2" type="number" min="0" disabled={!editable} value={minutes || ""} placeholder="0" onChange={event => { const value = Math.max(0, Number(event.target.value) || 0); change({ ...content, timeEntries: value === 0 ? content.timeEntries.filter(entry => entry.taskType !== taskType) : [...content.timeEntries.filter(entry => entry.taskType !== taskType), { taskType, minutes: value }] }); }} /></label>; })}</div>
      </Section>
      <Section number="06" title="Notes or links" description="Optional context that does not fit above."><textarea className="report-textarea" maxLength={5000} disabled={!editable} value={content.notes ?? ""} onChange={event => change({ ...content, notes: event.target.value || null })} /></Section>
    </div>

    {editable && <footer className="mt-7 flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-5 sm:flex-row sm:items-center sm:justify-between"><p className="text-sm text-slate-500">Submitting locks this version while your manager reviews it.</p><div className="flex gap-3">{confirmSubmit ? <><button className="report-secondary" type="button" onClick={() => setConfirmSubmit(false)} disabled={saving}>Cancel</button><button className="report-primary" type="button" onClick={() => void submit()} disabled={saving}>{saving ? "Submitting…" : "Confirm submission"}</button></> : <button className="report-primary" type="button" onClick={() => setConfirmSubmit(true)}>Submit for review</button>}</div></footer>}
  </div>;
}

function Section({ number, title, description, children }: { number: string; title: string; description: string; children: React.ReactNode }) { return <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6"><div className="mb-5 flex gap-4"><span className="text-xs font-bold text-brand">{number}</span><div><h2 className="font-semibold text-slate-900">{title}</h2><p className="mt-1 text-sm text-slate-500">{description}</p></div></div><div className="space-y-4">{children}</div></section>; }
function AddButton({ onClick, children }: { onClick(): void; children: React.ReactNode }) { return <button type="button" onClick={onClick} className="report-secondary text-sm">+ {children}</button>; }
function RemoveButton({ onClick, disabled }: { onClick(): void; disabled: boolean }) { return <button type="button" onClick={onClick} disabled={disabled} className="text-sm font-medium text-red-600 hover:underline">Remove</button>; }
function ProjectSelect({ value, projects, disabled, onChange }: { value: string | null; projects: Project[]; disabled: boolean; onChange(value: string | null): void }) { return <select className="report-select" disabled={disabled} value={value ?? ""} onChange={event => onChange(event.target.value || null)}><option value="">No project</option>{projects.map(project => <option key={project.id} value={project.id}>{project.name}</option>)}</select>; }
function CompletedRow({ task, projects, disabled, onChange, onRemove }: { task: CompletedTask; projects: Project[]; disabled: boolean; onChange(task: CompletedTask): void; onRemove(): void }) { return <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-4"><div className="grid gap-3 md:grid-cols-2"><label className="text-sm">Task name<input required maxLength={255} className="report-input mt-1" disabled={disabled} value={task.taskName} onChange={e => onChange({ ...task, taskName: e.target.value })} /></label><label className="text-sm">Project<ProjectSelect value={task.projectId} projects={projects} disabled={disabled} onChange={projectId => onChange({ ...task, projectId })} /></label><label className="text-sm">Priority<select className="report-select mt-1" disabled={disabled} value={task.priority} onChange={e => onChange({ ...task, priority: e.target.value as CompletedTask["priority"] })}>{priorities.map(value => <option key={value} value={value}>{label(value)}</option>)}</select></label><label className="text-sm">Status<select className="report-select mt-1" disabled={disabled} value={task.status} onChange={e => onChange({ ...task, status: e.target.value as CompletedTask["status"] })}>{taskStatuses.map(value => <option key={value} value={value}>{label(value)}</option>)}</select></label><NumberField label="Planned %" value={task.plannedPercentage} disabled={disabled} max={100} onChange={plannedPercentage => onChange({ ...task, plannedPercentage: plannedPercentage ?? 0 })} /><NumberField label="Actual %" value={task.actualPercentage} disabled={disabled} max={100} onChange={actualPercentage => onChange({ ...task, actualPercentage: actualPercentage ?? 0 })} /><NumberField label="Time planned (minutes)" value={task.plannedMinutes} disabled={disabled} onChange={plannedMinutes => onChange({ ...task, plannedMinutes })} /><NumberField label="Time spent (minutes)" value={task.spentMinutes} disabled={disabled} onChange={spentMinutes => onChange({ ...task, spentMinutes })} /></div><label className="mt-3 block text-sm">Description<textarea className="report-textarea mt-1" maxLength={2000} disabled={disabled} value={task.description ?? ""} onChange={e => onChange({ ...task, description: e.target.value || null })} /></label><label className="mt-3 block text-sm">Output or deliverable<textarea className="report-textarea mt-1" maxLength={4000} disabled={disabled} value={task.deliverable ?? ""} onChange={e => onChange({ ...task, deliverable: e.target.value || null })} /></label><div className="mt-3 text-right"><RemoveButton onClick={onRemove} disabled={disabled} /></div></div>; }
function PlannedRow({ task, projects, disabled, onChange, onRemove }: { task: PlannedTask; projects: Project[]; disabled: boolean; onChange(task: PlannedTask): void; onRemove(): void }) { return <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-4"><div className="grid gap-3 md:grid-cols-2"><label className="text-sm">Task name<input required maxLength={255} className="report-input mt-1" disabled={disabled} value={task.taskName} onChange={e => onChange({ ...task, taskName: e.target.value })} /></label><label className="text-sm">Project<ProjectSelect value={task.projectId} projects={projects} disabled={disabled} onChange={projectId => onChange({ ...task, projectId })} /></label><label className="text-sm">Priority<select className="report-select mt-1" disabled={disabled} value={task.priority} onChange={e => onChange({ ...task, priority: e.target.value as PlannedTask["priority"] })}>{priorities.map(value => <option key={value} value={value}>{label(value)}</option>)}</select></label><NumberField label="Estimated minutes" value={task.estimatedMinutes} disabled={disabled} onChange={estimatedMinutes => onChange({ ...task, estimatedMinutes })} /></div><label className="mt-3 block text-sm">Description<textarea className="report-textarea mt-1" maxLength={2000} disabled={disabled} value={task.description ?? ""} onChange={e => onChange({ ...task, description: e.target.value || null })} /></label><div className="mt-3 text-right"><RemoveButton onClick={onRemove} disabled={disabled} /></div></div>; }
function TextFlagRow({ value, disabled, flagLabel, flagged, secondaryLabel, secondary = false, onChange, onRemove }: { value: string; disabled: boolean; flagLabel: string; flagged: boolean; secondaryLabel?: string; secondary?: boolean; onChange(value: string, flagged: boolean, secondary: boolean): void; onRemove(): void }) { return <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-4"><textarea required maxLength={4000} className="report-textarea" disabled={disabled} value={value} onChange={e => onChange(e.target.value, flagged, secondary)} /><div className="mt-3 flex flex-wrap items-center gap-5"><label className="flex items-center gap-2 text-sm"><input type="checkbox" disabled={disabled} checked={flagged} onChange={e => onChange(value, e.target.checked, secondary)} />{flagLabel}</label>{secondaryLabel && <label className="flex items-center gap-2 text-sm"><input type="checkbox" disabled={disabled} checked={secondary} onChange={e => onChange(value, flagged, e.target.checked)} />{secondaryLabel}</label>}<span className="ml-auto"><RemoveButton onClick={onRemove} disabled={disabled} /></span></div></div>; }
function NumberField({ label: fieldLabel, value, disabled, max, onChange }: { label: string; value: number | null; disabled: boolean; max?: number; onChange(value: number | null): void }) { return <label className="text-sm">{fieldLabel}<input className="report-input mt-1" type="number" min="0" max={max} disabled={disabled} value={value ?? ""} onChange={e => onChange(e.target.value === "" ? null : Number(e.target.value))} /></label>; }
function ErrorCard({ message }: { message: string }) { return <div className="rounded-2xl border border-red-200 bg-red-50 p-6"><h1 className="font-semibold text-red-900">Could not open report</h1><p className="mt-2 text-sm text-red-700">{message}</p><Link href="/reports/new" className="mt-4 inline-block font-semibold text-brand">Choose another week</Link></div>; }
function formatMinutes(minutes: number) { return `${Math.floor(minutes / 60)}h ${minutes % 60}m`; }
function hasWorkInformation(content: ReportContent) { return Boolean(content.notes?.trim() || content.completedTasks.length || content.plannedTasks.length || content.blockers.length || content.achievements.length); }
function validateContent(content: ReportContent): string | null {
  if (content.completedTasks.some(task => !task.taskName.trim())) return "Every completed task needs a task name.";
  if (content.plannedTasks.some(task => !task.taskName.trim())) return "Every planned task needs a task name.";
  if (content.blockers.some(blocker => !blocker.description.trim())) return "Every blocker needs a description.";
  if (content.achievements.some(achievement => !achievement.description.trim())) return "Every achievement needs a description.";
  if (content.completedTasks.some(task => task.plannedPercentage < 0 || task.plannedPercentage > 100 || task.actualPercentage < 0 || task.actualPercentage > 100)) return "Task percentages must be between 0 and 100.";
  const minutes = [
    ...content.completedTasks.flatMap(task => [task.plannedMinutes, task.spentMinutes]),
    ...content.plannedTasks.map(task => task.estimatedMinutes),
    ...content.timeEntries.map(entry => entry.minutes),
  ];
  if (minutes.some(value => value !== null && (!Number.isInteger(value) || value < 0))) return "Time values must be whole, non-negative minutes.";
  return null;
}
