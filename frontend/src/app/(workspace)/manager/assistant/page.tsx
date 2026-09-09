import type { Metadata } from "next";

import { ReportAssistant } from "@/components/manager/report-assistant";

export const metadata: Metadata = { title: "Report assistant" };

export default function ReportAssistantPage() {
  return <ReportAssistant />;
}
