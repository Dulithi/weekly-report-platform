import type { Metadata } from "next";

import { UserManagement } from "@/components/admin/user-management";

export const metadata: Metadata = { title: "People and invitations" };

export default function UsersPage() {
  return <UserManagement />;
}
