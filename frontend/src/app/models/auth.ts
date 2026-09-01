export type Role = 'ADMIN' | 'USER';

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
}

/** One article as tracked for the signed in reader. */
export interface ProgressEntry {
  articleId: number;
  completed: boolean;
  favourite: boolean;
}

export interface LoginResponse {
  token: string;
  type: string;
  role: Role;
  username: string;
  email: string;
  expiresAt: string;
}

/** Plain confirmation returned by the account flows. */
export interface MessageResponse {
  message: string;
}

export interface AuthSession {
  token: string;
  role: Role;
  username: string;
  email: string;
  expiresAt: string;
}
