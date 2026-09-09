import type { Metadata } from "next";

import { ReportComparison } from "@/components/manager/report-comparison";

export const metadata: Metadata = { title: "Compare reports" };

export default function ReportComparisonPage() {
  return <ReportComparison />;
}
