import type { Metadata } from "next";

import { DashboardWelcome } from "@/components/dashboard/dashboard-welcome";

export const metadata: Metadata = { title: "Dashboard" };

export default function DashboardPage() {
  return <DashboardWelcome />;
}
