import { ReportEditor } from "@/components/reports/report-editor";
export const metadata = { title: "Report editor" };
export default async function Page({ params }: { params: Promise<{ reportId: string }> }) {
  const { reportId } = await params;
  return <ReportEditor key={reportId} reportId={reportId} />;
}
