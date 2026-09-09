"use client";

import { useAuth } from "@/components/auth/auth-provider";
import { DashboardWelcome } from "@/components/dashboard/dashboard-welcome";
import { ManagerDashboardView } from "@/components/dashboard/manager-dashboard";

export function DashboardContent() {
  const { user } = useAuth();
  if (!user) return null;
  return user.role === "MANAGER" || user.role === "ADMIN"
    ? <ManagerDashboardView />
    : <DashboardWelcome />;
}
