import { BookOpen, LayoutDashboard, UsersRound } from 'lucide-react'

export const COURSE_STATUSES = ['Publicado', 'Borrador', 'Archivado']
export const COURSE_STATUS_VALUES = {
  Publicado: 'PUBLICADO',
  Borrador: 'BORRADOR',
  Archivado: 'ARCHIVADO',
}
export const COURSE_STATUS_LABELS = Object.fromEntries(
  Object.entries(COURSE_STATUS_VALUES).map(([label, value]) => [value, label]),
)
export const COURSE_STATUS_BAR_STYLES = {
  Publicado: 'bg-emerald-200',
  Borrador: 'bg-amber-200',
  Archivado: 'bg-blue-200',
}

export const ADMIN_SECTIONS = [
  { id: 'summary', label: 'Resumen', icon: LayoutDashboard },
  { id: 'courses', label: 'Cursos', icon: BookOpen },
  { id: 'users', label: 'Usuarios', icon: UsersRound },
]
