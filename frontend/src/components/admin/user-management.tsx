"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";

import { useAuth } from "@/components/auth/auth-provider";
import { ApiError } from "@/lib/api/client";
import type { UserRole } from "@/lib/api/types";
import {
  type CreatedUserInvitation,
  type InvitationStatus,
  type ManagerAssignment,
  type Page,
  type UserInvitation,
  type UserSummary,
  label,
} from "@/lib/reports/model";

const roles: UserRole[] = ["TEAM_MEMBER", "MANAGER", "ADMIN"];
const invitationStatuses: InvitationStatus[] = ["PENDING", "ACCEPTED", "EXPIRED", "REVOKED"];

export function UserManagement() {
  const { user, request } = useAuth();
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [invitations, setInvitations] = useState<UserInvitation[]>([]);
  const [assignments, setAssignments] = useState<ManagerAssignment[]>([]);
  const [managerChoices, setManagerChoices] = useState<Record<string, string>>({});
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<UserRole | "">("");
  const [activeFilter, setActiveFilter] = useState<"" | "active" | "inactive">("");
  const [invitationFilter, setInvitationFilter] = useState<InvitationStatus>("PENDING");
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<UserRole>("TEAM_MEMBER");
  const [createdLink, setCreatedLink] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadUsers = useCallback(async () => {
    const loaded: UserSummary[] = [];
    for (let page = 0; ; page++) {
      const result = await request<Page<UserSummary>>(
        "/admin/users?page=" + page + "&size=100&sort=lastName,asc&sort=firstName,asc",
      );
      loaded.push(...result.content);
      if (result.last) return loaded;
    }
  }, [request]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [loadedUsers, loadedInvitations, loadedAssignments] = await Promise.all([
        loadUsers(),
        request<UserInvitation[]>("/admin/user-invitations"),
        request<ManagerAssignment[]>("/admin/manager-assignments"),
      ]);
      setUsers(loadedUsers);
      setInvitations(loadedInvitations);
      setAssignments(loadedAssignments);
      const assignmentByMember = new Map(
        loadedAssignments.map(item => [item.teamMemberId, item.managerId]),
      );
      setManagerChoices(Object.fromEntries(
        loadedUsers
          .filter(item => item.role === "TEAM_MEMBER")
          .map(item => [item.id, assignmentByMember.get(item.id) ?? ""]),
      ));
    } catch (caught) {
      setError(errorMessage(caught, "Could not load people and invitations."));
    } finally {
      setLoading(false);
    }
  }, [loadUsers, request]);

  useEffect(() => {
    if (user?.role === "ADMIN") void Promise.resolve().then(load);
  }, [load, user?.role]);

  const managers = useMemo(
    () => users.filter(item => item.role === "MANAGER" && item.active),
    [users],
  );
  const visibleUsers = useMemo(() => {
    const term = search.trim().toLowerCase();
    return users.filter(item => {
      const identity = [item.firstName, item.lastName, item.email].join(" ").toLowerCase();
      return (!term || identity.includes(term))
        && (!roleFilter || item.role === roleFilter)
        && (!activeFilter || item.active === (activeFilter === "active"));
    });
  }, [activeFilter, roleFilter, search, users]);
  const visibleInvitations = useMemo(
    () => invitations.filter(item => item.status === invitationFilter),
    [invitationFilter, invitations],
  );

  function beginAction() {
    setBusy(true);
    setError("");
    setNotice("");
  }

  async function createInvitation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const email = inviteEmail.trim();
    if (!email) return;
    beginAction();
    setCreatedLink("");
    try {
      const created = await request<CreatedUserInvitation>("/admin/user-invitations", {
        method: "POST",
        body: JSON.stringify({ email, role: inviteRole }),
      });
      const fragment = new URLSearchParams({ token: created.acceptanceToken }).toString();
      setCreatedLink(window.location.origin + "/accept-invitation#" + fragment);
      setInviteEmail("");
      setNotice(
        "Invitation created for " + created.invitation.email
          + ". Email delivery is attempted when SMTP is enabled. Copy the link as a secure fallback; the token is not stored in readable form.",
      );
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "Could not create the invitation."));
    } finally {
      setBusy(false);
    }
  }

  async function copyInvitationLink() {
    try {
      await navigator.clipboard.writeText(createdLink);
      setNotice("Invitation link copied. Share it only with the intended recipient.");
    } catch {
      setError("The browser blocked clipboard access. Select and copy the link manually.");
    }
  }

  async function revokeInvitation(invitation: UserInvitation) {
    const confirmed = window.confirm(
      "Revoke the invitation for " + invitation.email + "? Its link will stop working.",
    );
    if (!confirmed) return;
    beginAction();
    try {
      await request("/admin/user-invitations/" + invitation.id, { method: "DELETE" });
      setNotice("Invitation for " + invitation.email + " revoked.");
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "Could not revoke the invitation."));
    } finally {
      setBusy(false);
    }
  }

  async function changeRole(account: UserSummary, nextRole: UserRole) {
    if (nextRole === account.role) return;
    const confirmed = window.confirm(
      "Change " + account.firstName + " " + account.lastName + " from "
        + label(account.role) + " to " + label(nextRole) + "?",
    );
    if (!confirmed) return;
    beginAction();
    try {
      await request("/admin/users/" + account.id + "/role", {
        method: "PATCH",
        body: JSON.stringify({ role: nextRole }),
      });
      setNotice(account.firstName + " " + account.lastName + "'s role was changed.");
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "Could not change the role."));
    } finally {
      setBusy(false);
    }
  }

  async function changeActiveState(account: UserSummary) {
    const nextActive = !account.active;
    if (!nextActive) {
      const confirmed = window.confirm(
        "Deactivate " + account.firstName + " " + account.lastName
          + "? Their active sessions and API access will stop immediately.",
      );
      if (!confirmed) return;
    }
    beginAction();
    try {
      await request("/admin/users/" + account.id, {
        method: "PATCH",
        body: JSON.stringify({ active: nextActive }),
      });
      setNotice(
        account.firstName + " " + account.lastName + " is now "
          + (nextActive ? "active." : "inactive."),
      );
      await load();
    } catch (caught) {
      setError(errorMessage(
        caught,
        "Could not " + (nextActive ? "activate" : "deactivate") + " the account.",
      ));
    } finally {
      setBusy(false);
    }
  }

  async function saveManager(member: UserSummary) {
    const managerId = managerChoices[member.id] ?? "";
    const current = assignments.find(item => item.teamMemberId === member.id)?.managerId ?? "";
    if (managerId === current) return;
    beginAction();
    try {
      if (managerId) {
        await request("/admin/users/" + member.id + "/manager", {
          method: "PUT",
          body: JSON.stringify({ managerId }),
        });
      } else {
        await request("/admin/users/" + member.id + "/manager", { method: "DELETE" });
      }
      setNotice(
        member.firstName + " " + member.lastName + "'s manager assignment was updated.",
      );
      await load();
    } catch (caught) {
      setError(errorMessage(caught, "Could not update the manager assignment."));
    } finally {
      setBusy(false);
    }
  }

  if (user?.role !== "ADMIN") return <AccessDenied />;

  return (
    <section className="mx-auto max-w-7xl">
      <header className="mb-7 border-b border-slate-200 pb-7">
        <p className="text-sm font-semibold text-brand">Administration</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">People and invitations</h1>
        <p className="mt-2 max-w-3xl text-slate-500">
          Invite colleagues, manage account access and roles, and connect team members to managers.
        </p>
      </header>

      {error && <p role="alert" className="mb-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</p>}
      {notice && <p role="status" className="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">{notice}</p>}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.55fr)_minmax(330px,0.85fr)]">
        <UserList
          users={visibleUsers}
          total={users.length}
          managers={managers}
          assignments={assignments}
          managerChoices={managerChoices}
          currentUserId={user.id}
          loading={loading}
          busy={busy}
          search={search}
          roleFilter={roleFilter}
          activeFilter={activeFilter}
          onSearch={setSearch}
          onRoleFilter={setRoleFilter}
          onActiveFilter={setActiveFilter}
          onManagerChoice={(memberId, managerId) =>
            setManagerChoices(current => ({ ...current, [memberId]: managerId }))}
          onRoleChange={changeRole}
          onActiveChange={changeActiveState}
          onManagerSave={saveManager}
        />

        <div className="space-y-6">
          <form onSubmit={createInvitation} className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-6">
            <h2 className="font-semibold text-slate-900">Invite a user</h2>
            <p className="mt-1 text-sm leading-6 text-slate-500">
              The recipient chooses their name and password. The single-use link expires automatically.
            </p>
            <label className="mt-4 block text-sm font-medium text-slate-700">
              Email address
              <input className="report-input mt-1" type="email" required maxLength={320}
                autoComplete="off" value={inviteEmail}
                onChange={event => setInviteEmail(event.target.value)}
                placeholder="colleague@company.com" />
            </label>
            <label className="mt-3 block text-sm font-medium text-slate-700">
              Role
              <select className="report-select mt-1" value={inviteRole}
                onChange={event => setInviteRole(event.target.value as UserRole)}>
                {roles.map(role => <option key={role} value={role}>{label(role)}</option>)}
              </select>
            </label>
            <button type="submit" className="report-primary mt-4" disabled={busy}>
              Create invitation
            </button>
            {createdLink && (
              <div className="mt-5 rounded-xl border border-amber-200 bg-amber-50 p-4">
                <p className="text-sm font-semibold text-amber-950">Copy this link now</p>
                <p className="mt-1 text-xs leading-5 text-amber-800">
                  For security, the raw token is shown once and never appears in the invitation list.
                </p>
                <input className="report-input mt-3 font-mono text-xs" readOnly value={createdLink}
                  onFocus={event => event.currentTarget.select()} aria-label="New invitation link" />
                <button type="button" className="report-secondary mt-3"
                  onClick={() => void copyInvitationLink()}>Copy link</button>
              </div>
            )}
          </form>

          <InvitationList
            invitations={visibleInvitations}
            status={invitationFilter}
            loading={loading}
            busy={busy}
            onStatus={setInvitationFilter}
            onRevoke={revokeInvitation}
          />
        </div>
      </div>
    </section>
  );
}

