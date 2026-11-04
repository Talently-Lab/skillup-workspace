import { BookOpen, GraduationCap, UsersRound } from 'lucide-react'
import { COURSE_STATUS_BAR_STYLES } from './adminDashboardConstants'

export default function AdminDashboardSummary({
  dashboardStats,
  dashboardStatsError,
  loadDashboardStats,
  loadingDashboardStats,
  loadingUsers,
  users,
  usersError,
  loadingCourses,
  coursesError,
  courses,
  categoryBreakdown,
  courseStatusBreakdown,
}) {
  const cards = [
    {
      label: 'Cursos registrados',
      value: loadingDashboardStats ? '—' : dashboardStatsError ? '!' : dashboardStats.totalCourses,
      icon: BookOpen,
      detail: 'Cursos creados en la plataforma',
      iconStyle: 'bg-sky-50 text-sky-700',
    },
    {
      label: 'Alumnos registrados',
      value: loadingUsers
        ? '—'
        : usersError
          ? '!'
          : users.filter((listedUser) => listedUser.role?.trim().toUpperCase() === 'ALUMNO').length,
      icon: UsersRound,
      detail: 'Alumnos en la plataforma',
      iconStyle: 'bg-indigo-50 text-indigo-700',
    },
    {
      label: 'Inscripciones totales',
      value: loadingDashboardStats ? '—' : dashboardStatsError ? '!' : dashboardStats.totalEnrollments,
      icon: UsersRound,
      detail: 'Matrículas realizadas',
      iconStyle: 'bg-violet-50 text-violet-700',
    },
    {
      label: 'Cursos terminados',
      value: loadingDashboardStats
        ? '—'
        : dashboardStatsError
          ? '!'
          : dashboardStats.completedCoursesAvailable
            ? dashboardStats.completedCourses
            : 'No disponible',
      icon: GraduationCap,
      detail: 'Cursos finalizados',
      iconStyle: 'bg-emerald-50 text-emerald-700',
    },
  ]

  return (
    <section aria-label="Resumen del panel" className="space-y-5">
      <div className="grid items-stretch gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map(({ label, value, icon: Icon, detail, iconStyle }) => (
          <article
            className="h-full rounded-xl border border-slate-200 bg-white p-4 transition-colors hover:border-slate-300 sm:p-5"
            key={label}
          >
            <div className="flex min-h-[104px] items-start justify-between gap-3">
              <div>
                <p className="text-[13px] font-medium text-slate-500">{label}</p>
                <p className={`mt-2 font-bold leading-none tracking-tight text-slate-900 ${typeof value === 'number' ? 'text-3xl sm:text-[2rem]' : 'text-xl'}`}>
                  {value}
                </p>
                <p className="mt-2 text-xs leading-5 text-slate-500">{detail}</p>
              </div>
              <span className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${iconStyle}`}>
                <Icon aria-hidden="true" size={20} />
              </span>
            </div>
          </article>
        ))}
      </div>
      {!loadingDashboardStats &&
        !dashboardStatsError &&
        !dashboardStats.completedCoursesAvailable && (
          <p className="-mt-2 text-xs leading-5 text-slate-500">
            El sistema aún no registra un estado de finalización para los cursos.
          </p>
        )}
      {dashboardStatsError && (
        <div className="flex flex-col gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-danger sm:flex-row sm:items-center sm:justify-between">
          <p role="alert">No se pudieron cargar los indicadores: {dashboardStatsError}</p>
          <button
            className="min-h-10 shrink-0 rounded-lg border border-red-200 bg-white px-3 font-semibold hover:bg-red-100"
            onClick={loadDashboardStats}
            type="button"
          >
            Reintentar
          </button>
        </div>
      )}

      <div className="grid items-stretch gap-4 xl:grid-cols-2">
        <section className="h-full rounded-xl border border-slate-200 bg-white">
          <div className="border-b border-slate-100 px-4 py-3.5 sm:px-5">
            <h2 className="text-sm font-semibold text-slate-900">Cursos por categoría</h2>
            <p className="mt-0.5 text-xs text-slate-500">
              Distribución de los cursos registrados.
            </p>
          </div>
          <div className="space-y-3.5 p-4 sm:p-5">
            {loadingCourses ? (
              <p className="text-sm text-muted">Cargando análisis de cursos...</p>
            ) : coursesError ? (
              <p className="text-sm text-danger">No se pudo obtener el análisis de cursos.</p>
            ) : categoryBreakdown.length ? (
              categoryBreakdown.slice(0, 6).map(([category, count]) => (
                <div key={category}>
                  <div className="mb-1.5 flex items-center justify-between gap-3 text-xs">
                    <span className="truncate font-medium text-slate-700" title={category}>{category}</span>
                    <span className="shrink-0 font-medium tabular-nums text-slate-500">{count}</span>
                  </div>
                  <div
                    aria-label={`${category}: ${count} cursos`}
                    className="h-1.5 overflow-hidden rounded-full bg-slate-100"
                    role="img"
                  >
                    <div
                      className="h-full rounded-full bg-sky-200"
                      style={{ width: `${(count / courses.length) * 100}%` }}
                    />
                  </div>
                </div>
              ))
            ) : (
              <p className="text-sm text-muted">No hay cursos para analizar todavía.</p>
            )}
          </div>
        </section>

        <section className="h-full rounded-xl border border-slate-200 bg-white">
          <div className="border-b border-slate-100 px-4 py-3.5 sm:px-5">
            <h2 className="text-sm font-semibold text-slate-900">Estado de los cursos</h2>
            <p className="mt-0.5 text-xs text-slate-500">
              Cursos agrupados por su estado actual.
            </p>
          </div>
          <div className="space-y-3.5 p-4 sm:p-5">
            {loadingCourses ? (
              <p className="text-sm text-muted">Cargando estados de cursos...</p>
            ) : coursesError ? (
              <p className="text-sm text-danger">No se pudieron obtener los estados de cursos.</p>
            ) : courseStatusBreakdown.length ? (
              courseStatusBreakdown.map(([status, count]) => (
                <div key={status}>
                  <div className="mb-1.5 flex items-center justify-between gap-3 text-xs">
                    <span className="font-medium text-slate-700">{status}</span>
                    <span className="font-medium tabular-nums text-slate-500">{count}</span>
                  </div>
                  <div
                    aria-label={`${status}: ${count} cursos`}
                    className="h-1.5 overflow-hidden rounded-full bg-slate-100"
                    role="img"
                  >
                    <div
                      className={`h-full rounded-full ${COURSE_STATUS_BAR_STYLES[status]}`}
                      style={{ width: `${(count / courses.length) * 100}%` }}
                    />
                  </div>
                </div>
              ))
            ) : (
              <p className="text-sm text-muted">No hay cursos con estados disponibles para mostrar.</p>
            )}
          </div>
        </section>
      </div>
    </section>
  )
}
