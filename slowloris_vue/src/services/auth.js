// 统一的登录态与角色工具：所有「区分用户端/管理员端」的判断都走这里
function decodeToken() {
  const token = localStorage.getItem('authToken')
  if (!token) return null
  try {
    return JSON.parse(atob(token.split('.')[1]))
  } catch {
    return null
  }
}

/** 当前用户角色（小写、去掉 role_ 前缀），无登录态返回空串 */
export function getRole() {
  const payload = decodeToken()
  if (!payload) return ''
  const roles = payload.roles
  let r = ''
  if (Array.isArray(roles) && roles.length) r = roles[0]
  else if (typeof roles === 'string') r = roles
  return (r || '').toLowerCase().replace('role_', '')
}

/** 是否管理员端 */
export function isAdmin() {
  return getRole() === 'admin'
}

/** 当前登录用户名 */
export function getUsername() {
  return decodeToken()?.username || ''
}

/** 角色中文名 */
export function roleLabel(role) {
  const r = (role || getRole() || '').toLowerCase().replace('role_', '')
  return { admin: '管理员', operator: '操作员', viewer: '查看员', user: '普通用户' }[r] || '普通用户'
}

/** 是否已登录 */
export function isAuthenticated() {
  return !!localStorage.getItem('authToken')
}