interface UserListProps {
  users: UserSummary[];
  total: number;
  managers: UserSummary[];
  assignments: ManagerAssignment[];
  managerChoices: Record<string, string>;
  currentUserId: string;
  loading: boolean;
  busy: boolean;
  search: string;
  roleFilter: UserRole | "";
  activeFilter: "" | "active" | "inactive";
  onSearch(value: string): void;
  onRoleFilter(value: UserRole | ""): void;
  onActiveFilter(value: "" | "active" | "inactive"): void;
  onManagerChoice(memberId: string, managerId: string): void;
  onRoleChange(account: UserSummary, role: UserRole): Promise<void>;
  onActiveChange(account: UserSummary): Promise<void>;
  onManagerSave(account: UserSummary): Promise<void>;
}

function UserList(props: UserListProps) {
  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <div className="border-b border-slate-100 p-5 sm:p-6">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="font-semibold text-slate-900">User accounts</h2>
            <p className="mt-1 text-sm text-slate-500">
              Server safeguards protect active administration and valid team relationships.
            </p>
          </div>
          <span className="text-sm text-slate-400">{props.users.length} of {props.total}</span>
        </div>
        <div className="mt-5 grid gap-3 sm:grid-cols-[minmax(180px,1fr)_170px_150px]">
          <label className="text-sm font-medium text-slate-700">Search
            <input className="report-input mt-1" type="search" value={props.search}
              onChange={event => props.onSearch(event.target.value)} placeholder="Name or email" />
          </label>
          <label className="text-sm font-medium text-slate-700">Role
            <select className="report-select mt-1" value={props.roleFilter}
              onChange={event => props.onRoleFilter(event.target.value as UserRole | "")}>
              <option value="">All roles</option>
              {roles.map(role => <option key={role} value={role}>{label(role)}</option>)}
            </select>
          </label>
          <label className="text-sm font-medium text-slate-700">Access
            <select className="report-select mt-1" value={props.activeFilter}
              onChange={event => props.onActiveFilter(event.target.value as UserListProps["activeFilter"])}>
              <option value="">Any state</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </label>
        </div>
      </div>

      {props.loading ? <p className="p-6 text-sm text-slate-500">Loading accounts…</p>
        : props.users.length ? <ul className="divide-y divide-slate-100">
          {props.users.map(account => {
            const currentManagerId = props.assignments
              .find(item => item.teamMemberId === account.id)?.managerId ?? "";
            const selectedManagerId = props.managerChoices[account.id] ?? "";
            return (
              <li key={account.id} className="p-5 sm:p-6">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-semibold text-slate-900">{account.firstName} {account.lastName}</p>
                      <AccountState active={account.active} />
                      {account.id === props.currentUserId && (
                        <span className="rounded-full bg-brand-soft px-2 py-0.5 text-xs font-semibold text-brand-strong">You</span>
                      )}
                    </div>
                    <p className="mt-1 break-all text-sm text-slate-500">{account.email}</p>
                  </div>
                  <div className="flex flex-wrap items-end gap-2">
                    <label className="text-xs font-medium text-slate-500">Role
                      <select className="report-select mt-1 min-w-40" value={account.role}
                        aria-label={"Role for " + account.firstName + " " + account.lastName}
                        disabled={props.busy || account.id === props.currentUserId}
                        onChange={event => {
                          const nextRole = event.target.value as UserRole;
                          event.target.value = account.role;
                          void props.onRoleChange(account, nextRole);
                        }}>
                        {roles.map(role => <option key={role} value={role}>{label(role)}</option>)}
                      </select>
                    </label>
                    <button type="button" className="report-secondary"
                      disabled={props.busy || account.id === props.currentUserId}
                      onClick={() => void props.onActiveChange(account)}>
                      {account.active ? "Deactivate" : "Activate"}
                    </button>
                  </div>
                </div>
                {account.role === "TEAM_MEMBER" && (
                  <div className="mt-4 flex flex-col gap-2 rounded-xl bg-slate-50 p-3 sm:flex-row sm:items-end">
                    <label className="flex-1 text-xs font-medium text-slate-500">Manager
                      <select className="report-select mt-1" value={selectedManagerId}
                        disabled={props.busy || !account.active}
                        onChange={event => props.onManagerChoice(account.id, event.target.value)}>
                        <option value="">No manager assigned</option>
                        {props.managers.map(manager => (
                          <option key={manager.id} value={manager.id}>
                            {manager.firstName} {manager.lastName} · {manager.email}
                          </option>
                        ))}
                      </select>
                    </label>
                    <button type="button" className="report-secondary"
                      disabled={props.busy || !account.active || selectedManagerId === currentManagerId}
                      onClick={() => void props.onManagerSave(account)}>Save manager</button>
                  </div>
                )}
              </li>
            );
          })}
        </ul> : <p className="p-6 text-sm text-slate-500">No accounts match these filters.</p>}
    </section>
  );
}

