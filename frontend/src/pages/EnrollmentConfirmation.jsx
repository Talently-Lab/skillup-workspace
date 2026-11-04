import { useEffect, useState } from 'react'
import { Check, Clock3 } from 'lucide-react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { getCourse } from '../services/courseService'
import { getErrorMessage } from '../utils/apiErrors'

export default function EnrollmentConfirmation() {
  const location = useLocation()
  const courseId = location.state?.courseId
  const navigationCourse = location.state?.course
  const isAlreadyEnrolled = location.state?.alreadyEnrolled === true
  const enrollmentConfirmed = location.state?.enrollmentConfirmed === true
  const [courseResult, setCourseResult] = useState(() => ({
    id: navigationCourse && String(navigationCourse.id) === String(courseId)
      ? String(courseId)
      : null,
    course: navigationCourse && String(navigationCourse.id) === String(courseId)
      ? navigationCourse
      : null,
    error: '',
  }))
  const [imageErrorCourseId, setImageErrorCourseId] = useState(null)
  const validCourseId = (typeof courseId === 'string' || typeof courseId === 'number') &&
    /^[1-9]\d*$/.test(String(courseId))

  useEffect(() => {
    if (!validCourseId || (navigationCourse && String(navigationCourse.id) === String(courseId))) {
      return undefined
    }

    let active = true
    getCourse(courseId)
      .then((course) => {
        if (active) setCourseResult({ id: String(courseId), course, error: '' })
      })
      .catch((error) => {
        if (active) setCourseResult({ id: String(courseId), course: null, error: getErrorMessage(error) })
      })

    return () => {
      active = false
    }
  }, [courseId, navigationCourse, validCourseId])

  if (!validCourseId || (!enrollmentConfirmed && !isAlreadyEnrolled)) {
    return <Navigate replace to="/cursos" />
  }

  const loading = courseResult.id !== String(courseId)
  const course = loading ? null : courseResult.course
  const error = loading ? '' : courseResult.error

  return (
    <main className="flex min-h-[calc(100vh-85px)] items-center justify-center bg-slate-50 px-4 py-10 sm:px-6">
      <section className="w-full max-w-[500px] text-center">
        <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-blue-100 text-primary">
          <Check aria-hidden="true" size={30} strokeWidth={2.5} />
        </div>
        <h1 className="mt-4 text-2xl font-bold leading-tight text-ink sm:text-[28px]">
          {isAlreadyEnrolled ? 'Ya estás inscrito en este curso' : '¡Tu inscripción se realizó con éxito!'}
        </h1>
        <p className="mx-auto mt-2 max-w-[440px] text-sm leading-6 text-muted">
          {isAlreadyEnrolled
            ? 'Este curso ya está en tu panel de estudiante.'
            : 'Ya estás inscrito en el curso. Podés encontrarlo en tu panel de estudiante y comenzar cuando quieras'}
        </p>

        {loading && <p className="mt-6 text-sm text-muted" role="status">Cargando información del curso...</p>}
        {error && (
          <p className="mt-6 rounded-lg bg-red-50 px-4 py-3 text-left text-sm text-red-700" role="alert">
            No pudimos cargar los datos del curso: {error}
          </p>
        )}
        {course && (
          <article className="mt-6 overflow-hidden rounded-xl border border-slate-200 bg-white text-left sm:flex">
            {course.imageUrl && imageErrorCourseId !== course.id ? (
              <img
                alt={course.title}
                className="aspect-[16/9] w-full object-cover sm:aspect-auto sm:h-[132px] sm:w-[176px] sm:shrink-0"
                src={course.imageUrl}
                onError={() => setImageErrorCourseId(course.id)}
              />
            ) : (
              <div aria-hidden="true" className="flex aspect-[16/9] w-full items-center justify-center bg-blue-50 text-sm text-primary sm:aspect-auto sm:h-[132px] sm:w-[176px] sm:shrink-0">
                Imagen no disponible
              </div>
            )}
            <div className="min-w-0 p-4">
              <h2 className="text-base font-semibold text-ink">{course.title}</h2>
              <p className="mt-1 line-clamp-2 text-sm leading-5 text-muted">
                {course.summary || course.description || ' '}
              </p>
              <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted">
                {course.level && <span>Nivel: {course.level}</span>}
                {course.schedule?.days && (
                  <span className="inline-flex items-center gap-1">
                    <Clock3 aria-hidden="true" size={13} />
                    {course.schedule.days}
                    {(course.schedule.startTime || course.schedule.endTime) &&
                      ` · ${course.schedule.startTime || '—'} – ${course.schedule.endTime || '—'}`}
                  </span>
                )}
              </div>
            </div>
          </article>
        )}

        <div className="mx-auto mt-6 flex w-full max-w-[360px] flex-col gap-3">
          <Link
            className="inline-flex min-h-11 items-center justify-center rounded-button bg-primary px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-primary-hover"
            to="/mi-aprendizaje"
          >
            Ir a mis cursos
          </Link>
          <Link
            className="inline-flex min-h-11 items-center justify-center rounded-button border border-primary bg-white px-5 py-2.5 text-sm font-semibold text-primary transition hover:bg-blue-50"
            to="/cursos"
          >
            Volver al catálogo
          </Link>
        </div>
      </section>
    </main>
  )
}
