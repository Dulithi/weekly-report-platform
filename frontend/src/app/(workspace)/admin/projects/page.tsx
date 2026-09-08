import type { Metadata } from "next";
import { ProjectManagement } from "@/components/admin/project-management";

export const metadata: Metadata = { title: "Project management" };

export default function ProjectsPage() {
  return <ProjectManagement />;
}
