import { AppShell } from "@/components/app/app-shell";
import { AuthGuard } from "@/components/auth/auth-guard";

export default function WorkspaceLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <AppShell>{children}</AppShell>
    </AuthGuard>
  );
}
