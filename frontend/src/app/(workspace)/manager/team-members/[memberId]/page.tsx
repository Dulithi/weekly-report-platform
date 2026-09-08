import type { Metadata } from "next";

import { TeamMemberProfileView } from "@/components/manager/team-member-profile";

export const metadata: Metadata = { title: "Team member profile" };

export default async function TeamMemberPage({ params }: { params: Promise<{ memberId: string }> }) {
  const { memberId } = await params;
  return <TeamMemberProfileView memberId={memberId} />;
}
