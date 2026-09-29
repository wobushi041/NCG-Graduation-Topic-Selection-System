import {request} from '@umijs/max';

export type ApiResponse<T> = {code?: number; message?: string; data?: T};
export type LoginRequest = {account: string; password: string};
export type RoleSwitchAvailability = {available: boolean; targetRole?: 'teacher' | 'college'};
export type AdminResetPasswordResult = {account: string; temporaryPassword: string};
export type EmailVerificationResult = {proofToken: string; expiresInSeconds: number};

const post = <T>(url: string, data?: unknown, options?: Record<string, unknown>) =>
  request<ApiResponse<T>>(url, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    data,
    ...(options || {}),
  });

export const login = (body: LoginRequest, options?: Record<string, unknown>) =>
  post<API.LoginUserVO>('/auth/login', body, options);

export const logout = () => post<boolean>('/auth/logout');

export const switchRole = (targetRole: 'teacher' | 'college') =>
  post<API.LoginUserVO>('/auth/role-switch', {targetRole});

export const getRoleSwitchAvailability = () =>
  request<ApiResponse<RoleSwitchAvailability>>('/auth/role-switch/availability', {method: 'GET'});

export const adminResetPassword = (body: {account: string; name: string}) =>
  post<AdminResetPasswordResult>('/auth/password/admin-reset', body);

export const changePassword = (body: {
  account: string;
  currentPassword: string;
  newPassword: string;
  email?: string;
  emailProofToken?: string;
}) => post<number>('/auth/password/change', body);

export const resetPasswordByCode = (body: {
  account: string;
  resetCode: string;
  newPassword: string;
}) => post<number>('/auth/password/reset', body);

export const sendPasswordResetCode = (account: string) =>
  post<string>('/auth/password/reset-code/send', {account});

export const sendEmailVerificationCode = (email: string) =>
  post<string>('/auth/email-verification/code/send', {email});

export const verifyEmailCode = (email: string, code: string) =>
  post<EmailVerificationResult>('/auth/email-verification/code/verify', {email, code});
