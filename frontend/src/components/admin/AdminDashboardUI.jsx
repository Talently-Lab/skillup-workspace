import { useState } from 'react'
import { CalendarDays, ChevronDown, Image as ImageIcon, Mail, Menu, Pencil, Trash2, UsersRound, X } from 'lucide-react'
import { ADMIN_SECTIONS, COURSE_STATUSES } from './adminDashboardConstants'

const COURSE_STATUS_STYLES = {
  Publicado: 'border-[#bbf7d0] bg-[#DCFCE7] text-[#166534]',
  Borrador: 'border-[#fde68a] bg-[#FEF3C7] text-[#92400E]',
  Archivado: 'border-[#bfdbfe] bg-[#DBEAFE] text-[#1E40AF]',
}
const COURSE_STATUS_DOT_STYLES = {
  Publicado: 'bg-[#166534]',
  Borrador: 'bg-[#92400E]',
  Archivado: 'bg-[#1E40AF]',
}
export function AdminSidebar({ activeSection, onNavigate }) {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <aside className="relative z-20 w-full shrink-0 border-b border-border bg-white px-4 py-3 md:min-h-screen md:w-48 md:border-b-0 md:border-r md:px-3 md:py-6 lg:w-60 lg:px-5 lg:py-7">
      <div className="flex items-center justify-between md:mb-10">
        <p className="px-3 text-xs font-bold uppercase tracking-[0.14em] text-muted">
          Administración
        </p>
        <button
          aria-expanded={menuOpen}
          aria-label={menuOpen ? 'Cerrar menú de administración' : 'Abrir menú de administración'}
          className="flex size-11 items-center justify-center rounded-lg border border-border text-ink hover:bg-slate-50 md:hidden"
          onClick={() => setMenuOpen((open) => !open)}
          type="button"
        >
          {menuOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>
      <nav
        aria-label="Navegación administrativa"
        className={`${menuOpen ? 'flex' : 'hidden'} absolute left-0 right-0 top-full flex-col gap-1 border-b border-border bg-white p-3 shadow-md md:static md:flex md:border-0 md:bg-transparent md:p-0 md:shadow-none`}
      >
        {ADMIN_SECTIONS.map(({ id, label, icon: Icon }) => (
          <button
            aria-current={activeSection === id ? 'page' : undefined}
            className={`flex min-h-11 items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm transition ${
              activeSection === id
                ? 'bg-primary/10 font-semibold text-primary'
                : 'font-medium text-muted hover:bg-slate-50 hover:text-ink'
            }`}
            key={id}
            onClick={() => {
              onNavigate(id)
              setMenuOpen(false)
            }}
            type="button"
          >
            <Icon size={18} />
            {label}
          </button>
        ))}
      </nav>
    </aside>
  )
}

export function CourseStatusSelector({ courseId, status, onChange, disabled, error }) {
  const badgeStyles =
    COURSE_STATUS_STYLES[status] ?? 'border-slate-200 bg-slate-50 text-slate-500'

  return (
    <div className="min-w-0">
      <span
        className={`relative inline-flex min-h-8 max-w-full items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-semibold transition focus-within:ring-2 focus-within:ring-primary/20 sm:text-xs ${badgeStyles}`}
      >
        <span aria-hidden="true" className={`size-1.5 shrink-0 rounded-full ${COURSE_STATUS_DOT_STYLES[status] ?? 'bg-slate-400'}`} />
        <span className="truncate">{status || 'Seleccionar'}</span>
        <ChevronDown aria-hidden="true" className="shrink-0 opacity-60" size={13} />
        <select
          aria-label={`Estado del curso ${courseId}`}
          aria-busy={disabled}
          className="absolute inset-0 h-full w-full cursor-pointer rounded-full opacity-0 disabled:cursor-wait"
          disabled={disabled}
          onChange={(event) => onChange(courseId, event.target.value)}
          title="Cambiar estado del curso"
          value={status}
        >
          <option className="bg-white text-slate-600" value="">Seleccionar estado</option>
          {COURSE_STATUSES.map((courseStatus) => (
            <option
              className={COURSE_STATUS_STYLES[courseStatus]}
              key={courseStatus}
              value={courseStatus}
            >
              {courseStatus}
            </option>
          ))}
        </select>
      </span>
      {error && <span className="mt-1 block max-w-48 text-[11px] leading-4 text-danger" role="alert">{error}</span>}
    </div>
  )
}

export function CourseThumbnail({ course }) {
  const [imageFailed, setImageFailed] = useState(false)

  return (
    <span className="flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-slate-100 text-slate-400">
      {course.imageUrl && !imageFailed ? (
        <img
          alt=""
          className="size-full object-cover"
          onError={() => setImageFailed(true)}
          src={course.imageUrl}
        />
      ) : (
        <ImageIcon aria-hidden="true" size={17} />
      )}
    </span>
  )
}

