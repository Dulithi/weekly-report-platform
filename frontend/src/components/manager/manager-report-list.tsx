"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import {
  label,
  type ManagerReportSummary,
  type Page,
  type Project,
  type ReportMember,
  type ReportStatus,
} from "@/lib/reports/model";
import {
  formatDateRange,
  formatDateTime,
  StatusBadge,
} from "@/components/reports/report-history";

const PAGE_SIZE = 20;
const reportStatuses: ReportStatus[] = [
  "DRAFT",
  "SUBMITTED",
  "NEEDS_CORRECTION",
  "APPROVED",
];

interface Filters {
  memberId: string;
  projectId: string;
  from: string;
  to: string;
  status: "" | ReportStatus;
}

const emptyFilters: Filters = {
  memberId: "",
  projectId: "",
  from: "",
  to: "",
  status: "",
};

export function ManagerReportList() {
  const { user, request } = useAuth();
  const [draftFilters, setDraftFilters] = useState<Filters>(emptyFilters);
  const [filters, setFilters] = useState<Filters>(emptyFilters);
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<ManagerReportSummary> | null>(null);
  const [members, setMembers] = useState<ReportMember[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterOptionsLoading, setFilterOptionsLoading] = useState(true);
  const [error, setError] = useState("");
  const [filterError, setFilterError] = useState("");
  const loadGeneration = useRef(0);

  const allowed = user?.role === "MANAGER" || user?.role === "ADMIN";

  const loadReports = useCallback(async () => {
    const generation = ++loadGeneration.current;
    const query = new URLSearchParams({
      page: String(pageNumber),
      size: String(PAGE_SIZE),
      sort: "weekStart,desc",
    });
    for (const [key, value] of Object.entries(filters)) {
      if (value) query.set(key, value);
    }

    setLoading(true);
    setError("");
    try {
      const loaded = await request<Page<ManagerReportSummary>>(
        `/manager/reports?${query.toString()}`,
      );
      if (generation === loadGeneration.current) setPage(loaded);
    } catch (caught) {
      if (generation === loadGeneration.current) {
        setError(caught instanceof Error ? caught.message : "Could not load team reports.");
      }
    } finally {
      if (generation === loadGeneration.current) setLoading(false);
    }
  }, [filters, pageNumber, request]);

  useEffect(() => {
    if (!allowed) return;
    void Promise.resolve().then(loadReports);
    return () => { loadGeneration.current += 1; };
  }, [allowed, loadReports]);

  useEffect(() => {
    if (!allowed) return;
    let active = true;
    void Promise.all([
      request<ReportMember[]>("/manager/team-members"),
      request<Page<Project>>("/projects?page=0&size=100&sort=name,asc"),
    ]).then(([loadedMembers, loadedProjects]) => {
      if (!active) return;
      setMembers(loadedMembers);
      setProjects(loadedProjects.content);
    }).catch(() => {
      // The report list remains useful if one of the optional filter sources fails.
    }).finally(() => {
      if (active) setFilterOptionsLoading(false);
    });
    return () => { active = false; };
  }, [allowed, request]);

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (draftFilters.from && draftFilters.to && draftFilters.from > draftFilters.to) {
      setFilterError("The start date must be on or before the end date.");
      return;
    }
    setFilterError("");
    setPageNumber(0);
    setFilters({ ...draftFilters });
  }

  function clearFilters() {
    setDraftFilters(emptyFilters);
    setFilters(emptyFilters);
    setFilterError("");
    setPageNumber(0);
  }

  if (!allowed) {
    return <AccessDenied />;
  }

  return <section className="mx-auto max-w-6xl">
    <header className="mb-7 border-b border-slate-200 pb-7">
      <p className="text-sm font-semibold text-brand">Manager workspace</p>
      <h1 className="mt-2 text-3xl font-semibold tracking-tight">Team reports</h1>
      <p className="mt-2 max-w-2xl text-slate-500">
        Track every team member&apos;s weekly report and open submitted work for review.
        Draft contents remain private until the member submits them.
      </p>
    </header>

    <form onSubmit={applyFilters} className="mb-6 rounded-2xl border border-slate-200 bg-white p-5">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <FilterSelect
          label="Team member"
          value={draftFilters.memberId}
          disabled={filterOptionsLoading}
          onChange={memberId => setDraftFilters(current => ({ ...current, memberId }))}
        >
          <option value="">All members</option>
          {members.map(member => <option key={member.id} value={member.id}>
            {member.firstName} {member.lastName}{member.active ? "" : " (inactive)"}
          </option>)}
        </FilterSelect>
        <FilterSelect
          label="Project"
          value={draftFilters.projectId}
          disabled={filterOptionsLoading}
          onChange={projectId => setDraftFilters(current => ({ ...current, projectId }))}
        >
          <option value="">All projects</option>
          {projects.map(project => <option key={project.id} value={project.id}>
            {project.name}{project.status === "ARCHIVED" ? " (archived)" : ""}
          </option>)}
        </FilterSelect>
        <FilterSelect
          label="Status"
          value={draftFilters.status}
          onChange={status => setDraftFilters(current => ({
            ...current,
            status: status as Filters["status"],
          }))}
        >
          <option value="">All statuses</option>
          {reportStatuses.map(status => <option key={status} value={status}>{label(status)}</option>)}
        </FilterSelect>
        <DateFilter
          label="Overlaps from"
          value={draftFilters.from}
          onChange={from => setDraftFilters(current => ({ ...current, from }))}
        />
        <DateFilter
          label="Overlaps to"
          value={draftFilters.to}
          onChange={to => setDraftFilters(current => ({ ...current, to }))}
        />
      </div>
      {filterError && <p role="alert" className="mt-3 text-sm font-medium text-red-700">{filterError}</p>}
      <div className="mt-5 flex flex-wrap gap-3">
        <button type="submit" className="report-primary">Apply filters</button>
        <button type="button" className="report-secondary" onClick={clearFilters}>Clear</button>
      </div>
    </form>

    {error && <div role="alert" className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
      <p>{error}</p>
      <button type="button" onClick={() => void loadReports()} className="mt-2 font-semibold underline">Try again</button>
    </div>}

    {loading ? <ReportListSkeleton /> : page?.content.length ? <>
      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <ul className="divide-y divide-slate-100">
          {page.content.map(report => <ManagerReportRow key={report.id} report={report} />)}
        </ul>
      </div>
      <nav className="mt-5 flex flex-col items-center justify-between gap-3 sm:flex-row" aria-label="Team report pages">
        <button type="button" className="report-secondary" disabled={page.first} onClick={() => setPageNumber(value => Math.max(0, value - 1))}>← Previous</button>
        <p className="text-sm text-slate-500">Page {page.number + 1} of {page.totalPages} · {page.totalElements} reports</p>
        <button type="button" className="report-secondary" disabled={page.last} onClick={() => setPageNumber(value => value + 1)}>Next →</button>
      </nav>
    </> : !error && <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center">
      <h2 className="text-xl font-semibold text-slate-900">No matching reports</h2>
      <p className="mt-2 text-slate-500">Adjust the filters or wait for team members to start their reports.</p>
    </div>}
  </section>;
}

