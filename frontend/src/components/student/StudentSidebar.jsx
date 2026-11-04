
import { useEffect } from 'react'
import {
  Award,
  Bell,
  BookOpen,
  Heart,
  Settings,
  TrendingUp,
} from 'lucide-react'

const STUDENT_NAVIGATION = [
  { id: 'progress', label: 'Mi progreso', icon: TrendingUp, available: true },
  { id: 'courses', label: 'Mis cursos', icon: BookOpen, available: true },
  { id: 'certificates', label: 'Certificados', icon: Award, available: false },
  { id: 'favorites', label: 'Favoritos', icon: Heart, available: false },
  { id: 'notifications', label: 'Notificaciones', icon: Bell, available: false },
  { id: 'settings', label: 'Configuración', icon: Settings, available: false },
]

export default function StudentSidebar({ user, activeItem, onNavigate, mobileOpen, onClose }) {
  const displayName = [user?.firstName, user?.lastName]
    .map((name) => name?.trim())
    .filter(Boolean)
    .join(' ')
  const initials = [user?.firstName?.[0], user?.lastName?.[0]]
    .filter(Boolean)
    .join('')
    .toUpperCase() || 'A'

  useEffect(() => {
    if (!mobileOpen) return undefined

    const closeOnEscape = (event) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [mobileOpen, onClose])

  return (
    <>
      {mobileOpen && (
        <button
          aria-label="Cerrar navegación"
          className="fixed inset-x-0 bottom-0 top-[60px] z-40 cursor-default bg-slate-950/35 lg:hidden"
          onClick={onClose}
          tabIndex={-1}
          type="button"
        />
      )}
      <aside
        aria-label="Navegación del alumno"
        className={`${mobileOpen ? 'block' : 'hidden'} fixed bottom-0 left-0 top-[60px] z-50 w-[min(288px,85vw)] overflow-y-auto border-r border-slate-200 bg-white lg:sticky lg:top-[76px] lg:block lg:min-h-[calc(100vh-76px)] lg:w-[320px] lg:overflow-visible`}
        id="student-sidebar"
      >
        <div className="border-b border-slate-100 px-4 py-5 lg:py-5 lg:pl-7 lg:pr-4">
          <div className="flex items-start gap-3">
            <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary lg:size-10">
              {initials}
            </span>
            <div className="min-w-0 flex-1">
              <p className="break-words text-[13px] font-semibold text-slate-800 lg:text-base">
                {displayName}
              </p>
              <p className="mt-0.5 break-all text-[10px] leading-4 text-slate-500 lg:text-[11px]">{user?.email}</p>
            </div>
          </div>
        </div>
        <nav aria-label="Menú del alumno" className="space-y-2.5 px-4 py-4 lg:pl-7 lg:pr-3">
          {STUDENT_NAVIGATION.map(({ id, label, icon: Icon, available }) => (
            <button
              aria-current={activeItem === id ? 'page' : undefined}
              className={`flex min-h-[43px] w-full items-center gap-3 rounded-md px-3 text-left text-[13px] transition lg:min-h-[58px] lg:text-[15px] ${
                activeItem === id
                  ? 'bg-primary/10 font-semibold text-primary'
                  : available
                    ? 'font-medium text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                    : 'cursor-not-allowed font-medium text-slate-400'
              }`}
              disabled={!available}
              key={id}
              onClick={() => {
                onNavigate(id)
                onClose()
              }}
              title={available ? undefined : 'Esta opción todavía no está disponible'}
              type="button"
            >
              <Icon aria-hidden="true" className="shrink-0" size={26} strokeWidth={1.8} />
              <span className="flex-1">{label}</span>
            </button>
          ))}
        </nav>
      </aside>
    </>
  )
}
