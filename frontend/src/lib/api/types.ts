export type UserRole = "TEAM_MEMBER" | "MANAGER" | "ADMIN";

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: "Bearer" | string;
  expiresAt: string;
}

export interface CurrentUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
}

export type RegisteredUser = CurrentUser;

export interface CsrfProof {
  headerName: string;
  token: string;
}

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput extends LoginInput {
  firstName: string;
  lastName: string;
}

export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}