function InvitationList({
  invitations, status, loading, busy, onStatus, onRevoke,
}: {
  invitations: UserInvitation[];
  status: InvitationStatus;
  loading: boolean;
  busy: boolean;
  onStatus(status: InvitationStatus): void;
  onRevoke(invitation: UserInvitation): Promise<void>;
}) {
  return (
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
      <div className="border-b border-slate-100 p-5">
        <div className="flex items-end justify-between gap-3">
          <div><h2 className="font-semibold text-slate-900">Invitations</h2><p className="mt-1 text-xs text-slate-400">Tokens never appear here</p></div>
          <label className="text-xs font-medium text-slate-500">Status
            <select className="report-select mt-1" value={status}
              onChange={event => onStatus(event.target.value as InvitationStatus)}>
              {invitationStatuses.map(item => <option key={item} value={item}>{label(item)}</option>)}
            </select>
          </label>
        </div>
      </div>
      {loading ? <p className="p-5 text-sm text-slate-500">Loading invitations…</p>
        : invitations.length ? <ul className="divide-y divide-slate-100">
          {invitations.map(invitation => (
            <li key={invitation.id} className="p-5">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="break-all font-medium text-slate-800">{invitation.email}</p>
                  <p className="mt-1 text-xs text-slate-400">
                    {label(invitation.role)} · {invitation.status === "PENDING"
                      ? "Expires " + formatDateTime(invitation.expiresAt)
                      : label(invitation.status) + " "
                        + formatDateTime(invitation.acceptedAt ?? invitation.revokedAt ?? invitation.expiresAt)}
                  </p>
                </div>
                <InvitationState status={invitation.status} />
              </div>
              {invitation.status === "PENDING" && (
                <button type="button" className="mt-3 text-sm font-semibold text-red-700 hover:underline disabled:opacity-50"
                  disabled={busy} onClick={() => void onRevoke(invitation)}>Revoke invitation</button>
              )}
            </li>
          ))}
        </ul> : <p className="p-5 text-sm text-slate-500">No {label(status).toLowerCase()} invitations.</p>}
    </section>
  );
}

