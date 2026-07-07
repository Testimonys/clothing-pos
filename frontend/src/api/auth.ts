import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  displayName: string
  role: string
}

export interface UserInfo {
  username: string
  displayName: string
  role: string
}

/** 登录 */
export function login(params: LoginParams): Promise<LoginResult> {
  return request.post('/auth/login', params).then((res) => res.data)
}

/** 登出 */
export function logout(): Promise<void> {
  return request.post('/auth/logout').then((res) => res.data)
}

/** 获取当前用户信息 */
export function getCurrentUser(): Promise<UserInfo> {
  return request.get('/auth/current-user').then((res) => res.data)
}
