"use client";

import { useAuth } from "@/components/auth/auth-provider";
import type { UserRole } from "@/lib/api/types";

const workspaceContent: Record<UserRole, Array<{ eyebrow: string; title: string; description: string }>> = {
  TEAM_MEMBER: [
    { eyebrow: "Report", title: "Capture your week", description: "Record completed work, next steps, achievements, and blockers in one structured report." },
    { eyebrow: "History", title: "Follow every version", description: "Return to earlier reports and see how feedback shaped each revision." },
    { eyebrow: "Focus", title: "Keep priorities visible", description: "Connect time and progress to the projects that matter to your team." },
  ],
  MANAGER: [
    { eyebrow: "Reviews", title: "Move reports forward", description: "Review submitted work, request a correction with context, or approve it." },
    { eyebrow: "Team", title: "See who needs support", description: "Track submissions across the whole team without losing individual detail." },
    { eyebrow: "Insights", title: "Spot patterns early", description: "Use weekly signals to understand workload, blockers, and project momentum." },
  ],
  ADMIN: [
    { eyebrow: "People", title: "Manage team access", description: "Invite people, maintain roles, and preserve historical ownership safely." },
    { eyebrow: "Projects", title: "Keep work organized", description: "Maintain the active project catalogue and team memberships." },
    { eyebrow: "Governance", title: "Protect continuity", description: "Manage access with role boundaries and an auditable activity trail." },
  ],
};

export function DashboardWelcome() {
  const { user } = useAuth();
  if (!user) return null;

  return (
    <div className="animate-soft-in">
      <div className="flex flex-col justify-between gap-5 border-b border-slate-200 pb-8 sm:flex-row sm:items-end">
        <div>
          <p className="mb-2 text-sm font-semibold text-brand">Your workspace</p>
          <h1 className="text-3xl font-semibold tracking-[-0.04em] text-slate-950 sm:text-4xl">
            Welcome, {user.firstName}.
          </h1>
          <p className="mt-3 max-w-2xl leading-7 text-slate-500">
            Here is where your weekly reporting work comes together.
          </p>
        </div>
        <div className="w-fit rounded-full border border-emerald-200 bg-emerald-50 px-3.5 py-1.5 text-xs font-semibold text-emerald-700">
          Session active
        </div>
      </div>

      <section className="py-9" aria-labelledby="workspace-heading">
        <div className="mb-6 flex items-center justify-between">
          <h2 id="workspace-heading" className="text-lg font-semibold tracking-tight text-slate-900">What you can do</h2>
          <span className="text-xs font-medium uppercase tracking-[0.12em] text-slate-400">Based on your role</span>
        </div>
        <div className="grid gap-5 md:grid-cols-3">
          {workspaceContent[user.role].map((item, index) => (
            <article key={item.title} className="group rounded-2xl border border-slate-200 bg-white p-6 shadow-[0_1px_2px_rgba(15,23,42,0.03)] transition hover:-translate-y-0.5 hover:border-emerald-200 hover:shadow-md">
              <div className="mb-8 flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-[0.13em] text-brand">{item.eyebrow}</span>
                <span className="grid size-8 place-items-center rounded-full bg-stone-100 text-xs font-semibold text-slate-400 group-hover:bg-brand-soft group-hover:text-brand">0{index + 1}</span>
              </div>
              <h3 className="text-xl font-semibold tracking-[-0.025em] text-slate-900">{item.title}</h3>
              <p className="mt-3 text-sm leading-6 text-slate-500">{item.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="rounded-2xl bg-brand-strong px-6 py-7 text-white sm:flex sm:items-center sm:justify-between sm:px-8">
        <div>
          <p className="text-sm font-semibold text-emerald-200">A clear weekly rhythm</p>
          <h2 className="mt-2 text-2xl font-semibold tracking-[-0.03em]">Capture. Review. Improve.</h2>
        </div>
        <p className="mt-4 max-w-lg text-sm leading-6 text-emerald-50/70 sm:mt-0 sm:text-right">
          Your role controls every action while shared context keeps the whole team aligned.
        </p>
      </section>
    </div>
  );
}
