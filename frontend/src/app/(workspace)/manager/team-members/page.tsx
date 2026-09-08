import type { Metadata } from "next";

import { TeamMemberDirectory } from "@/components/manager/team-member-directory";

export const metadata: Metadata = { title: "Team members" };

export default function TeamMembersPage() {
  return <TeamMemberDirectory />;
}
