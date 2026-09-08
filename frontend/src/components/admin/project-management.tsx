"use client";

import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import Link from "next/link";

import { useAuth } from "@/components/auth/auth-provider";
import { ApiError } from "@/lib/api/client";
import { type Page, type Project, type ProjectMember, type UserSummary } from "@/lib/reports/model";

interface ProjectInput { name: string; description: string }
const emptyInput: ProjectInput = { name: "", description: "" };

export function ProjectManagement() {
  const { user, request } = useAuth();
  const [projects, setProjects] = useState<Project[]>([]);
  const [teamMembers, setTeamMembers] = useState<UserSummary[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [createInput, setCreateInput] = useState<ProjectInput>(emptyInput);
  const [editInput, setEditInput] = useState<ProjectInput>(emptyInput);
  const [assignUserId, setAssignUserId] = useState("");
  const [loading, setLoading] = useState(true);
  const [membersLoading, setMembersLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const selected = projects.find(project => project.id === selectedId) ?? null;
  const assignableMembers = useMemo(() => {
    const assignedIds = new Set(members.map(member => member.id));
    return teamMembers.filter(member => member.active && !assignedIds.has(member.id));
  }, [members, teamMembers]);

  const loadAllPages = useCallback(async <T,>(path: string): Promise<T[]> => {
    const items: T[] = [];
    for (let page = 0; ; page++) {
      const separator = path.includes("?") ? "&" : "?";
      const result = await request<Page<T>>(`${path}${separator}page=${page}&size=100`);
      items.push(...result.content);
      if (result.last) return items;
    }
  }, [request]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [loadedProjects, loadedMembers] = await Promise.all([
        loadAllPages<Project>("/projects?sort=name,asc"),
        loadAllPages<UserSummary>("/admin/users?role=TEAM_MEMBER&sort=firstName,asc"),
      ]);
      setProjects(loadedProjects);
      setTeamMembers(loadedMembers);
      setSelectedId(current => current && loadedProjects.some(item => item.id === current)
        ? current
        : loadedProjects[0]?.id ?? null);
    } catch (caught) {
      setError(message(caught, "Could not load projects."));
    } finally {
      setLoading(false);
    }
  }, [loadAllPages]);

  const loadMembers = useCallback(async (projectId: string) => {
    setMembersLoading(true);
    setError("");
    try {
      setMembers(await request<ProjectMember[]>(`/projects/${projectId}/members`));
    } catch (caught) {
      setMembers([]);
      setError(message(caught, "Could not load project members."));
    } finally {
      setMembersLoading(false);
    }
  }, [request]);

  useEffect(() => {
    if (user?.role !== "ADMIN") return;
    void Promise.resolve().then(load);
  }, [load, user?.role]);

  useEffect(() => {
    void Promise.resolve().then(() => {
      if (!selected) {
        setMembers([]);
        return;
      }
      setEditInput({ name: selected.name, description: selected.description ?? "" });
      setAssignUserId("");
      return loadMembers(selected.id);
    });
  }, [loadMembers, selected]);

  function beginAction() {
    setBusy(true);
    setError("");
    setNotice("");
  }

  async function createProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const name = createInput.name.trim();
    if (!name) { setError("Project name is required."); return; }
    beginAction();
    try {
      const created = await request<Project>("/admin/projects", {
        method: "POST",
        body: JSON.stringify({ name, description: createInput.description.trim() || null }),
      });
      setCreateInput(emptyInput);
      setNotice(`Created ${created.name}.`);
      await load();
      setSelectedId(created.id);
    } catch (caught) {
      setError(message(caught, "Could not create the project."));
    } finally { setBusy(false); }
  }

  async function saveProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;
    const name = editInput.name.trim();
    if (!name) { setError("Project name is required."); return; }
    beginAction();
    try {
      const updated = await request<Project>(`/admin/projects/${selected.id}`, {
        method: "PUT",
        body: JSON.stringify({ name, description: editInput.description.trim() || null }),
      });
      setProjects(current => current.map(project => project.id === updated.id ? updated : project));
      setNotice(`Saved ${updated.name}.`);
    } catch (caught) {
      setError(message(caught, "Could not update the project."));
    } finally { setBusy(false); }
  }

  async function changeStatus() {
    if (!selected) return;
    const nextStatus = selected.status === "ACTIVE" ? "ARCHIVED" : "ACTIVE";
    if (nextStatus === "ARCHIVED" && !window.confirm(`Archive ${selected.name}? It will disappear from members' editable report choices.`)) return;
    beginAction();
    try {
      const updated = await request<Project>(`/admin/projects/${selected.id}`, {
        method: "PATCH",
        body: JSON.stringify({ status: nextStatus }),
      });
      setProjects(current => current.map(project => project.id === updated.id ? updated : project));
      setNotice(`${updated.name} is now ${updated.status.toLowerCase()}.`);
    } catch (caught) {
      setError(message(caught, "Could not change project status."));
    } finally { setBusy(false); }
  }

  async function deleteProject() {
    if (!selected || !window.confirm(`Permanently delete ${selected.name}? Referenced projects must be archived instead.`)) return;
    beginAction();
    try {
      await request(`/admin/projects/${selected.id}`, { method: "DELETE" });
      setNotice(`Deleted ${selected.name}.`);
      setProjects(current => current.filter(project => project.id !== selected.id));
      setSelectedId(null);
    } catch (caught) {
      setError(message(caught, "Could not delete the project."));
    } finally { setBusy(false); }
  }

  async function assignMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || !assignUserId) return;
    beginAction();
    try {
      await request(`/admin/projects/${selected.id}/members`, {
        method: "POST",
        body: JSON.stringify({ userId: assignUserId }),
      });
      setAssignUserId("");
      setNotice("Team member assigned.");
      await loadMembers(selected.id);
    } catch (caught) {
      setError(message(caught, "Could not assign the team member."));
    } finally { setBusy(false); }
  }

  async function removeMember(member: ProjectMember) {
    if (!selected) return;
    beginAction();
    try {
      await request(`/admin/projects/${selected.id}/members/${member.id}`, { method: "DELETE" });
      setNotice(`${member.firstName} ${member.lastName} removed from ${selected.name}.`);
      await loadMembers(selected.id);
    } catch (caught) {
      setError(message(caught, "Could not remove the team member."));
    } finally { setBusy(false); }
  }

  if (user?.role !== "ADMIN") return <AccessDenied />;

  return <section className="mx-auto max-w-7xl">
    <header className="mb-7 border-b border-slate-200 pb-7">
      <p className="text-sm font-semibold text-brand">Administration</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight">Projects and assignments</h1>
      <p className="mt-2 max-w-3xl text-slate-500">Create the project catalogue and control which active projects each team member can use in weekly reports.</p>
    </header>

    {error && <p role="alert" className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</p>}
    {notice && <p role="status" className="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">{notice}</p>}

    <div className="grid gap-6 lg:grid-cols-[minmax(260px,0.8fr)_minmax(0,1.7fr)]">
      <div className="space-y-6">
        <form onSubmit={createProject} className="rounded-2xl border border-slate-200 bg-white p-5">
          <h2 className="font-semibold text-slate-900">Create project</h2>
          <ProjectFields input={createInput} disabled={busy} onChange={setCreateInput} />
          <button type="submit" className="report-primary mt-4" disabled={busy}>Create project</button>
        </form>

        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <div className="border-b border-slate-100 p-4"><h2 className="font-semibold text-slate-900">Project catalogue</h2><p className="mt-1 text-xs text-slate-400">{projects.length} projects</p></div>
          {loading ? <div className="p-5 text-sm text-slate-500">Loading projects…</div> : projects.length ? <ul className="max-h-[620px] divide-y divide-slate-100 overflow-y-auto">{projects.map(project => <li key={project.id}><button type="button" onClick={() => setSelectedId(project.id)} className={`w-full p-4 text-left transition ${selectedId === project.id ? "bg-brand-soft" : "hover:bg-slate-50"}`}><span className="flex items-center justify-between gap-3"><span className="font-medium text-slate-800">{project.name}</span><Status status={project.status} /></span><span className="mt-1 block truncate text-xs text-slate-400">{project.description || "No description"}</span></button></li>)}</ul> : <p className="p-5 text-sm text-slate-500">No projects yet.</p>}
        </section>
      </div>

      {selected ? <div className="space-y-6">
        <form onSubmit={saveProject} className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">Selected project</p><h2 className="mt-1 text-xl font-semibold text-slate-900">{selected.name}</h2></div><Status status={selected.status} /></div>
          <ProjectFields input={editInput} disabled={busy || selected.status === "ARCHIVED"} onChange={setEditInput} />
          <div className="mt-5 flex flex-wrap gap-3">
            <button type="submit" className="report-primary" disabled={busy || selected.status === "ARCHIVED"}>Save details</button>
            <button type="button" className="report-secondary" disabled={busy} onClick={() => void changeStatus()}>{selected.status === "ACTIVE" ? "Archive" : "Reactivate"}</button>
            <button type="button" className="rounded-xl border border-red-200 bg-white px-4 py-2.5 font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-50" disabled={busy} onClick={() => void deleteProject()}>Delete permanently</button>
          </div>
          {selected.status === "ARCHIVED" && <p className="mt-3 text-sm text-slate-500">Reactivate this project before editing its details or assigning new members.</p>}
        </form>

        <section className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
          <h2 className="font-semibold text-slate-900">Team member access</h2>
          <p className="mt-1 text-sm text-slate-500">Only assigned members can discover or attach this project to an editable report.</p>
          <form onSubmit={assignMember} className="mt-4 flex flex-col gap-3 sm:flex-row">
            <label className="flex-1 text-sm font-medium text-slate-700">Add team member
              <select className="report-select mt-1" value={assignUserId} disabled={busy || selected.status === "ARCHIVED" || !assignableMembers.length} onChange={event => setAssignUserId(event.target.value)}>
                <option value="">{assignableMembers.length ? "Choose a member" : "All active members assigned"}</option>
                {assignableMembers.map(member => <option key={member.id} value={member.id}>{member.firstName} {member.lastName} · {member.email}</option>)}
              </select>
            </label>
            <button type="submit" className="report-primary self-end" disabled={busy || !assignUserId || selected.status === "ARCHIVED"}>Assign</button>
          </form>

          {membersLoading ? <p className="mt-5 text-sm text-slate-500">Loading assignments…</p> : members.length ? <ul className="mt-5 divide-y divide-slate-100 border-t border-slate-100">{members.map(member => <li key={member.id} className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between"><div><p className="font-medium text-slate-800">{member.firstName} {member.lastName}</p><p className="mt-1 text-sm text-slate-500">{member.email}</p></div><button type="button" className="text-left text-sm font-semibold text-red-600 hover:underline disabled:opacity-50" disabled={busy} onClick={() => void removeMember(member)}>Remove access</button></li>)}</ul> : <p className="mt-5 rounded-xl bg-slate-50 p-4 text-sm text-slate-500">No team members are assigned.</p>}
        </section>
      </div> : <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-slate-500">Create or select a project to manage it.</div>}
    </div>
  </section>;
}

function ProjectFields({ input, disabled, onChange }: { input: ProjectInput; disabled: boolean; onChange(value: ProjectInput): void }) {
  return <div className="mt-4 space-y-4"><label className="block text-sm font-medium text-slate-700">Project name<input className="report-input mt-1" required maxLength={150} disabled={disabled} value={input.name} onChange={event => onChange({ ...input, name: event.target.value })} /></label><label className="block text-sm font-medium text-slate-700">Description<textarea className="report-textarea mt-1" maxLength={2000} disabled={disabled} value={input.description} onChange={event => onChange({ ...input, description: event.target.value })} /></label></div>;
}
function Status({ status }: { status: Project["status"] }) { return <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${status === "ACTIVE" ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>{status === "ACTIVE" ? "Active" : "Archived"}</span>; }
function message(caught: unknown, fallback: string) { return caught instanceof ApiError || caught instanceof Error ? caught.message : fallback; }
function AccessDenied() { return <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6"><h1 className="font-semibold text-amber-950">Administrator access required</h1><p className="mt-2 text-sm text-amber-800">Only administrators can manage projects and assignments.</p><Link href="/dashboard" className="mt-4 inline-block font-semibold text-brand">Return to dashboard</Link></div>; }
