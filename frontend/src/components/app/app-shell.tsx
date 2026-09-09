"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";

import { BrandMark } from "@/components/brand-mark";
import { useAuth } from "@/components/auth/auth-provider";
import type { UserRole } from "@/lib/api/types";

const roleLabels: Record<UserRole, string> = {
  TEAM_MEMBER: "Team member",
  MANAGER: "Manager",
  ADMIN: "Administrator",
};

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  if (!user) {
    return null;
  }

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logout();
    } catch {
      // AuthProvider still clears the in-memory session. A later sign-in safely
      // replaces any refresh cookie if the server could not be reached.
    } finally {
      setLoggingOut(false);
      router.replace("/login");
    }
  }

  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();

  return (
    <div className="min-h-screen bg-stone-50 lg:grid lg:grid-cols-[272px_minmax(0,1fr)]">
      <a href="#main-content" className="sr-only z-50 rounded bg-white px-4 py-2 focus:not-sr-only focus:fixed focus:left-4 focus:top-4">
        Skip to content
      </a>

      {menuOpen && (
        <button
          className="fixed inset-0 z-30 bg-slate-950/35 backdrop-blur-[2px] lg:hidden"
          aria-label="Close navigation"
          onClick={() => setMenuOpen(false)}
        />
      )}

      <aside className={`fixed inset-y-0 left-0 z-40 flex w-[272px] flex-col border-r border-slate-200 bg-white px-5 py-6 transition-transform lg:sticky lg:top-0 lg:h-screen lg:translate-x-0 ${menuOpen ? "translate-x-0" : "-translate-x-full"}`}>
        <div className="px-2"><BrandMark /></div>

        <nav className="mt-12 flex-1" aria-label="Main navigation">
          <p className="px-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-slate-400">Workspace</p>
          <div className="mt-3 space-y-1">
            <NavLink href="/dashboard" active={pathname === "/dashboard"} onNavigate={() => setMenuOpen(false)}>
              <HomeIcon /> Dashboard
            </NavLink>
            {user.role === "TEAM_MEMBER" && (
              <NavLink href="/reports" active={pathname.startsWith("/reports")} onNavigate={() => setMenuOpen(false)}>
                <ReportIcon /> My reports
              </NavLink>
            )}
            {(user.role === "MANAGER" || user.role === "ADMIN") && (
              <>
                <NavLink href="/manager/reports" active={pathname.startsWith("/manager/reports")} onNavigate={() => setMenuOpen(false)}>
                  <ReportIcon /> Team reports
                </NavLink>
                <NavLink href="/manager/team-members" active={pathname.startsWith("/manager/team-members")} onNavigate={() => setMenuOpen(false)}>
                  <TeamIcon /> Team members
                </NavLink>
                <NavLink href="/manager/comparisons" active={pathname.startsWith("/manager/comparisons")} onNavigate={() => setMenuOpen(false)}>
                  <CompareIcon /> Compare sections
                </NavLink>
                <NavLink href="/manager/assistant" active={pathname.startsWith("/manager/assistant")} onNavigate={() => setMenuOpen(false)}>
                  <AssistantIcon /> Report assistant
                </NavLink>
              </>
            )}
            {user.role === "ADMIN" && (
              <>
                <NavLink href="/admin/projects" active={pathname.startsWith("/admin/projects")} onNavigate={() => setMenuOpen(false)}>
                  <ProjectIcon /> Projects
                </NavLink>
                <NavLink href="/admin/users" active={pathname.startsWith("/admin/users")} onNavigate={() => setMenuOpen(false)}>
                  <UsersIcon /> People
                </NavLink>
              </>
            )}
          </div>
        </nav>

        <div className="border-t border-slate-100 pt-5">
          <div className="flex items-center gap-3 rounded-xl px-2 py-2">
            <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-brand-soft text-sm font-bold text-brand-strong" aria-hidden="true">{initials}</span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-slate-800">{user.firstName} {user.lastName}</p>
              <p className="truncate text-xs text-slate-400">{roleLabels[user.role]}</p>
            </div>
          </div>
          <button
            onClick={() => void handleLogout()}
            disabled={loggingOut}
            className="mt-2 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium text-slate-500 transition hover:bg-slate-50 hover:text-slate-800 focus-visible:outline-2 focus-visible:outline-brand disabled:opacity-50"
          >
            <LogoutIcon /> {loggingOut ? "Signing out…" : "Sign out"}
          </button>
        </div>
      </aside>

      <div className="min-w-0">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200/80 bg-stone-50/90 px-5 backdrop-blur-lg sm:px-8 lg:hidden">
          <button
            onClick={() => setMenuOpen(true)}
            aria-label="Open navigation"
            aria-expanded={menuOpen}
            className="grid size-10 place-items-center rounded-xl border border-slate-200 bg-white text-slate-700 shadow-sm"
          >
            <MenuIcon />
          </button>
          <p className="text-sm font-semibold text-slate-700">Weekly</p>
          <span className="grid size-9 place-items-center rounded-xl bg-brand-soft text-xs font-bold text-brand-strong" aria-hidden="true">{initials}</span>
        </header>
        <main id="main-content" className="mx-auto w-full max-w-[1440px] px-5 py-7 sm:px-8 sm:py-10 xl:px-12">
          {children}
        </main>
      </div>
    </div>
  );
}

