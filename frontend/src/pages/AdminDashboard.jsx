import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { AlertTriangle, ChevronDown, LogOut, Plus, Search, Trash2, X } from 'lucide-react'
import { useAuth } from '../context/useAuth'
import api from '../services/api'
import { ROLES } from '../utils/roles'
import { getErrorMessage } from '../utils/apiErrors'
import CourseFormModal from '../components/admin/CourseFormModal'
import AdminDashboardSummary from '../components/admin/AdminDashboardSummary'
import {
  AdminSidebar,
  CourseActions,
  CourseStatusSelector,
  CourseThumbnail,
  TablePagination,
} from '../components/admin/AdminDashboardUI'
import {
  ADMIN_SECTIONS,
  COURSE_STATUS_BAR_STYLES,
  COURSE_STATUS_LABELS,
  COURSE_STATUS_VALUES,
  COURSE_STATUSES,
} from '../components/admin/adminDashboardConstants'

const TABLE_PAGE_SIZE = 10

async function requestCourses() {
  const { data } = await api.get('/courses')
  if (!Array.isArray(data)) {
    throw new Error('La respuesta del servidor no tiene el formato esperado.')
  }
  return data
}

async function requestUsers() {
  const { data } = await api.get('/admin/users')
  if (!Array.isArray(data)) {
    throw new Error('La respuesta del servidor no tiene el formato esperado.')
  }
  return data
}

async function requestDashboardStats() {
  const { data } = await api.get('/admin/dashboard-stats')
  if (
    !data ||
    typeof data.totalCourses !== 'number' ||
    typeof data.totalEnrollments !== 'number' ||
    typeof data.completedCoursesAvailable !== 'boolean'
  ) {
    throw new Error('La respuesta de estadísticas no tiene el formato esperado.')
  }
  return data
}

const ACTIVE_SECTION_STORAGE_KEY = 'skillup_admin_active_section'
const COURSE_PAGE_STORAGE_KEY = 'skillup_admin_courses_page'
const USER_PAGE_STORAGE_KEY = 'skillup_admin_users_page'

function getInitialPage(storageKey) {
  try {
    const savedPage = Number(localStorage.getItem(storageKey))
    return Number.isInteger(savedPage) && savedPage > 0 ? savedPage : 1
  } catch {
    return 1
  }
}

function getInitialAdminSection() {
  try {
    const savedSection = localStorage.getItem(ACTIVE_SECTION_STORAGE_KEY)
    return ADMIN_SECTIONS.some(({ id }) => id === savedSection) ? savedSection : 'summary'
  } catch {
    return 'summary'
  }
}

