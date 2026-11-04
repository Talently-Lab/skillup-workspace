import { useEffect, useMemo, useState } from 'react'
import { ArrowLeft, CheckCircle2, Clock3, Download, Radio, Signal, Video } from 'lucide-react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { enrollInCourse, getCourse } from '../services/courseService'
import { getErrorMessage } from '../utils/apiErrors'
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

export default function CourseDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, user } = useAuth()
  const [courseResult, setCourseResult] = useState({ id: null, course: null, error: '' })
  const [activeTab, setActiveTab] = useState('description')
  const [enrolling, setEnrolling] = useState(false)
  const [enrollmentMessage, setEnrollmentMessage] = useState('')
  const [enrollmentError, setEnrollmentError] = useState('')

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

  const handleEnroll = async () => {
    setEnrollmentMessage('')
    setEnrollmentError('')
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    if (user?.role !== 'ALUMNO') {
      setEnrollmentError('La inscripción está disponible para cuentas de estudiante.')
      return
    }

    setEnrolling(true)
    try {
      const enrollment = await enrollInCourse(course.id)
      setEnrollmentMessage(`Inscripción realizada para ${enrollment.courseTitle || course.title}.`)
    } catch (requestError) {
      if (requestError.response?.status === 401) {
        navigate('/login', { state: { from: location } })
      } else {
        setEnrollmentError(getErrorMessage(requestError))
      }
    } finally {
      setEnrolling(false)
    }
  }

  if (loading) {
    return <p className="mx-auto max-w-7xl px-4 py-16 text-sm text-muted" role="status">Cargando curso...</p>
  }

  if (error || !course) {
    return (
      <section className="mx-auto max-w-7xl px-4 py-12">
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
    <article className="mx-auto max-w-7xl px-4 py-6 sm:py-10">
      <Link className="inline-flex min-h-10 items-center gap-2 text-sm font-medium text-primary hover:text-primary-hover" to="/cursos">
        <ArrowLeft aria-hidden="true" size={16} />
        Volver al catálogo
      </Link>

      <section className="mt-4 grid items-start gap-8 lg:grid-cols-[minmax(0,1.1fr)_minmax(20rem,0.9fr)] lg:gap-12">
        <div className="overflow-hidden rounded-2xl bg-secondary">
          {course.imageUrl ? (
            <img className="aspect-[3/2] w-full object-cover" src={course.imageUrl} alt={course.title} />
          ) : (
            <div aria-hidden="true" className="aspect-[3/2] w-full" />
          )}
        </div>

        <div className="flex flex-col items-start">
          {course.category && (
            <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-primary">
              {course.category}
            </span>
          )}
          <h1 className="mt-3 text-3xl font-bold leading-tight text-ink sm:text-4xl">{course.title}</h1>
          {course.summary && <p className="mt-3 text-base leading-7 text-muted">{course.summary}</p>}

          <div className="mt-6 grid w-full gap-3 sm:grid-cols-2">
            {course.durationWeeks != null && (
              <div className="flex items-center gap-3 rounded-xl border border-border bg-white p-3.5">
                <Clock3 aria-hidden="true" className="shrink-0 text-primary" size={19} />
                <span className="text-sm text-ink"><strong>{course.durationWeeks} semanas</strong> estimadas</span>
              </div>
            )}
            {durationMinutes > 0 && (
              <div className="flex items-center gap-3 rounded-xl border border-border bg-white p-3.5">
                <Clock3 aria-hidden="true" className="shrink-0 text-primary" size={19} />
                <span className="text-sm text-ink"><strong>{formatMinutes(durationMinutes)}</strong> de carga total</span>
              </div>
            )}
            {course.level && (
              <div className="flex items-center gap-3 rounded-xl border border-border bg-white p-3.5">
                <Signal aria-hidden="true" className="shrink-0 text-primary" size={19} />
                <span className="text-sm text-ink">Nivel <strong>{course.level}</strong></span>
              </div>
            )}
            <div className="flex items-center gap-3 rounded-xl border border-border bg-white p-3.5">
              <ModalityIcon aria-hidden="true" className="shrink-0 text-primary" size={19} />
              <span className="text-sm text-ink">Modalidad <strong>{modality.label}</strong></span>
            </div>
            {modality.frequency && (
              <div className="flex items-center gap-3 rounded-xl border border-border bg-white p-3.5 sm:col-span-2">
                <Radio aria-hidden="true" className="shrink-0 text-primary" size={19} />
                <span className="text-sm text-ink">Frecuencia <strong>{modality.frequency}</strong></span>
              </div>
            )}
            {modality.hasLiveClasses && course.schedule?.days && (
              <div className="rounded-xl border border-border bg-white p-3.5 text-sm text-ink sm:col-span-2">
                <p><strong>Días:</strong> {course.schedule.days}</p>
                {(course.schedule.startTime || course.schedule.endTime) && (
                  <p className="mt-1">
                    <strong>Horario:</strong> {course.schedule.startTime || '—'} – {course.schedule.endTime || '—'}
                  </p>
                )}
              </div>
            )}
          </div>

          <button
            className="mt-6 min-h-12 w-full rounded-button bg-primary px-6 py-3 text-base font-semibold text-white transition hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
            disabled={enrolling || !courseIsActive}
            onClick={handleEnroll}
            type="button"
          >
            {!courseIsActive ? 'Curso no disponible' : enrolling ? 'Procesando inscripción...' : 'Inscribirme'}
          </button>
          {enrollmentMessage && <p className="mt-3 text-sm text-emerald-700" role="status">{enrollmentMessage}</p>}
          {enrollmentError && <p className="mt-3 text-sm text-red-700" role="alert">{enrollmentError}</p>}
        </div>
      </section>

      <section className="mt-10 border-b border-border">
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
        className="max-w-4xl py-7"
        id="course-tabpanel"
        role="tabpanel"
      >
        {activeTab === 'description' && (
          <div className="space-y-5">
            <h2 className="text-xl font-bold text-ink">Descripción del curso</h2>
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
