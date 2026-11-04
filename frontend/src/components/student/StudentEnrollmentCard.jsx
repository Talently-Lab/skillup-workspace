import { useEffect, useState } from 'react'
import { CalendarDays, X } from 'lucide-react'

const COMPLETED_STATUSES = ['COMPLETED', 'FINISHED', 'FINALIZED', 'FINALIZADO']

function getEnrollmentStatus(status) {
  const normalizedStatus = status?.trim().toUpperCase()
  if (normalizedStatus === 'ACTIVE') {
    return { label: 'Activo', className: 'bg-blue-50 text-blue-700' }
  }
  if (COMPLETED_STATUSES.includes(normalizedStatus)) {
    return { label: 'Finalizado', className: 'bg-slate-100 text-slate-600' }
  }
  if (!normalizedStatus) {
    return { label: 'Sin estado', className: 'bg-slate-100 text-slate-600' }
  }
  return {
    label: normalizedStatus.charAt(0) + normalizedStatus.slice(1).toLowerCase(),
    className: 'bg-slate-100 text-slate-600',
  }
}

function formatEnrollmentDate(value) {
  if (!value) return null

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null

  return new Intl.DateTimeFormat('es', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date).replace(/\.$/, '')
}

function getProgressPercentage(value) {
  if (typeof value !== 'number' || !Number.isFinite(value)) return 0
  return Math.min(100, Math.max(0, value))
}

export default function StudentEnrollmentCard({ enrollment }) {
  const [showComingSoon, setShowComingSoon] = useState(false)
  const status = getEnrollmentStatus(enrollment.status)
  const enrollmentDate = formatEnrollmentDate(enrollment.enrolledAt)
  const progress = getProgressPercentage(enrollment.progressPercentage)

  useEffect(() => {
    if (!showComingSoon) return undefined

    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setShowComingSoon(false)
    }

    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [showComingSoon])

  return (
    <>
      <article className="flex h-[301px] w-full max-w-[376px] min-w-0 flex-col rounded-[18px] border border-slate-200 bg-white p-[18px]">
        <span className={`inline-flex h-7 w-[90px] shrink-0 items-center justify-center overflow-hidden rounded-full text-ellipsis whitespace-nowrap text-xs font-medium ${status.className}`}>
          {status.label}
        </span>
        <h3 className="mt-4 line-clamp-2 text-[22px] font-semibold leading-7 text-slate-900">
          {enrollment.courseTitle}
        </h3>
        {enrollmentDate && (
          <p className="mt-3 inline-flex min-w-0 items-center gap-2 text-base leading-6 text-slate-500">
            <CalendarDays aria-hidden="true" className="shrink-0" size={16} />
            <span className="truncate">Inscripto el {enrollmentDate}</span>
          </p>
        )}
        <div className="mt-auto pt-4">
          <p className="text-[26px] font-semibold leading-8 text-primary">{progress}%</p>
          <div
            aria-label={`Progreso del curso: ${progress}%`}
            className="mt-2 h-[7px] overflow-hidden rounded-full bg-blue-100"
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={progress}
          >
            <div
              className="h-full rounded-full bg-primary transition-[width]"
              style={{ width: `${progress}%` }}
            />
          </div>
          <button
            className="mt-[18px] inline-flex h-[43px] w-full items-center justify-center rounded-lg bg-primary px-3 text-lg font-semibold text-white transition hover:bg-primary-hover"
            onClick={() => setShowComingSoon(true)}
            type="button"
          >
            Ver temario
          </button>
        </div>
      </article>
      {showComingSoon && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/50 p-4">
          <section
            aria-labelledby="coming-soon-title"
            aria-modal="true"
            className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl"
            role="dialog"
          >
            <div className="flex items-start justify-between gap-4">
              <h2 className="text-lg font-semibold text-slate-900" id="coming-soon-title">
                Ver temario
              </h2>
              <button
                aria-label="Cerrar"
                className="flex size-8 shrink-0 items-center justify-center rounded-full text-slate-500 hover:bg-slate-100 hover:text-slate-700"
                onClick={() => setShowComingSoon(false)}
                type="button"
              >
                <X aria-hidden="true" size={18} />
              </button>
            </div>
            <p className="mt-3 text-sm text-slate-600">Funcionalidad próximamente.</p>
            <button
              autoFocus
              className="mt-6 min-h-10 w-full rounded-lg bg-primary px-4 text-sm font-semibold text-white transition hover:bg-primary-hover"
              onClick={() => setShowComingSoon(false)}
              type="button"
            >
              Entendido
            </button>
          </section>
        </div>
      )}
    </>
  )
}
