import type { Metadata } from "next";

import { ReportDetail } from "@/components/reports/report-detail";

export const metadata: Metadata = { title: "Report details" };

export default async function Page({ params }: { params: Promise<{ reportId: string }> }) {
  const { reportId } = await params;
  return <ReportDetail reportId={reportId} />;
}
