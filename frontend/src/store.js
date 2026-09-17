import { reactive } from 'vue'
import { clearSession, getSession, getToken, setSession } from './api.js'

export const session = reactive({
  token: getToken(),
  user: getSession()
})

export function saveLogin(loginResp) {
  session.token = loginResp.token
  session.user = {
    userId: loginResp.userId,
    displayName: loginResp.displayName,
    role: loginResp.role,
    roleLabel: loginResp.roleLabel,
    officeId: loginResp.officeId,
    officeName: loginResp.officeName,
    objectId: loginResp.objectId
  }
  setSession(loginResp.token, session.user)
}

export function logout() {
  session.token = ''
  session.user = null
  clearSession()
}

export const isStaff = () => session.user && (session.user.role === 'SUPERVISOR' || session.user.role === 'OFFICER')
export const isObject = () => session.user && session.user.role === 'OBJECT'

// ---------- toast ----------
let toastSeq = 0
export const toasts = reactive([])

export function toast(message, type = 'info', timeout = 3200) {
  const id = ++toastSeq
  toasts.push({ id, message, type })
  setTimeout(() => {
    const i = toasts.findIndex(t => t.id === id)
    if (i >= 0) toasts.splice(i, 1)
  }, timeout)
}