export function CourseActions({ course, deleting, onEdit, onDelete, onViewEnrollments }) {
  return (
    <div className="flex items-center justify-end gap-1.5">
      <button
        aria-label={`Ver inscritos en ${course.title}`}
        className="flex size-10 shrink-0 items-center justify-center rounded-lg border border-border text-slate-500 transition hover:border-primary/30 hover:bg-secondary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
        onClick={() => onViewEnrollments(course)}
        title="Ver inscritos"
        type="button"
      >
        <UsersRound aria-hidden="true" size={17} />
      </button>
      <button
        aria-label={`Editar ${course.title}`}
        className="flex size-10 shrink-0 items-center justify-center rounded-lg border border-border text-slate-500 transition hover:border-primary/30 hover:bg-secondary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
        onClick={() => onEdit(course.id)}
        title="Editar curso"
        type="button"
      >
        <Pencil aria-hidden="true" size={16} />
      </button>
      <button
        aria-label={`Eliminar ${course.title}`}
        className="flex size-10 shrink-0 items-center justify-center rounded-lg border border-border text-slate-500 transition hover:border-red-200 hover:bg-red-50 hover:text-danger focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-300 disabled:cursor-wait disabled:opacity-50"
        disabled={deleting}
        onClick={() => onDelete(course)}
        title="Eliminar curso"
        type="button"
      >
        <Trash2 aria-hidden="true" size={16} />
      </button>
    </div>
  )
}

export function CourseEnrollmentsModal({ course, enrollments, loading, error, onClose }) {
  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
      role="presentation"
    >
      <section
        aria-labelledby="course-enrollments-title"
        aria-modal="true"
        className="max-h-[min(85vh,720px)] w-full max-w-2xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl"
        role="dialog"
      >
        <header className="flex items-start gap-4 border-b border-slate-100 px-5 py-4 sm:px-6">
          <div className="min-w-0 flex-1">
            <h2 className="text-lg font-bold text-ink" id="course-enrollments-title">
              Alumnos inscritos
            </h2>
            <p className="mt-1 truncate text-sm text-muted" title={course.title}>
              {course.title}
            </p>
          </div>
          <button
            aria-label="Cerrar lista de inscritos"
            className="flex size-9 shrink-0 items-center justify-center rounded-lg text-muted transition hover:bg-slate-100 hover:text-ink"
            onClick={onClose}
            type="button"
          >
            <X aria-hidden="true" size={18} />
          </button>
        </header>

        <div aria-live="polite" className="max-h-[calc(min(85vh,720px)-76px)] overflow-y-auto p-4 sm:p-6">
          {loading ? (
            <p className="py-10 text-center text-sm text-muted" role="status">
              Cargando alumnos inscritos...
            </p>
          ) : error ? (
            <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-danger" role="alert">
              No se pudo cargar la lista: {error}
            </p>
          ) : enrollments.length === 0 ? (
            <p className="py-10 text-center text-sm text-muted">
              Todavía no hay alumnos inscritos en este curso.
            </p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {enrollments.map((enrollment) => {
                const name = [enrollment.firstName, enrollment.lastName].filter(Boolean).join(' ')
                const enrollmentDate = enrollment.enrolledAt
                  ? new Intl.DateTimeFormat('es', {
                    dateStyle: 'medium',
                    timeStyle: 'short',
                  }).format(new Date(enrollment.enrolledAt))
                  : null
                return (
                  <li className="flex flex-col gap-2 py-4 first:pt-0 sm:flex-row sm:items-center sm:justify-between" key={enrollment.enrollmentId}>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-ink">{name || 'Alumno'}</p>
                      <p className="mt-1 flex min-w-0 items-center gap-1.5 text-xs text-muted">
                        <Mail aria-hidden="true" className="shrink-0" size={13} />
                        <span className="truncate">{enrollment.email}</span>
                      </p>
                    </div>
                    <div className="flex shrink-0 items-center gap-3 text-xs text-muted">
                      {enrollmentDate && (
                        <span className="inline-flex items-center gap-1.5">
                          <CalendarDays aria-hidden="true" size={13} />
                          {enrollmentDate}
                        </span>
                      )}
                      <span className="rounded-full bg-emerald-50 px-2.5 py-1 font-medium text-emerald-700">
                        {enrollment.status || 'Inscrito'}
                      </span>
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      </section>
    </div>
  )
}

export function TablePagination({ currentPage, pageSize, totalItems, label, onPageChange }) {
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))
  const firstItem = totalItems ? (currentPage - 1) * pageSize + 1 : 0
  const lastItem = Math.min(currentPage * pageSize, totalItems)

  return (
    <nav
      aria-label={`Paginación de ${label}`}
      className="flex flex-col gap-3 border-t border-slate-100 px-4 py-3 text-xs text-muted sm:flex-row sm:items-center sm:justify-between sm:px-5"
    >
      <p aria-live="polite">
        Mostrando {firstItem}–{lastItem} de {totalItems} {label}
      </p>
      <div className="flex items-center gap-2 self-end sm:self-auto">
        <button
          aria-label={`Ir a la página anterior de ${label}`}
          className="min-h-9 rounded-lg border border-border px-3 font-medium text-ink transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={currentPage <= 1}
          onClick={() => onPageChange(currentPage - 1)}
          type="button"
        >
          Anterior
        </button>
        <span className="min-w-20 text-center tabular-nums">
          Página {currentPage} de {totalPages}
        </span>
        <button
          aria-label={`Ir a la página siguiente de ${label}`}
          className="min-h-9 rounded-lg border border-border px-3 font-medium text-ink transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          disabled={currentPage >= totalPages}
          onClick={() => onPageChange(currentPage + 1)}
          type="button"
        >
          Siguiente
        </button>
      </div>
    </nav>
  )
}
