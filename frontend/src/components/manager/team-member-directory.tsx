"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import type { ReportMember } from "@/lib/reports/model";

export function TeamMemberDirectory() {
  const { user, request } = useAuth();
  const [members, setMembers] = useState<ReportMember[]>([]);
  const [search, setSearch] = useState("");
  const [stateFilter, setStateFilter] = useState<"" | "active" | "inactive">("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const allowed = user?.role === "MANAGER" || user?.role === "ADMIN";

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setMembers(await request<ReportMember[]>("/manager/team-members"));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not load the team directory.");
    } finally {
      setLoading(false);
    }
  }, [request]);

  useEffect(() => {
    if (allowed) void Promise.resolve().then(load);
  }, [allowed, load]);

  const visibleMembers = useMemo(() => {
    const term = search.trim().toLowerCase();
    return members.filter(member => {
      const identity = [member.firstName, member.lastName, member.email].join(" ").toLowerCase();
      return (!term || identity.includes(term))
        && (!stateFilter || member.active === (stateFilter === "active"));
    });
  }, [members, search, stateFilter]);

  if (!allowed) return <AccessDenied />;

  return <section className="mx-auto max-w-6xl">
    <header className="mb-7 border-b border-slate-200 pb-7">
      <p className="text-sm font-semibold text-brand">Manager workspace</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight">Team members</h1>
      <p className="mt-2 max-w-2xl text-slate-500">Open a member profile to see reporting totals and weekly status history. Draft contents remain private.</p>
    </header>

    <div className="mb-6 grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 sm:grid-cols-[minmax(0,1fr)_180px_auto] sm:items-end">
      <label className="text-sm font-medium text-slate-700">Search
        <input type="search" className="report-input mt-1" value={search} onChange={event => setSearch(event.target.value)} placeholder="Name or email" />
      </label>
      <label className="text-sm font-medium text-slate-700">Account state
        <select className="report-select mt-1" value={stateFilter} onChange={event => setStateFilter(event.target.value as typeof stateFilter)}>
          <option value="">All members</option>
          <option value="active">Active</option>
          <option value="inactive">Inactive</option>
        </select>
      </label>
      <p className="pb-3 text-sm text-slate-400">{visibleMembers.length} of {members.length}</p>
    </div>

    {error && <div role="alert" className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700"><p>{error}</p><button type="button" onClick={() => void load()} className="mt-2 font-semibold underline">Try again</button></div>}

    {loading ? <DirectorySkeleton /> : visibleMembers.length ? <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {visibleMembers.map(member => <li key={member.id}>
        <Link href={`/manager/team-members/${member.id}`} className="group flex h-full items-start gap-4 rounded-2xl border border-slate-200 bg-white p-5 transition hover:-translate-y-0.5 hover:border-brand/30 hover:shadow-sm">
          <span aria-hidden="true" className="grid size-12 shrink-0 place-items-center rounded-xl bg-brand-soft font-bold text-brand-strong">{initials(member)}</span>
          <span className="min-w-0 flex-1">
            <span className="flex flex-wrap items-center gap-2"><span className="font-semibold text-slate-900">{member.firstName} {member.lastName}</span><AccountState active={member.active} /></span>
            <span className="mt-1 block truncate text-sm text-slate-500">{member.email}</span>
            <span className="mt-4 block text-sm font-semibold text-brand group-hover:text-brand-strong">View profile →</span>
          </span>
        </Link>
      </li>)}
    </ul> : !error && <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center"><h2 className="text-xl font-semibold text-slate-900">{members.length === 0 ? "No assigned team members" : "No matching team members"}</h2><p className="mt-2 text-slate-500">{members.length === 0 ? "An administrator must assign team members to you before their profiles appear here." : "Adjust the search or account-state filter."}</p></div>}
  </section>;
}

function initials(member: ReportMember) {
  return `${member.firstName.charAt(0)}${member.lastName.charAt(0)}`.toUpperCase();
}

function AccountState({ active }: { active: boolean }) {
  return <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>{active ? "Active" : "Inactive"}</span>;
}

function DirectorySkeleton() {
  return <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-label="Loading team members">{[0, 1, 2, 3, 4, 5].map(item => <div key={item} className="h-32 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}</div>;
}

function AccessDenied() {
  return <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6"><h1 className="font-semibold text-amber-950">Manager access required</h1><p className="mt-2 text-sm text-amber-800">Team member profiles are available to managers and administrators.</p><Link href="/dashboard" className="mt-4 inline-block font-semibold text-brand">Return to dashboard</Link></div>;
}
