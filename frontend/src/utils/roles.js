export const ROLES = {
  ADMIN: 'ADMIN',
  STUDENT: 'ALUMNO',
}

export function getHomeForRole(role) {
  if (role === ROLES.ADMIN) return '/admin'
  if (role === ROLES.STUDENT) return '/mi-aprendizaje'
  return '/'
}