export default function AdminDashboard() {
  const { user, isAuthenticated, logout } = useAuth()
  const [profileMenuOpen, setProfileMenuOpen] = useState(false)
  const profileMenuRef = useRef(null)
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [users, setUsers] = useState([])
  const [loadingUsers, setLoadingUsers] = useState(true)
  const [usersError, setUsersError] = useState('')
  const [dashboardStats, setDashboardStats] = useState(null)
  const [loadingDashboardStats, setLoadingDashboardStats] = useState(true)
  const [dashboardStatsError, setDashboardStatsError] = useState('')
  const [statusUpdating, setStatusUpdating] = useState({})
  const [statusErrors, setStatusErrors] = useState({})
  const [activeSection, setActiveSection] = useState(getInitialAdminSection)
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [coursePage, setCoursePage] = useState(() => getInitialPage(COURSE_PAGE_STORAGE_KEY))
  const [userPage, setUserPage] = useState(() => getInitialPage(USER_PAGE_STORAGE_KEY))
  const [modalCourse, setModalCourse] = useState(null)
  const [creating, setCreating] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [deletingCourseId, setDeletingCourseId] = useState(null)
  const [courseToDelete, setCourseToDelete] = useState(null)
  const [deleteError, setDeleteError] = useState('')
  const [actionMessage, setActionMessage] = useState('')

  useEffect(() => {
    try {
      localStorage.setItem(ACTIVE_SECTION_STORAGE_KEY, activeSection)
    } catch {
      // Keep navigation usable when browser storage is unavailable.
    }
  }, [activeSection])

  useEffect(() => {
    if (!profileMenuOpen) return undefined

    const closeOnOutsideClick = (event) => {
      if (!profileMenuRef.current?.contains(event.target)) {
        setProfileMenuOpen(false)
      }
    }
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setProfileMenuOpen(false)
    }

    document.addEventListener('mousedown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [profileMenuOpen])

  const loadCourses = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      setCourses(await requestCourses())
    } catch (error) {
      setLoadError(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [])

  const loadUsers = useCallback(async () => {
    setLoadingUsers(true)
    setUsersError('')
    try {
      setUsers(await requestUsers())
    } catch (error) {
      setUsersError(getErrorMessage(error))
    } finally {
      setLoadingUsers(false)
    }
  }, [])

  const loadDashboardStats = useCallback(async () => {
    setLoadingDashboardStats(true)
    setDashboardStatsError('')
    try {
      setDashboardStats(await requestDashboardStats())
    } catch (error) {
      setDashboardStatsError(getErrorMessage(error))
    } finally {
      setLoadingDashboardStats(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    requestCourses()
      .then((data) => {
        if (active) setCourses(data)
      })
      .catch((error) => {
        if (active) setLoadError(getErrorMessage(error))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    requestUsers()
      .then((data) => {
        if (active) setUsers(data)
      })
      .catch((error) => {
        if (active) setUsersError(getErrorMessage(error))
      })
      .finally(() => {
        if (active) setLoadingUsers(false)
      })

    requestDashboardStats()
      .then((data) => {
        if (active) setDashboardStats(data)
      })
      .catch((error) => {
        if (active) setDashboardStatsError(getErrorMessage(error))
      })
      .finally(() => {
        if (active) setLoadingDashboardStats(false)
      })

    return () => {
      active = false
    }
  }, [])

  const filteredCourses = useMemo(() => {
    const normalizedSearch = search.trim().toLocaleLowerCase()
    return courses.filter((course) =>
      (!normalizedSearch ||
        `${course.title ?? ''} ${course.category ?? ''}`
          .toLocaleLowerCase()
          .includes(normalizedSearch)) &&
      (!categoryFilter ||
        (course.category?.trim() || 'Sin categoría') === categoryFilter) &&
      (!statusFilter || course.status === statusFilter),
    )
  }, [categoryFilter, courses, search, statusFilter])
  const coursePageCount = Math.max(1, Math.ceil(filteredCourses.length / TABLE_PAGE_SIZE))
  const visibleCoursePage = Math.min(coursePage, coursePageCount)
  const paginatedCourses = useMemo(
    () => filteredCourses.slice(
      (visibleCoursePage - 1) * TABLE_PAGE_SIZE,
      visibleCoursePage * TABLE_PAGE_SIZE,
    ),
    [filteredCourses, visibleCoursePage],
  )
  const userPageCount = Math.max(1, Math.ceil(users.length / TABLE_PAGE_SIZE))
  const visibleUserPage = Math.min(userPage, userPageCount)
  const paginatedUsers = useMemo(
    () => users.slice(
      (visibleUserPage - 1) * TABLE_PAGE_SIZE,
      visibleUserPage * TABLE_PAGE_SIZE,
    ),
    [users, visibleUserPage],
  )

  useEffect(() => {
    if (loading || loadError) return
    try {
      localStorage.setItem(COURSE_PAGE_STORAGE_KEY, String(visibleCoursePage))
    } catch {
      // Keep pagination usable when browser storage is unavailable.
    }
  }, [loadError, loading, visibleCoursePage])

  useEffect(() => {
    if (loadingUsers || usersError) return
    try {
      localStorage.setItem(USER_PAGE_STORAGE_KEY, String(visibleUserPage))
    } catch {
      // Keep pagination usable when browser storage is unavailable.
    }
  }, [loadingUsers, usersError, visibleUserPage])

  const availableCategories = useMemo(
    () => [...new Set(courses.map((course) => course.category?.trim() || 'Sin categoría'))]
      .sort((categoryA, categoryB) => categoryA.localeCompare(categoryB)),
    [courses],
  )
  const categoryBreakdown = useMemo(() => {
    const counts = courses.reduce((result, course) => {
      const category = course.category?.trim() || 'Sin categoría'
      result[category] = (result[category] ?? 0) + 1
      return result
    }, {})
    return Object.entries(counts).sort(([, countA], [, countB]) => countB - countA)
  }, [courses])
  const courseStatusBreakdown = useMemo(() => {
    const counts = courses.reduce((result, course) => {
      const label = COURSE_STATUS_LABELS[course.status]
      if (label) result[label] = (result[label] ?? 0) + 1
      return result
    }, {})
    return COURSE_STATUSES
      .filter((status) => counts[status] > 0)
      .map((status) => [status, counts[status]])
  }, [courses])

  if (!isAuthenticated) return <Navigate replace to="/login" />
  if (user?.role !== ROLES.ADMIN) return <Navigate replace to="/" />

  const updateCourseStatus = async (courseId, statusLabel) => {
    const requestedStatus = COURSE_STATUS_VALUES[statusLabel]
    const previousStatus = courses.find((course) => course.id === courseId)?.status
    if (!requestedStatus || previousStatus === requestedStatus) return

    setStatusUpdating((current) => ({ ...current, [courseId]: true }))
    setStatusErrors((current) => ({ ...current, [courseId]: '' }))
    setCourses((current) =>
      current.map((course) =>
        course.id === courseId ? { ...course, status: requestedStatus } : course,
      ),
    )
    try {
      const { data } = await api.patch(`/courses/${courseId}/status`, {
        status: requestedStatus,
      })
      if (!data || !COURSE_STATUS_LABELS[data.status]) {
        throw new Error('El servidor devolvió un estado de curso no válido.')
      }
      setCourses((current) =>
        current.map((course) =>
          course.id === courseId ? { ...course, status: data.status } : course,
        ),
      )
    } catch (error) {
      setCourses((current) =>
        current.map((course) =>
          course.id === courseId ? { ...course, status: previousStatus } : course,
        ),
      )
      setStatusErrors((current) => ({
        ...current,
        [courseId]: getErrorMessage(error),
      }))
    } finally {
      setStatusUpdating((current) => ({ ...current, [courseId]: false }))
    }
  }

  const openEditModal = async (courseId) => {
    setDetailError('')
    setActionMessage('')
    setCreating(false)
    setModalCourse({ loading: true })
    try {
      const { data } = await api.get(`/courses/${courseId}`)
      if (!data || typeof data !== 'object' || Array.isArray(data)) {
        throw new Error('El servidor devolvió información de curso no válida.')
      }
      setModalCourse({ ...data, id: data.id ?? courseId })
    } catch (error) {
      setModalCourse(null)
      setDetailError(`No se pudo cargar el curso para editarlo: ${getErrorMessage(error)}`)
    }
  }

  const handleDelete = async () => {
    if (!courseToDelete || deletingCourseId) return
    const course = courseToDelete
    setDeletingCourseId(course.id)
    setDeleteError('')
    setActionMessage('')
    try {
      await api.delete(`/courses/${course.id}`)
      setCourses((current) => current.filter((item) => item.id !== course.id))
      setActionMessage('El curso se eliminó correctamente.')
      setCourseToDelete(null)
      loadDashboardStats()
    } catch (error) {
      setDeleteError(getErrorMessage(error))
    } finally {
      setDeletingCourseId(null)
    }
  }

  const handleSaved = (savedCourse, savedCourseId) => {
    setModalCourse(null)
    setCreating(false)
    setActiveSection('courses')
    setActionMessage('El curso se guardó correctamente.')
    loadDashboardStats()
    if (savedCourseId) {
      setCourses((current) =>
        current.map((item) =>
          item.id === savedCourseId ? { ...item, ...savedCourse, id: savedCourseId } : item,
        ),
      )
    } else if (savedCourse?.id) {
      setCourses((current) => [
        { ...savedCourse, id: savedCourse.id },
        ...current.filter((item) => item.id !== savedCourse.id),
      ])
    } else {
      loadCourses()
    }
  }

  return (
    <div className="min-h-screen w-full min-w-0 overflow-x-clip bg-background text-ink md:flex">
      <AdminSidebar activeSection={activeSection} onNavigate={setActiveSection} />
      <main className="min-w-0 flex-1">
        <header className="flex min-w-0 items-center justify-between gap-3 border-b border-border bg-white px-4 py-3 sm:px-6 sm:py-4 lg:px-8">
          <p className="min-w-0 truncate text-xs font-medium text-muted sm:text-sm">
            SkillUp Campus / Administración
          </p>
          <div className="relative" ref={profileMenuRef}>
            <button
              aria-expanded={profileMenuOpen}
              aria-haspopup="menu"
              aria-label="Abrir menú de cuenta"
              className="flex min-h-11 items-center gap-1.5 rounded-full p-1 text-primary transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
              onClick={() => setProfileMenuOpen((open) => !open)}
              type="button"
            >
              <span
                aria-hidden="true"
                className="flex size-10 items-center justify-center rounded-full bg-secondary text-sm font-bold"
              >
                {(user?.firstName?.[0] ?? 'A').toUpperCase()}
              </span>
              <ChevronDown aria-hidden="true" size={16} />
            </button>
            {profileMenuOpen && (
              <div
                className="absolute right-0 top-full z-30 mt-2 min-w-48 rounded-xl border border-border bg-white p-1.5 shadow-lg"
                role="menu"
              >
                <button
                  className="flex min-h-11 w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-medium text-ink transition hover:bg-slate-50"
                  onClick={logout}
                  role="menuitem"
                  type="button"
                >
                  <LogOut aria-hidden="true" size={16} />
                  Cerrar sesión
                </button>
              </div>
            )}
          </div>
        </header>

        <div className="mx-auto w-full max-w-7xl min-w-0 space-y-6 px-4 py-6 sm:space-y-7 sm:px-6 sm:py-8 lg:px-8">
          <section
            className="flex min-w-0 flex-col justify-between gap-4 sm:flex-row sm:items-center"
          >
            <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
              {activeSection === 'summary'
                ? 'Resumen general'
                : activeSection === 'courses'
                  ? 'Gestión de cursos'
                  : 'Gestión de usuarios'}
            </h1>
            {activeSection === 'courses' && (
              <button
                className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-primary-hover sm:w-auto"
                onClick={() => {
                  setDetailError('')
                  setModalCourse(null)
                  setCreating(true)
                }}
                type="button"
              >
                <Plus size={18} />
                Crear curso
              </button>
            )}
          </section>

          {activeSection === 'summary' && (
            <AdminDashboardSummary
              categoryBreakdown={categoryBreakdown}
              courseStatusBreakdown={courseStatusBreakdown}
              courses={courses}
              coursesError={loadError}
              dashboardStats={dashboardStats}
              dashboardStatsError={dashboardStatsError}
              loadDashboardStats={loadDashboardStats}
              loadingCourses={loading}
              loadingDashboardStats={loadingDashboardStats}
              loadingUsers={loadingUsers}
              users={users}
              usersError={usersError}
            />
          )}

          {activeSection === 'courses' && <section
            className="overflow-hidden rounded-xl border border-border bg-white"
          >
            <div className="space-y-4 border-b border-border px-4 py-4 sm:px-5">
              <h2 className="text-lg font-bold">Cursos</h2>
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-[minmax(14rem,1.5fr)_minmax(10rem,1fr)_minmax(10rem,1fr)_auto] xl:items-end">
                <label className="relative block min-w-0">
                  <span className="mb-1.5 block text-xs font-medium text-muted">Buscar</span>
                  <Search
                    aria-hidden="true"
                    className="absolute left-3 top-[calc(50%+0.4rem)] -translate-y-1/2 text-muted"
                    size={17}
                  />
                  <input
                    className="w-full rounded-xl border border-border py-2.5 pl-10 pr-3 text-sm outline-none transition placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/15"
                    onChange={(event) => {
                      setSearch(event.target.value)
                      setCoursePage(1)
                    }}
                    placeholder="Nombre o categoría..."
                    type="search"
                    value={search}
                  />
                </label>
                <label className="block min-w-0">
                  <span className="mb-1.5 block text-xs font-medium text-muted">Categoría</span>
                  <select
                    aria-label="Filtrar cursos por categoría"
                    className="w-full rounded-xl border border-border bg-white px-3 py-2.5 text-sm text-ink outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15"
                    onChange={(event) => {
                      setCategoryFilter(event.target.value)
                      setCoursePage(1)
                    }}
                    value={categoryFilter}
                  >
                    <option value="">Todas las categorías</option>
                    {availableCategories.map((category) => (
                      <option key={category} value={category}>{category}</option>
                    ))}
                  </select>
                </label>
                <label className="block min-w-0">
                  <span className="mb-1.5 block text-xs font-medium text-muted">Estado</span>
                  <select
                    aria-label="Filtrar cursos por estado"
                    className="w-full rounded-xl border border-border bg-white px-3 py-2.5 text-sm text-ink outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15"
                    onChange={(event) => {
                      setStatusFilter(event.target.value)
                      setCoursePage(1)
                    }}
                    value={statusFilter}
                  >
                    <option value="">Todos los estados</option>
                    {COURSE_STATUSES.map((status) => (
                      <option key={status} value={COURSE_STATUS_VALUES[status]}>{status}</option>
                    ))}
                  </select>
                </label>
                {(search || categoryFilter || statusFilter) && (
                  <button
                    className="inline-flex min-h-11 items-center justify-center gap-1.5 rounded-xl border border-border px-3 text-sm font-medium text-muted transition hover:bg-slate-50 hover:text-ink"
                    onClick={() => {
                      setSearch('')
                      setCategoryFilter('')
                      setStatusFilter('')
                      setCoursePage(1)
                    }}
                    type="button"
                  >
                    <X aria-hidden="true" size={15} />
                    Limpiar
                  </button>
                )}
              </div>
            </div>

            {actionMessage && (
              <p aria-live="polite" className="px-5 py-3 text-sm text-ink sm:px-6">
                {actionMessage}
              </p>
            )}
            {detailError && (
              <p className="px-5 py-3 text-sm text-danger sm:px-6" role="alert">
                {detailError}
              </p>
            )}

            {loading ? (
              <div className="px-6 py-14 text-center text-sm text-muted">Cargando cursos...</div>
            ) : loadError ? (
              <div className="px-6 py-12 text-center">
                <p className="text-sm text-danger" role="alert">
                  No se pudieron cargar los cursos: {loadError}
                </p>
                <button
                  className="mt-4 rounded-lg border border-border px-4 py-2 text-sm font-semibold text-primary hover:bg-secondary"
                  onClick={loadCourses}
                  type="button"
                >
                  Reintentar
                </button>
              </div>
            ) : (
              <div>
                <div className="hidden md:block">
                  <table className="w-full table-fixed text-left text-xs sm:text-sm">
                    <colgroup>
                      <col className="w-[46%] lg:w-[50%]" />
                      <col className="w-[20%] lg:w-[19%]" />
                      <col className="w-[18%] lg:w-[17%]" />
                      <col className="w-[16%] lg:w-[14%]" />
                    </colgroup>
                    <thead className="border-b border-border bg-slate-50/70 text-[10px] uppercase tracking-[0.08em] text-muted sm:text-[11px]">
                      <tr>
                        <th className="px-3 py-3 font-semibold lg:px-4" scope="col">Nombre</th>
                        <th className="px-2 py-3 font-semibold lg:px-3" scope="col">Categoría</th>
                        <th className="px-2 py-3 font-semibold lg:px-3" scope="col">Estado</th>
                        <th className="px-3 py-3 text-right font-semibold lg:px-4" scope="col">Acciones</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {paginatedCourses.map((course) => (
                        <tr className="transition-colors hover:bg-slate-50/70" key={course.id}>
                          <td className="px-3 py-2.5 lg:px-4">
                            <div className="flex min-w-0 items-center gap-3">
                              <CourseThumbnail course={course} />
                              <div className="min-w-0">
                                <p className="truncate text-xs font-semibold text-ink sm:text-sm" title={course.title}>
                                  {course.title}
                                </p>
                              </div>
                            </div>
                          </td>
                          <td className="px-2 py-2.5 lg:px-3">
                            <span className="block truncate text-muted" title={course.category || 'Sin categoría'}>
                              {course.category || 'Sin categoría'}
                            </span>
                          </td>
                          <td className="px-2 py-2.5 lg:px-3">
                            <CourseStatusSelector
                              courseId={course.id}
                              disabled={Boolean(statusUpdating[course.id])}
                              error={statusErrors[course.id]}
                              onChange={updateCourseStatus}
                              status={COURSE_STATUS_LABELS[course.status] ?? ''}
                            />
                          </td>
                          <td className="px-3 py-2.5 lg:px-4">
                            <CourseActions
                              course={course}
                              deleting={deletingCourseId === course.id}
                              onDelete={(selectedCourse) => {
                                setDeleteError('')
                                setCourseToDelete(selectedCourse)
                              }}
                              onEdit={openEditModal}
                            />
                          </td>
                        </tr>
                      ))}
                      {filteredCourses.length === 0 && (
                        <tr>
                          <td className="px-4 py-12 text-center text-sm text-muted" colSpan="4">
                            {courses.length
                              ? 'No hay cursos que coincidan con la búsqueda.'
                              : 'Todavía no hay cursos para mostrar.'}
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
                <div className="divide-y divide-slate-100 md:hidden">
                  {paginatedCourses.map((course) => (
                    <article className="space-y-3 px-4 py-4" key={course.id}>
                      <div className="flex min-w-0 items-center gap-3">
                        <CourseThumbnail course={course} />
                        <div className="min-w-0">
                          <h3 className="truncate text-sm font-semibold text-ink" title={course.title}>
                            {course.title}
                          </h3>
                        </div>
                      </div>
                      <div className="flex min-w-0 flex-wrap items-center justify-between gap-x-3 gap-y-2 pl-[52px]">
                        <div className="min-w-0 flex-1">
                          <p className="text-[10px] font-semibold uppercase tracking-wide text-muted">Categoría</p>
                          <p className="truncate text-xs text-ink" title={course.category || 'Sin categoría'}>
                            {course.category || 'Sin categoría'}
                          </p>
                        </div>
                        <div className="shrink-0">
                          <p className="mb-1 text-[10px] font-semibold uppercase tracking-wide text-muted">
                            Estado
                          </p>
                          <CourseStatusSelector
                            courseId={course.id}
                            disabled={Boolean(statusUpdating[course.id])}
                            error={statusErrors[course.id]}
                            onChange={updateCourseStatus}
                            status={COURSE_STATUS_LABELS[course.status] ?? ''}
                          />
                        </div>
                      </div>
                      <div className="flex items-center justify-between pl-[52px]">
                        <span className="text-[10px] font-semibold uppercase tracking-wide text-muted">Acciones</span>
                        <CourseActions
                          course={course}
                          deleting={deletingCourseId === course.id}
                          onDelete={(selectedCourse) => {
                            setDeleteError('')
                            setCourseToDelete(selectedCourse)
                          }}
                          onEdit={openEditModal}
                        />
                      </div>
                    </article>
                  ))}
                  {filteredCourses.length === 0 && (
                    <p className="px-4 py-12 text-center text-sm text-muted">
                      {courses.length
                        ? 'No hay cursos que coincidan con la búsqueda.'
                        : 'Todavía no hay cursos para mostrar.'}
                    </p>
                  )}
                </div>
                <TablePagination
                  currentPage={visibleCoursePage}
                  label="cursos"
                  onPageChange={setCoursePage}
                  pageSize={TABLE_PAGE_SIZE}
                  totalItems={filteredCourses.length}
                />
              </div>
            )}
          </section>}

          {activeSection === 'users' && <section
            className="overflow-hidden rounded-xl border border-border bg-white"
          >
            <div className="flex flex-col gap-1 border-b border-border px-5 py-4 sm:px-6">
              <h2 className="text-lg font-bold">Usuarios</h2>
              <p className="text-xs text-muted">
                El estado de la cuenta no está disponible en los datos actuales.
              </p>
            </div>
            <div
              aria-label="Tabla de usuarios; desplázate horizontalmente para ver todas las columnas"
              className="max-w-full overflow-x-auto overscroll-x-contain"
              role="region"
              tabIndex={0}
            >
              <table className="w-full min-w-[620px] text-left text-sm">
                <thead className="border-b border-border bg-slate-50/70 text-xs uppercase tracking-wide text-muted">
                  <tr>
                    <th className="px-5 py-3.5 font-semibold sm:px-6" scope="col">Nombre</th>
                    <th className="px-5 py-3.5 font-semibold sm:px-6" scope="col">Email</th>
                    <th className="px-5 py-3.5 font-semibold sm:px-6" scope="col">Rol</th>
                    <th className="px-5 py-3.5 font-semibold sm:px-6" scope="col">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {loadingUsers ? (
                    <tr>
                      <td className="px-5 py-8 text-center text-sm text-muted sm:px-6" colSpan="4">
                        Cargando usuarios...
                      </td>
                    </tr>
                  ) : usersError ? (
                    <tr>
                      <td className="px-5 py-8 text-center sm:px-6" colSpan="4">
                        <p className="text-sm text-danger" role="alert">
                          No se pudieron cargar los usuarios: {usersError}
                        </p>
                        <button
                          className="mt-3 rounded-lg border border-border px-4 py-2 text-sm font-semibold text-primary hover:bg-secondary"
                          onClick={loadUsers}
                          type="button"
                        >
                          Reintentar
                        </button>
                      </td>
                    </tr>
                  ) : users.length ? (
                    paginatedUsers.map((listedUser) => (
                      <tr className="border-b border-border last:border-0" key={listedUser.id}>
                        <td className="px-5 py-4 font-medium text-ink sm:px-6">
                          {[listedUser.firstName, listedUser.lastName].filter(Boolean).join(' ') ||
                            'Sin nombre'}
                        </td>
                        <td className="px-5 py-4 text-muted sm:px-6">{listedUser.email}</td>
                        <td className="px-5 py-4 text-muted sm:px-6">
                          {listedUser.role || 'Sin rol'}
                        </td>
                        <td className="px-5 py-4 sm:px-6">
                          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500">
                            No registrado
                          </span>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td className="px-5 py-8 text-center text-sm text-muted sm:px-6" colSpan="4">
                        Todavía no hay usuarios para mostrar.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            {!loadingUsers && !usersError && (
              <TablePagination
                currentPage={visibleUserPage}
                label="usuarios"
                onPageChange={setUserPage}
                pageSize={TABLE_PAGE_SIZE}
                totalItems={users.length}
              />
            )}
          </section>}
        </div>
      </main>

      {creating && (
        <CourseFormModal course={null} onClose={() => setCreating(false)} onSaved={handleSaved} />
      )}
      {modalCourse && !modalCourse.loading && (
        <CourseFormModal
          course={modalCourse}
          onClose={() => setModalCourse(null)}
          onSaved={handleSaved}
        />
      )}
      {modalCourse?.loading && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
          <div className="rounded-xl bg-white px-6 py-5 text-sm text-muted" role="status">
            Cargando información del curso...
          </div>
        </div>
      )}
      {courseToDelete && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !deletingCourseId) {
              setCourseToDelete(null)
              setDeleteError('')
            }
          }}
          role="presentation"
        >
          <section
            aria-labelledby="delete-course-title"
            aria-modal="true"
            className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl"
            role="alertdialog"
          >
            <div className="flex items-start gap-4">
              <span className="flex size-11 shrink-0 items-center justify-center rounded-full bg-red-50 text-danger">
                <AlertTriangle aria-hidden="true" size={21} />
              </span>
              <div className="min-w-0">
                <h2 className="text-lg font-bold text-ink" id="delete-course-title">
                  Eliminar curso
                </h2>
                <p className="mt-2 break-words text-sm leading-6 text-muted">
                  ¿Quieres eliminar «{courseToDelete.title}»? Esta acción no se puede deshacer.
                </p>
              </div>
              <button
                aria-label="Cerrar confirmación"
                className="ml-auto flex size-9 shrink-0 items-center justify-center rounded-lg text-muted transition hover:bg-slate-100 hover:text-ink disabled:opacity-50"
                disabled={Boolean(deletingCourseId)}
                onClick={() => {
                  setCourseToDelete(null)
                  setDeleteError('')
                }}
                type="button"
              >
                <X aria-hidden="true" size={18} />
              </button>
            </div>
            {deleteError && (
              <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-danger" role="alert">
                {deleteError}
              </p>
            )}
            <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                className="min-h-11 rounded-lg border border-border px-4 py-2 text-sm font-semibold text-ink transition hover:bg-slate-50 disabled:opacity-50"
                disabled={Boolean(deletingCourseId)}
                onClick={() => {
                  setCourseToDelete(null)
                  setDeleteError('')
                }}
                type="button"
              >
                Cancelar
              </button>
              <button
                className="inline-flex min-h-11 items-center justify-center gap-2 rounded-lg bg-danger px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={Boolean(deletingCourseId)}
                onClick={handleDelete}
                type="button"
              >
                <Trash2 aria-hidden="true" size={16} />
                {deletingCourseId ? 'Eliminando...' : 'Eliminar curso'}
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
