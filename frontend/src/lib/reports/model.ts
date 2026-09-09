export const priorities = ["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const;
export const taskStatuses = ["NOT_STARTED", "IN_PROGRESS", "COMPLETED", "BLOCKED"] as const;
export const taskTypes = ["DEVELOPMENT", "TESTING", "MEETINGS", "DOCUMENTATION", "RESEARCH", "DESIGN", "OTHER"] as const;
export type ReportStatus = "DRAFT" | "SUBMITTED" | "NEEDS_CORRECTION" | "APPROVED";
export interface CompletedTask {
  projectId: string | null; projectName?: string; taskName: string; description: string | null;
  priority: typeof priorities[number]; plannedPercentage: number; actualPercentage: number;
  status: typeof taskStatuses[number]; plannedMinutes: number | null; spentMinutes: number | null;
  deliverable: string | null;
}
export interface PlannedTask {
  projectId: string | null; projectName?: string; taskName: string; description: string | null;
  priority: typeof priorities[number]; estimatedMinutes: number | null;
}
export interface Blocker { description: string; keyBlocker: boolean; resolved: boolean }
export interface Achievement { description: string; keyAchievement: boolean }
export interface TimeEntry { taskType: typeof taskTypes[number]; minutes: number }
export interface ReportContent {
  notes: string | null; completedTasks: CompletedTask[]; plannedTasks: PlannedTask[];
  blockers: Blocker[]; achievements: Achievement[]; timeEntries: TimeEntry[];
}
export interface WeeklyReport {
  id: string; userId: string; weekStart: string; weekEnd: string; status: ReportStatus;
  submittedAt: string | null; approvedAt: string | null; entityVersion: number;
  currentVersion: ReportContent & {
    id: string; entityVersion: number; versionNumber: number;
    createdAt: string; submittedAt: string | null;
  };
}
export interface WeeklyReportSummary {
  id: string;
  weekStart: string;
  weekEnd: string;
  status: ReportStatus;
  currentVersionNumber: number;
  submittedAt: string | null;
  updatedAt: string;
}
export interface Project {
  id: string; name: string; description: string | null; status: "ACTIVE" | "ARCHIVED";
  createdBy: string; createdAt: string; updatedAt: string;
}
export interface ProjectMember {
  id: string; email: string; firstName: string; lastName: string;
  role: "TEAM_MEMBER" | "MANAGER" | "ADMIN";
}
export interface UserSummary extends ProjectMember { active: boolean }
export interface ManagerAssignment {
  teamMemberId: string; managerId: string; assignedAt: string;
}
export type InvitationStatus = "PENDING" | "ACCEPTED" | "EXPIRED" | "REVOKED";
export interface UserInvitation {
  id: string; email: string; role: ProjectMember["role"]; status: InvitationStatus;
  expiresAt: string; acceptedAt: string | null; revokedAt: string | null; createdAt: string;
}
export interface CreatedUserInvitation {
  invitation: UserInvitation; acceptanceToken: string;
}
export interface ReportMember {
  id: string; email: string; firstName: string; lastName: string; active: boolean;
}
export interface TeamMemberReportStatistics {
  totalReports: number; draftReports: number; submittedReports: number;
  needsCorrectionReports: number; approvedReports: number;
}
export interface TeamMemberProfile {
  member: ReportMember; statistics: TeamMemberReportStatistics;
}
export interface ManagerReportSummary {
  id: string; member: ReportMember; weekStart: string; weekEnd: string;
  status: ReportStatus; submittedAt: string | null; approvedAt: string | null; updatedAt: string;
}
export type ReportVersion = WeeklyReport["currentVersion"];
export interface ManagerReportDetail {
  id: string; member: ReportMember; weekStart: string; weekEnd: string;
  status: ReportStatus; submittedAt: string | null; approvedAt: string | null;
  submittedVersion: ReportVersion;
}
export interface ReportVersionSummary {
  id: string; versionNumber: number; createdAt: string; submittedAt: string | null;
  submitted: boolean; current: boolean;
}
export interface Page<T> {
  content: T[]; number: number; size: number; totalElements: number;
  totalPages: number; first: boolean; last: boolean;
}
export interface Review {
  id: string; reportVersionId: string; reportVersionNumber: number;
  action: "APPROVED" | "CHANGES_REQUESTED"; comment: string | null;
  reviewer: ReportMember;
  createdAt: string;
}
export type SubmissionTrackingStatus = ReportStatus | "NOT_STARTED";
export type SubmissionTiming = "ON_TIME" | "PENDING" | "LATE";
export type ReportActivityType =
  | "REPORT_CREATED"
  | "REPORT_SUBMITTED"
  | "REPORT_RESUBMITTED"
  | "REPORT_CHANGES_REQUESTED"
  | "REPORT_APPROVED";
export interface SubmissionTracking {
  member: ReportMember; reportId: string | null; weekStart: string; weekEnd: string;
  status: SubmissionTrackingStatus; timing: SubmissionTiming; dueAt: string;
  firstSubmittedAt: string | null;
}
export interface ManagerDashboard {
  weekStart: string; weekEnd: string; dueAt: string;
  summary: {
    totalActiveMembers: number; submittedReports: number; onTimeSubmissions: number;
    pendingSubmissions: number; lateSubmissions: number; submissionRatePercent: number;
    onTimeComplianceRatePercent: number; needsCorrectionReports: number; openBlockers: number;
  };
  submissionsByMember: SubmissionTracking[];
  completedTaskTrend: Array<{ weekStart: string; completedTasks: number }>;
  projectTaskDistribution: Array<{ projectId: string | null; projectName: string; taskCount: number }>;
  timeByTaskType: Array<{ taskType: typeof taskTypes[number]; minutes: number }>;
  recentActivity: Array<{
    id: string; type: ReportActivityType; reportId: string | null;
    actor: { id: string; firstName: string; lastName: string } | null; createdAt: string;
  }>;
}
export interface MemberSectionComparison {
  member: ReportMember; reportId: string | null; status: SubmissionTrackingStatus;
  timing: SubmissionTiming; submittedVersionNumber: number | null;
  blockers: Array<Blocker & { id: string; sortOrder: number }>;
  achievements: Array<Achievement & { id: string; sortOrder: number }>;
}
export interface WeeklySectionComparison {
  weekStart: string; weekEnd: string; members: MemberSectionComparison[];
}
export interface AssistantSource {
  sourceKey: string; reportId: string; versionNumber: number;
  memberName: string; weekStart: string; href: string;
}
export interface AssistantResponse {
  answer: string; fromWeek: string; throughWeek: string;
  reportsConsidered: number; sources: AssistantSource[];
}
export const label = (value: string) => value.toLowerCase().replaceAll("_", " ").replace(/^./, c => c.toUpperCase());
export function isMonday(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value) && new Date(`${value}T12:00:00Z`).getUTCDay() === 1;
}
// Use date-only UTC arithmetic: local daylight-saving changes must not shift a week.
export function weekEnd(value: string) {
  const date = new Date(`${value}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + 6);
  return date.toISOString().slice(0, 10);
}
export function updatePayload(content: ReportContent, entityVersion: number) {
  return { notes: content.notes, entityVersion,
    completedTasks: content.completedTasks.map(t => ({ projectId: t.projectId, taskName: t.taskName, description: t.description, priority: t.priority, plannedPercentage: t.plannedPercentage, actualPercentage: t.actualPercentage, status: t.status, plannedMinutes: t.plannedMinutes, spentMinutes: t.spentMinutes, deliverable: t.deliverable })),
    plannedTasks: content.plannedTasks.map(t => ({ projectId: t.projectId, taskName: t.taskName, description: t.description, priority: t.priority, estimatedMinutes: t.estimatedMinutes })),
    blockers: content.blockers.map(b => ({ description: b.description, keyBlocker: b.keyBlocker, resolved: b.resolved })),
    achievements: content.achievements.map(a => ({ description: a.description, keyAchievement: a.keyAchievement })),
    timeEntries: content.timeEntries.map(t => ({ taskType: t.taskType, minutes: t.minutes })),
  };
}
