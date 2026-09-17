const TOKEN_KEY = 'jwt_token'
const USER_KEY = 'jwt_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function getSession() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export function setSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message)
    this.status = status
    this.code = code
  }
}

export async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  let resp
  try {
    resp = await fetch(path, { ...options, headers })
  } catch (networkError) {
    throw new ApiError(0, 'NETWORK_ERROR', '网络连接失败，请检查网络后重试')
  }
  if (resp.status === 204) return null
  const data = await resp.json().catch(() => ({}))
  if (!resp.ok) {
    const message = data.message || `请求失败（${resp.status}）`
    if (resp.status === 401) {
      clearSession()
    }
    throw new ApiError(resp.status, data.code || 'ERROR', message)
  }
  return data
}
