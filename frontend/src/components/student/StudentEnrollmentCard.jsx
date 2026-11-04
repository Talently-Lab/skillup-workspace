import { BookOpenText, Clock3, Signal } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'

function getDuration(course) {
  if (course.durationHours) {
    return `${course.durationHours} ${course.durationHours === 1 ? 'hora' : 'horas'}`
  }
  if (course.durationWeeks) {
    return `${course.durationWeeks} ${course.durationWeeks === 1 ? 'semana' : 'semanas'}`
  }
  return null
}

export default function StudentEnrollmentCard({ enrollment }) {
  const [imageFailed, setImageFailed] = useState(false)
  const duration = getDuration(enrollment)

  return (
    <article className="flex min-w-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white sm:flex-row">
      <div className="h-40 shrink-0 overflow-hidden bg-blue-50 sm:h-auto sm:w-48">
        {enrollment.imageUrl && !imageFailed ? (
          <img
            alt=""
            aria-hidden="true"
            className="size-full object-cover"
            loading="lazy"
            onError={() => setImageFailed(true)}
            src={enrollment.imageUrl}
          />
        ) : (
          <div className="flex size-full min-h-32 items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
            <BookOpenText aria-hidden="true" className="text-blue-300" size={32} strokeWidth={1.5} />
          </div>
        )}
      </div>
      <div className="flex min-w-0 flex-1 flex-col p-4">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="min-w-0">
            {enrollment.category && (
              <span className="text-xs font-medium text-primary">{enrollment.category}</span>
            )}
            <h3 className="mt-1 text-base font-semibold text-slate-900">
              {enrollment.courseTitle}
            </h3>
          </div>
          <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700">
            Cursando
          </span>
        </div>
        {enrollment.summary && (
          <p className="mt-2 line-clamp-2 text-sm leading-5 text-slate-500">
            {enrollment.summary}
          </p>
        )}
        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-slate-500">
          {enrollment.level && (
            <span className="inline-flex items-center gap-1.5">
              <Signal aria-hidden="true" className="text-primary" size={14} />
              {enrollment.level}
            </span>
          )}
          {duration && (
            <span className="inline-flex items-center gap-1.5">
              <Clock3 aria-hidden="true" className="text-primary" size={14} />
              {duration}
            </span>
          )}
        </div>
        <Link
          className="mt-4 inline-flex min-h-10 items-center justify-center self-start rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:bg-primary-hover"
          to={`/cursos/${enrollment.courseId}`}
        >
          Ver curso
        </Link>
      </div>
    </article>
  )
}
