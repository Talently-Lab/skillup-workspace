import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, CheckCircle2, Clock3, Download, Heart, Radio, Signal, Video, X } from 'lucide-react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import {
  addCourseFavorite,
  enrollInCourse,
  getCourse,
  getCourseFavorite,
  getMyEnrollments,
  removeCourseFavorite,
} from '../services/courseService'
import { getErrorMessage } from '../utils/apiErrors'
import { isAlreadyEnrolled } from '../utils/enrollmentErrors'
import { formatMinutes, parseScheduleDays } from '../utils/courseSchedule'

const COURSE_TABS = [
  { id: 'description', label: 'Descripción' },
  { id: 'objectives', label: 'Objetivos' },
  { id: 'content', label: 'Contenidos' },
  { id: 'instructor', label: 'Instructor' },
]

function getModalityInfo(course) {
  const modality = course.modality ?? {}
  const normalizedType = String(modality.type ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
  const schedule = course.schedule ?? {}
  const days = parseScheduleDays(schedule.days)
  const frequency =
    modality.frequency ||
    (days.length
      ? `${days.length} clase${days.length === 1 ? '' : 's'} semanal${days.length === 1 ? '' : 'es'}`
      : '')

  if (normalizedType === 'EN VIVO') {
    return { label: 'En Vivo', frequency, icon: Radio, hasLiveClasses: true }
  }
  if (normalizedType === 'GRABADO') {
    return { label: 'Grabado', frequency: '', icon: Video, hasLiveClasses: false }
  }
  if (normalizedType === 'HIBRIDO') {
    return { label: 'Híbrido', frequency, icon: Radio, hasLiveClasses: true }
  }
  return {
    label: modality.type || 'Modalidad no especificada',
    frequency,
    icon: Radio,
    hasLiveClasses: Boolean(days.length || schedule.startTime || schedule.endTime),
  }
}

function getInstructorName(instructor) {
  if (typeof instructor === 'string') return instructor.trim()
  if (!instructor || typeof instructor !== 'object') return ''
  return [
    instructor.name,
    [instructor.firstName, instructor.lastName].filter(Boolean).join(' '),
  ].find((name) => typeof name === 'string' && name.trim()) ?? ''
}

function getLiveClassUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return ''

  try {
    const url = new URL(value.trim())
    return ['http:', 'https:'].includes(url.protocol) ? url.href : ''
  } catch {
    return ''
  }
}

function getYouTubeEmbedUrl(value) {
  const safeUrl = getLiveClassUrl(value)
  if (!safeUrl) return ''

  try {
    const url = new URL(safeUrl)
    const host = url.hostname.toLowerCase()
    const isYouTubeHost =
      host === 'youtu.be' ||
      host === 'youtube.com' ||
      host.endsWith('.youtube.com') ||
      host === 'youtube-nocookie.com' ||
      host.endsWith('.youtube-nocookie.com')
    if (!isYouTubeHost) return ''

    const videoId =
      host === 'youtu.be'
        ? url.pathname.split('/').filter(Boolean)[0]
        : url.searchParams.get('v') ??
          url.pathname.match(/^\/(?:embed|shorts|live)\/([^/?]+)/)?.[1]
    return videoId && /^[\w-]{11}$/.test(videoId)
      ? `https://www.youtube-nocookie.com/embed/${videoId}`
      : ''
  } catch {
    return ''
  }
}

