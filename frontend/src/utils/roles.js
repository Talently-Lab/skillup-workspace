// ⚠️ A CONFIRMAR con Backend: valores exactos del rol
export const ROLES = {
  ADMIN: 'ADMIN',
  STUDENT: 'STUDENT',
}

export function getHomeForRole(role) {
  if (role === ROLES.ADMIN) return '/admin'
  return '/' // cuando exista el dashboard del alumno, va acá
}