"use client";
import { useState, useRef, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth/auth-provider";
import { ApiError } from "@/lib/api/client";
import { isMonday, weekEnd, type Page, type WeeklyReport, type WeeklyReportSummary } from "@/lib/reports/model";
import { WeekPicker } from "@/components/ui/week-picker";

export function NewReport() {
  const { user, request } = useAuth();
  const router = useRouter();
  const [date, setDate] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const busy = useRef(false);
  if (user?.role !== "TEAM_MEMBER") return <p>Personal reports are available to team members.</p>;
  async function create(event: FormEvent) {
    event.preventDefault();
    if (busy.current) return;
    if (!isMonday(date)) { setError("Choose a Monday as the start of your reporting week."); return; }
    busy.current = true; setPending(true); setError("");
    try {
      let report: WeeklyReport;
      try {
        report = await request<WeeklyReport>("/reports", { method: "POST", body: JSON.stringify({ weekStart: date }) });
      } catch (caught) {
        if (!(caught instanceof ApiError) || caught.status !== 409) throw caught;
        // The API has no week filter; page through the owner's history to reopen
        // an existing report instead of leaving the user at a duplicate-week error.
        for (let page = 0; ; page++) {
          const history = await request<Page<WeeklyReportSummary>>(`/reports/me?page=${page}&size=100`);
          const existing = history.content.find(r => r.weekStart === date);
          if (existing) { router.push(`/reports/${existing.id}/edit`); return; }
          if (history.last) throw caught;
        }
      }
      router.push(`/reports/${report.id}/edit`);
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Could not open the report."); }
    finally { busy.current = false; setPending(false); }
  }
  return <section className="max-w-2xl">
    <p className="text-sm font-semibold text-brand">Personal reporting</p>
    <h1 className="mt-2 text-3xl font-semibold tracking-tight">Your weekly report</h1>
    <p className="my-5 text-slate-600">Choose a week to create a private draft or reopen your existing report.</p>
    <form onSubmit={create} className="space-y-5 rounded-2xl border border-slate-200 bg-white p-6">
      <WeekPicker label="Week starting" value={date} onChange={setDate} disabled={pending} />
      {isMonday(date) && <p className="text-sm text-slate-600">Reporting period: {date} – {weekEnd(date)}</p>}
      {error && <p role="alert" className="text-red-700">{error}</p>}
      <button className="report-primary" disabled={pending}>{pending ? "Opening…" : "Open report"}</button>
    </form>
  </section>;
}
