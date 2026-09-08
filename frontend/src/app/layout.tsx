import type { Metadata } from "next";

import { AuthProvider } from "@/components/auth/auth-provider";

import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Weekly",
    template: "%s · Weekly",
  },
  description: "Weekly reporting and team insights in one focused workspace.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className="h-full antialiased"
    >
      <body className="min-h-full bg-stone-50 text-slate-950">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