function AccountState({ active }: { active: boolean }) {
  const colors = active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500";
  return <span className={"rounded-full px-2 py-0.5 text-xs font-semibold " + colors}>{active ? "Active" : "Inactive"}</span>;
}

function InvitationState({ status }: { status: InvitationStatus }) {
  const colors: Record<InvitationStatus, string> = {
    PENDING: "bg-amber-50 text-amber-700",
    ACCEPTED: "bg-emerald-50 text-emerald-700",
    EXPIRED: "bg-slate-100 text-slate-500",
    REVOKED: "bg-red-50 text-red-700",
  };
  return <span className={"shrink-0 rounded-full px-2 py-0.5 text-xs font-semibold " + colors[status]}>{label(status)}</span>;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" })
    .format(new Date(value));
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : error instanceof Error ? error.message : fallback;
}

function AccessDenied() {
  return (
    <div className="mx-auto max-w-xl rounded-2xl border border-amber-200 bg-amber-50 p-6">
      <h1 className="font-semibold text-amber-950">Administrator access required</h1>
      <p className="mt-2 text-sm text-amber-800">Only administrators can manage users and invitations.</p>
      <Link href="/dashboard" className="mt-4 inline-block font-semibold text-brand hover:underline">Return to dashboard</Link>
    </div>
  );
}
