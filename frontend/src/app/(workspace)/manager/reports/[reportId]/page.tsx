import type { Metadata } from "next";

import { ManagerReportDetail } from "@/components/manager/manager-report-detail";

export const metadata: Metadata = { title: "Review team report" };

export default async function ManagerReportPage({ params }: { params: Promise<{ reportId: string }> }) {
  const { reportId } = await params;
  return <ManagerReportDetail reportId={reportId} />;
}