function ManagerReportRow({ report }: { report: ManagerReportSummary }) {
  const canOpen = report.status !== "DRAFT";
  const content = <>
    <div>
      <div className="flex flex-wrap items-center gap-3">
        <h2 className="font-semibold text-slate-900">{report.member.firstName} {report.member.lastName}</h2>
        <StatusBadge status={report.status} />
        {!report.member.active && <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500">Inactive member</span>}
      </div>
      <p className="mt-1 text-sm text-slate-500">{report.member.email}</p>
      <p className="mt-2 text-sm text-slate-600">{formatDateRange(report.weekStart, report.weekEnd)}</p>
      <p className="mt-1 text-xs text-slate-400">
        {report.submittedAt ? `Submitted ${formatDateTime(report.submittedAt)}` : `Updated ${formatDateTime(report.updatedAt)}`}
      </p>
    </div>
    <span className={`text-sm font-semibold ${canOpen ? "text-brand group-hover:text-brand-strong" : "text-slate-400"}`}>
      {canOpen ? "Open review →" : "Draft content is private"}
    </span>
  </>;

  return <li>
    {canOpen
      ? <Link href={`/manager/reports/${report.id}`} className="group grid gap-4 p-5 transition hover:bg-slate-50 sm:grid-cols-[1fr_auto] sm:items-center sm:px-6">{content}</Link>
      : <div className="grid gap-4 p-5 sm:grid-cols-[1fr_auto] sm:items-center sm:px-6">{content}</div>}
  </li>;
}

function FilterSelect({ label: name, value, disabled = false, onChange, children }: {
  label: string; value: string; disabled?: boolean; onChange(value: string): void; children: React.ReactNode;
}) {
  return <label className="text-sm font-medium text-slate-700">{name}
    <select className="report-select mt-1" value={value} disabled={disabled} onChange={event => onChange(event.target.value)}>{children}</select>
  </label>;
}

function DateFilter({ label: name, value, onChange }: { label: string; value: string; onChange(value: string): void }) {
  return <label className="text-sm font-medium text-slate-700">{name}
    <input type="date" className="report-input mt-1" value={value} onChange={event => onChange(event.target.value)} />
  </label>;
}

function ReportListSkeleton() {
  return <div className="space-y-3" aria-label="Loading team reports">
    {[0, 1, 2, 3].map(item => <div key={item} className="h-32 animate-pulse rounded-2xl border border-slate-200 bg-white" />)}
  </div>;
}

function AccessDenied() {
  return <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6">
    <h1 className="font-semibold text-amber-950">Manager access required</h1>
    <p className="mt-2 text-sm text-amber-800">Team report review is available to managers and administrators.</p>
    <Link href="/dashboard" className="mt-4 inline-block font-semibold text-brand">Return to dashboard</Link>
  </div>;
}
