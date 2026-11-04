import { useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import StudentCourseTabs from '../components/student/StudentCourseTabs'
import StudentEnrollmentCard from '../components/student/StudentEnrollmentCard'
import StudentSidebar from '../components/student/StudentSidebar'
import Navbar from '../components/layout/Navbar'
import { useAuth } from '../context/useAuth'
import { ROLES, getHomeForRole } from '../utils/roles'
import { getMyEnrollments } from '../services/courseService'

export default function StudentDashboard() {
  const { user, isAuthenticated } = useAuth()
  const [activeItem, setActiveItem] = useState('progress')
  const [activeCourseTab, setActiveCourseTab] = useState('ACTIVE')
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [enrollments, setEnrollments] = useState([])
  const [enrollmentsLoading, setEnrollmentsLoading] = useState(true)
  const [enrollmentsError, setEnrollmentsError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const fullName = [user?.firstName, user?.lastName]
    .map((name) => name?.trim())
    .filter(Boolean)
    .join(' ')

  useEffect(() => {
    if (!isAuthenticated || user?.role !== ROLES.STUDENT) return undefined

    let active = true
    getMyEnrollments()
      .then((result) => {
        if (active) setEnrollments(result)
      })
      .catch(() => {
        if (active) setEnrollmentsError(true)
      })
      .finally(() => {
        if (active) setEnrollmentsLoading(false)
      })

    return () => {
      active = false
    }
  }, [isAuthenticated, reloadKey, user?.role])

  const filteredEnrollments = useMemo(
    () => enrollments.filter((enrollment) => {
      const status = enrollment.status?.toUpperCase()
      return activeCourseTab === 'COMPLETED'
        ? ['COMPLETED', 'FINISHED', 'FINALIZED', 'FINALIZADO'].includes(status)
        : !['COMPLETED', 'FINISHED', 'FINALIZED', 'FINALIZADO'].includes(status)
    }),
    [activeCourseTab, enrollments],
  )

  if (!isAuthenticated) {
    return (
      <Navigate
        replace
        to="/login"
        state={{ from: { pathname: '/mi-aprendizaje' } }}
      />
    )
  }
  if (user?.role !== ROLES.STUDENT) {
    return <Navigate replace to={getHomeForRole(user?.role)} />
  }

  return (
    <>
      <Navbar
        menuOpen={sidebarOpen}
        onMenuToggle={() => setSidebarOpen((open) => !open)}
        sticky
      />
      <div className="min-h-[calc(100vh-60px)] min-w-0 overflow-x-clip bg-slate-50 text-slate-900 lg:grid lg:grid-cols-[320px_minmax(0,1fr)]">
        <StudentSidebar
          activeItem={activeItem}
          onNavigate={(item) => {
            setActiveItem(item)
            if (item === 'courses') {
              document.getElementById('student-courses')?.scrollIntoView({
                behavior: 'smooth',
                block: 'start',
              })
            } else {
              window.scrollTo({ top: 0, behavior: 'smooth' })
            }
          }}
          mobileOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          user={user}
        />
        <main className="min-h-[calc(100vh-60px)] min-w-0 bg-slate-50 px-4 py-6 sm:px-6 sm:py-7 lg:px-[27px] lg:py-9">
          <section aria-labelledby="student-welcome-title">
            <h1 className="text-[22px] font-bold tracking-tight text-slate-900 lg:text-2xl xl:text-[26px]" id="student-welcome-title">
              {fullName ? `¡Hola, ${fullName}!` : '¡Hola!'}
            </h1>
            <p className="mt-2 text-[13px] text-slate-500 sm:text-sm xl:text-[15px]">
              Acá tenés un resumen de tu progreso y tus cursos.
            </p>
          </section>

          <section
            aria-labelledby="student-courses-title"
            className="mt-8 scroll-mt-16 lg:mt-10"
            id="student-courses"
          >
            <h2 className="text-base font-semibold text-slate-900 lg:text-lg" id="student-courses-title">
              Mis cursos
            </h2>
            <div className="mt-3">
              <StudentCourseTabs activeTab={activeCourseTab} onChange={setActiveCourseTab} />
            </div>
            <div aria-live="polite" className="mt-4">
              {enrollmentsLoading ? (
                <p className="rounded-xl border border-slate-200 bg-white px-4 py-6 text-center text-sm text-slate-500">
                  Cargando tus cursos...
                </p>
              ) : enrollmentsError ? (
                <div className="rounded-xl border border-rose-200 bg-white px-4 py-5 text-center">
                  <p className="text-sm text-rose-700">
                    No pudimos cargar tus cursos en este momento.
                  </p>
                  <button
                    className="mt-2 text-sm font-semibold text-primary hover:underline"
                    onClick={() => {
                      setEnrollmentsError(false)
                      setEnrollmentsLoading(true)
                      setReloadKey((key) => key + 1)
                    }}
                    type="button"
                  >
                    Intentar nuevamente
                  </button>
                </div>
              ) : filteredEnrollments.length > 0 ? (
                <div className="grid gap-4 xl:grid-cols-2">
                  {filteredEnrollments.map((enrollment) => (
                    <StudentEnrollmentCard enrollment={enrollment} key={enrollment.id} />
                  ))}
                </div>
              ) : (
                <p className="rounded-xl border border-slate-200 bg-white px-4 py-6 text-center text-sm text-slate-500">
                  {activeCourseTab === 'COMPLETED'
                    ? 'Todavía no tienes cursos finalizados.'
                    : 'Todavía no tienes cursos cursando. Cuando te inscribas, aparecerán aquí.'}
                </p>
              )}
            </div>
          </section>
        </main>
      </div>
    </>
  )
}
