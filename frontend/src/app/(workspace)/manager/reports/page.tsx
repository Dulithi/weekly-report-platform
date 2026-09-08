import type { Metadata } from "next";

import { ManagerReportList } from "@/components/manager/manager-report-list";

export const metadata: Metadata = { title: "Team reports" };

export default function ManagerReportsPage() {
  return <ManagerReportList />;
}