function NavLink({ href, active, onNavigate, children }: { href: string; active: boolean; onNavigate(): void; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      onNavigate={onNavigate}
      aria-current={active ? "page" : undefined}
      className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${active ? "bg-brand-soft text-brand-strong" : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"}`}
    >
      {children}
    </Link>
  );
}

function HomeIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="m4 10 8-6 8 6v9a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1v-9Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" /></svg>;
}

function ReportIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="M7 3h7l4 4v14H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" /><path d="M14 3v5h5M9 13h6M9 17h4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>;
}

function ProjectIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="M4 7.5 12 3l8 4.5-8 4.5-8-4.5Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" /><path d="m4 12 8 4.5 8-4.5M4 16.5 12 21l8-4.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg>;
}

function UsersIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="M16 20v-1.5a4.5 4.5 0 0 0-4.5-4.5h-3A4.5 4.5 0 0 0 4 18.5V20M10 10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM17 11a3 3 0 0 0 0-6M17.5 14a4 4 0 0 1 3.5 4v2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg>;
}

function TeamIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="M15.5 20v-1.5a4.5 4.5 0 0 0-4.5-4.5H7.5A4.5 4.5 0 0 0 3 18.5V20M9.25 10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM16 11a3 3 0 1 0 0-6M16.5 14a4.5 4.5 0 0 1 4.5 4.5V20" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>;
}

function CompareIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="M4 5h6v14H4zM14 5h6v14h-6z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" /><path d="M6.5 9h1M16.5 9h1M6.5 13h1M16.5 13h1" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" /></svg>;
}

function AssistantIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="M5 5.5A2.5 2.5 0 0 1 7.5 3h9A2.5 2.5 0 0 1 19 5.5v8a2.5 2.5 0 0 1-2.5 2.5H11l-4.5 4v-4A2.5 2.5 0 0 1 4 13.5v-8Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" /><path d="m10.25 8 .55-1.5.55 1.5 1.5.55-1.5.55-.55 1.5-.55-1.5-1.5-.55 1.5-.55Z" fill="currentColor" /></svg>;
}

function LogoutIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="M14 8V5a1 1 0 0 0-1-1H5a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1v-3M10 12h10m0 0-3-3m3 3-3 3" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg>;
}

function MenuIcon() {
  return <svg viewBox="0 0 24 24" className="size-5" fill="none" aria-hidden="true"><path d="M5 7h14M5 12h14M5 17h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>;
}