export default function CourseDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, user } = useAuth()
  const [courseResult, setCourseResult] = useState({ id: null, course: null, error: '' })
  const [activeTab, setActiveTab] = useState('description')
  const [enrolling, setEnrolling] = useState(false)
  const [showCourseComingSoon, setShowCourseComingSoon] = useState(false)
  const [enrollmentStatus, setEnrollmentStatus] = useState({
    courseId: null,
    status: 'idle',
  })
  const [enrollmentError, setEnrollmentError] = useState(
    () => location.state?.enrollmentError ?? '',
  )
  const [favoriteState, setFavoriteState] = useState({
    courseId: null,
    favorited: false,
    status: 'idle',
    error: '',
  })
  const [imageErrorCourseId, setImageErrorCourseId] = useState(null)

  useEffect(() => {
    let active = true
    getCourse(id)
      .then((data) => {
        if (active) setCourseResult({ id, course: data, error: '' })
      })
      .catch((requestError) => {
        if (active) setCourseResult({ id, course: null, error: getErrorMessage(requestError) })
      })
    return () => {
      active = false
    }
  }, [id])

  const loading = courseResult.id !== id
  const course = loading ? null : courseResult.course
  const error = loading ? '' : courseResult.error
  const courseId = course?.id
  const liveClassUrl = getLiveClassUrl(course?.schedule?.liveClassUrl)

  useEffect(() => {
    if (!courseId || !isAuthenticated || user?.role !== 'ALUMNO') return undefined
    let active = true
    getMyEnrollments()
      .then((enrollments) => {
        if (active) {
          setEnrollmentStatus({
            courseId,
            status: enrollments.some(
              (enrollment) => String(enrollment.courseId) === String(courseId),
            ) ? 'enrolled' : 'available',
          })
        }
      })
      .catch(() => {
        if (active) setEnrollmentStatus({ courseId, status: 'error' })
      })
    return () => {
      active = false
    }
  }, [courseId, isAuthenticated, user?.role])

  useEffect(() => {
    if (!showCourseComingSoon) return undefined

    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setShowCourseComingSoon(false)
    }

    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [showCourseComingSoon])

  useEffect(() => {
    if (!course || user?.role !== 'ALUMNO' || !isAuthenticated) return undefined

    let active = true
    getCourseFavorite(course.id)
      .then((result) => {
        if (active) {
          setFavoriteState({
            courseId: course.id,
            favorited: result.favorited,
            status: 'loaded',
            error: '',
          })
        }
      })
      .catch((requestError) => {
        if (active) {
          setFavoriteState({
            courseId: course.id,
            favorited: false,
            status: 'error',
            error: requestError.response?.status === 401 ? 'unauthorized' : 'unavailable',
          })
        }
      })
    return () => {
      active = false
    }
  }, [course, isAuthenticated, user?.role])

  const modality = useMemo(() => (course ? getModalityInfo(course) : null), [course])
  const savedDurationMinutes = Number(course?.schedule?.totalDurationMinutes)
  const durationMinutes = savedDurationMinutes > 0
    ? savedDurationMinutes
    : (Number(course?.durationHours) || 0) * 60
  const instructor = course?.instructor
  const instructorName = getInstructorName(instructor)
  const instructorPhoto = typeof instructor === 'object'
    ? instructor?.imageUrl ?? instructor?.photoUrl ?? ''
    : ''
  const objectives = [
    ...(Array.isArray(course?.learningGoals) ? course.learningGoals : []),
    ...(Array.isArray(course?.objectives) ? course.objectives : []),
  ].filter((objective) => typeof objective === 'string' && objective.trim())
  const modules = Array.isArray(course?.contentModules) ? course.contentModules : []
  const courseIsActive = course?.isActive !== false
  const currentFavoriteState = favoriteState.courseId === course?.id ? favoriteState : null
  const favorite = currentFavoriteState?.favorited ?? false
  const favoriteLoading =
    currentFavoriteState?.status === 'loading' ||
    (isAuthenticated && user?.role === 'ALUMNO' && !currentFavoriteState)
  const currentEnrollmentStatus = enrollmentStatus.courseId === course?.id
    ? enrollmentStatus.status
    : 'loading'
  const checkingEnrollment = isAuthenticated && user?.role === 'ALUMNO'
    && currentEnrollmentStatus === 'loading'
  const isEnrolled = isAuthenticated && user?.role === 'ALUMNO'
    && currentEnrollmentStatus === 'enrolled'
  const enrollmentStatusError = isAuthenticated && user?.role === 'ALUMNO'
    && currentEnrollmentStatus === 'error'

  const handleEnroll = async () => {
    setEnrollmentError('')
    if (!isAuthenticated) {
      navigate('/registro', {
        state: { pendingEnrollment: { courseId: course.id, course } },
      })
      return
    }
    if (user?.role !== 'ALUMNO') {
      setEnrollmentError('La inscripción está disponible para cuentas de estudiante.')
      return
    }

    setEnrolling(true)
    try {
      await enrollInCourse(course.id)
      navigate('/inscripcion/confirmacion', {
        replace: true,
        state: { courseId: course.id, course, enrollmentConfirmed: true },
      })
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        navigate('/login', {
          state: { pendingEnrollment: { courseId: course.id, course } },
        })
      } else if (isAlreadyEnrolled(requestError)) {
        navigate('/inscripcion/confirmacion', {
          replace: true,
          state: { courseId: course.id, course, alreadyEnrolled: true },
        })
      } else {
        setEnrollmentError(
          requestError.response?.status === 404
            ? 'Este curso ya no está disponible para inscripción.'
            : 'No pudimos completar la inscripción. Inténtalo nuevamente.',
        )
      }
    } finally {
      setEnrolling(false)
    }
  }

  const handleToggleFavorite = async () => {
    if (favoriteLoading) return
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    if (user?.role !== 'ALUMNO') {
      setFavoriteState({
        courseId: course.id,
        favorited: false,
        status: 'error',
        error: 'unavailable',
      })
      return
    }

    setFavoriteState({
      courseId: course.id,
      favorited: favorite,
      status: 'loading',
      error: '',
    })
    try {
      if (favorite) {
        await removeCourseFavorite(course.id)
      } else {
        await addCourseFavorite(course.id)
      }
      setFavoriteState({
        courseId: course.id,
        favorited: !favorite,
        status: 'loaded',
        error: '',
      })
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        navigate('/login', { state: { from: location } })
      } else {
        setFavoriteState({
          courseId: course.id,
          favorited: favorite,
          status: 'error',
          error: 'unavailable',
        })
      }
    }
  }

  if (loading) {
    return <p className="mx-auto max-w-7xl px-5 py-16 text-sm text-muted sm:px-6 md:px-8" role="status">Cargando curso...</p>
  }

  if (error || !course) {
    return (
      <section className="mx-auto max-w-7xl px-5 py-12 sm:px-6 md:px-8">
        <Link className="inline-flex items-center gap-2 text-sm font-medium text-primary" to="/cursos">
          <ArrowLeft aria-hidden="true" size={16} /> Volver al catálogo
        </Link>
        <p className="mt-6 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">
          {error || 'No se encontró el curso solicitado.'}
        </p>
      </section>
    )
  }

  const ModalityIcon = modality.icon

  return (
    <article className="mx-auto max-w-7xl px-5 py-5 sm:px-6 md:px-8 md:py-7">
      <Link className="inline-flex min-h-8 items-center gap-2 text-sm font-medium text-primary hover:text-primary-hover" to="/cursos">
        <ArrowLeft aria-hidden="true" size={16} />
        Volver al catálogo
      </Link>

      <section className="mt-3 grid items-center gap-5 md:grid-cols-[minmax(0,345px)_minmax(0,1fr)] md:gap-8">
        <div className="w-full overflow-hidden rounded-xl bg-secondary md:max-w-[345px]">
          {course.imageUrl && imageErrorCourseId !== course.id ? (
            <img
              className="aspect-[345/205] w-full object-cover"
              src={course.imageUrl}
              alt={course.title}
              onError={() => setImageErrorCourseId(course.id)}
            />
          ) : (
            <div aria-hidden="true" className="flex aspect-[345/205] w-full items-center justify-center bg-blue-50 text-sm font-medium text-primary">
              Imagen no disponible
            </div>
          )}
        </div>

        <div className="flex flex-col items-start">
          {course.category && (
            <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-primary">
              {course.category}
            </span>
          )}
          <h1 className="mt-2 text-2xl font-bold leading-tight text-ink sm:text-3xl">{course.title}</h1>
          {course.summary && <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">{course.summary}</p>}

          <div className="mt-3 flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-muted">
            {course.durationWeeks != null && (
              <span className="inline-flex items-center gap-2">
                <Clock3 aria-hidden="true" className="text-primary" size={16} />
                {course.durationWeeks} semanas
              </span>
            )}
            {durationMinutes > 0 && (
              <span className="inline-flex items-center gap-2">
                <Clock3 aria-hidden="true" className="text-primary" size={16} />
                {formatMinutes(durationMinutes)}
              </span>
            )}
            {course.level && (
              <span className="inline-flex items-center gap-2">
                <Signal aria-hidden="true" className="text-primary" size={16} />
                {course.level}
              </span>
            )}
            <span className="inline-flex items-center gap-2">
              <ModalityIcon aria-hidden="true" className="text-primary" size={16} />
              {modality.label}
            </span>
          </div>
          {modality.frequency && (
            <p className="mt-2 text-sm text-muted">
              {modality.frequency}
              {modality.hasLiveClasses && course.schedule?.days ? ` · ${course.schedule.days}` : ''}
              {modality.hasLiveClasses && (course.schedule?.startTime || course.schedule?.endTime)
                ? ` · ${course.schedule.startTime || '—'} – ${course.schedule.endTime || '—'}`
                : ''}
            </p>
          )}

          <div className="mt-4 flex w-full flex-wrap items-center gap-3">
            {isEnrolled && (
              <>
                <button
                  className="min-h-10 rounded-button bg-primary px-6 py-2 text-sm font-semibold text-white transition hover:bg-primary-hover"
                  onClick={() => setShowCourseComingSoon(true)}
                  type="button"
                >
                  Ir al curso
                </button>
                {liveClassUrl && (
                  <a
                    className="inline-flex min-h-10 items-center rounded-button border border-primary px-5 py-2 text-sm font-semibold text-primary transition hover:bg-blue-50"
                    href={liveClassUrl}
                    rel="noreferrer"
                    target="_blank"
                  >
                    Abrir enlace de la clase
                  </a>
                )}
              </>
            )}
            {!isEnrolled && !checkingEnrollment && (
              <button
                className="min-h-10 rounded-button bg-primary px-6 py-2 text-sm font-semibold text-white transition hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
                disabled={enrolling || !courseIsActive}
                onClick={handleEnroll}
                type="button"
              >
                {!courseIsActive ? 'Curso no disponible' : enrolling ? 'Procesando inscripción...' : 'Inscribirme'}
              </button>
            )}
            {checkingEnrollment && (
              <p className="text-sm text-muted" role="status">Verificando inscripción...</p>
            )}
            {enrollmentStatusError && (
              <p className="text-sm text-amber-700" role="status">
                No pudimos verificar si ya estás inscrito. Si ya tienes acceso, revisa tus cursos.
              </p>
            )}
            <button
              aria-label={favorite ? 'Quitar de favoritos' : 'Agregar a favoritos'}
              aria-pressed={favorite}
              className={`inline-flex size-10 items-center justify-center rounded-lg border border-border bg-white transition hover:border-primary hover:text-primary ${
                favorite ? 'text-primary' : 'text-muted'
              }`}
              aria-busy={favoriteLoading}
              onClick={handleToggleFavorite}
              title={user?.role && user.role !== 'ALUMNO' ? 'Disponible para estudiantes' : undefined}
              type="button"
            >
              <Heart aria-hidden="true" fill={favorite ? 'currentColor' : 'none'} size={18} />
            </button>
          </div>
          {enrollmentError && <p className="mt-3 text-sm text-red-700" role="alert">{enrollmentError}</p>}
        </div>
      </section>

      {showCourseComingSoon && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/50 p-4"
          onClick={(event) => {
            if (event.target === event.currentTarget) setShowCourseComingSoon(false)
          }}
        >
          <section
            aria-labelledby="course-coming-soon-title"
            aria-modal="true"
            className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl"
            role="dialog"
          >
            <div className="flex items-start justify-between gap-4">
              <h2 className="text-lg font-semibold text-slate-900" id="course-coming-soon-title">
                Próximamente
              </h2>
              <button
                aria-label="Cerrar"
                className="flex size-8 shrink-0 items-center justify-center rounded-full text-slate-500 hover:bg-slate-100 hover:text-slate-700"
                onClick={() => setShowCourseComingSoon(false)}
                type="button"
              >
                <X aria-hidden="true" size={18} />
              </button>
            </div>
            <p className="mt-3 text-sm text-slate-600">
              Esta funcionalidad estará disponible próximamente.
            </p>
            <button
              autoFocus
              className="mt-6 min-h-10 w-full rounded-lg bg-primary px-4 text-sm font-semibold text-white transition hover:bg-primary-hover"
              onClick={() => setShowCourseComingSoon(false)}
              type="button"
            >
              Entendido
            </button>
          </section>
        </div>
      )}

      <section className="mt-6 border-b border-border">
        <nav aria-label="Secciones del curso" className="flex gap-7 overflow-x-auto" role="tablist">
          {COURSE_TABS.map(({ id: tabId, label }) => (
            <button
              aria-controls="course-tabpanel"
              aria-selected={activeTab === tabId}
              className={`min-h-12 shrink-0 border-b-2 px-1 text-sm font-semibold transition ${
                activeTab === tabId
                  ? 'border-primary text-primary'
                  : 'border-transparent text-muted hover:text-ink'
              }`}
              id={`course-tab-${tabId}`}
              key={tabId}
              onClick={() => setActiveTab(tabId)}
              role="tab"
              type="button"
            >
              {label}
            </button>
          ))}
        </nav>
      </section>

      <section
        aria-labelledby={`course-tab-${activeTab}`}
        aria-live="polite"
        className="max-w-4xl py-5"
        id="course-tabpanel"
        role="tabpanel"
      >
        {activeTab === 'description' && (
          <div className="space-y-5">
            <h2 className="text-lg font-bold text-ink">¿Qué vas a aprender?</h2>
            <p className="whitespace-pre-line text-sm leading-7 text-muted">
              {course.description || course.summary || 'No hay una descripción disponible.'}
            </p>
            {objectives.length > 0 && (
              <div>
                <h3 className="mb-3 text-base font-semibold text-ink">Lo que aprenderás</h3>
                <ul className="space-y-3">
                  {objectives.map((objective, index) => (
                    <li className="flex items-start gap-3 text-sm leading-6 text-muted" key={`${index}-${objective}`}>
                      <CheckCircle2 aria-hidden="true" className="mt-0.5 shrink-0 text-primary" size={18} />
                      {objective}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {course.syllabusUrl && (
              <a
                className="inline-flex min-h-11 items-center gap-2 rounded-button border border-primary px-4 py-2 text-sm font-semibold text-primary hover:bg-blue-50"
                href={course.syllabusUrl}
                rel="noreferrer"
                target="_blank"
              >
                <Download aria-hidden="true" size={16} />
                Descargar programa
              </a>
            )}
          </div>
        )}

        {activeTab === 'objectives' && (
          <div>
            <h2 className="text-xl font-bold text-ink">Objetivos y competencias</h2>
            {objectives.length ? (
              <ul className="mt-5 space-y-3">
                {objectives.map((objective, index) => (
                  <li className="flex items-start gap-3 text-sm leading-6 text-muted" key={`${index}-${objective}`}>
                    <CheckCircle2 aria-hidden="true" className="mt-0.5 shrink-0 text-primary" size={18} />
                    {objective}
                  </li>
                ))}
              </ul>
            ) : <p className="mt-4 text-sm text-muted">No hay objetivos disponibles para este curso.</p>}
          </div>
        )}

        {activeTab === 'content' && (
          <div>
            <h2 className="text-xl font-bold text-ink">Contenidos del curso</h2>
            {modules.length ? (
              <div className="mt-5 space-y-4">
                {modules.map((module, index) => (
                  <article className="overflow-hidden rounded-xl border border-border bg-white" key={`${module.moduleNumber ?? index}-${module.title}`}>
                    <h3 className="border-b border-border bg-slate-50 px-4 py-3 text-sm font-semibold text-ink">
                      Módulo {module.moduleNumber ?? index + 1}: {module.title}
                    </h3>
                    {getLiveClassUrl(module.videoUrl) && (
                      <div className="border-b border-border p-4">
                        {getYouTubeEmbedUrl(module.videoUrl) ? (
                          <iframe
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                            allowFullScreen
                            className="aspect-video w-full rounded-lg bg-slate-100"
                            loading="lazy"
                            referrerPolicy="strict-origin-when-cross-origin"
                            src={getYouTubeEmbedUrl(module.videoUrl)}
                            title={`Video del módulo ${module.moduleNumber ?? index + 1}`}
                          />
                        ) : (
                          <video
                            className="aspect-video w-full rounded-lg bg-slate-100 object-contain"
                            controls
                            preload="metadata"
                            src={getLiveClassUrl(module.videoUrl)}
                          >
                            Tu navegador no puede reproducir este video.
                          </video>
                        )}
                      </div>
                    )}
                    {Array.isArray(module.topics) && module.topics.length > 0 && (
                      <ul className="space-y-2 px-5 py-4">
                        {module.topics.map((topic, topicIndex) => (
                          <li className="list-inside list-disc text-sm text-muted" key={`${topicIndex}-${topic}`}>{topic}</li>
                        ))}
                      </ul>
                    )}
                  </article>
                ))}
              </div>
            ) : <p className="mt-4 text-sm text-muted">No hay contenidos disponibles para este curso.</p>}
          </div>
        )}

        {activeTab === 'instructor' && (
          <div>
            <h2 className="text-xl font-bold text-ink">Instructor</h2>
            {instructorName ? (
              <div className="mt-5 flex items-center gap-4">
                {instructorPhoto && (
                  <img alt="" className="size-16 rounded-full object-cover" src={instructorPhoto} />
                )}
                <div>
                  <p className="font-semibold text-ink">{instructorName}</p>
                  {instructor?.bio && <p className="mt-1 text-sm leading-6 text-muted">{instructor.bio}</p>}
                </div>
              </div>
            ) : (
              <p className="mt-4 text-sm text-muted">La información del instructor no está disponible.</p>
            )}
          </div>
        )}
      </section>
    </article>
  )
}
