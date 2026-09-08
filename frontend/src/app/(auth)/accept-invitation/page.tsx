import type { Metadata } from "next";

import { InvitationAcceptanceForm } from "@/components/auth/invitation-acceptance-form";

export const metadata: Metadata = { title: "Accept invitation" };

export default function AcceptInvitationPage() {
  return <InvitationAcceptanceForm />;
}
