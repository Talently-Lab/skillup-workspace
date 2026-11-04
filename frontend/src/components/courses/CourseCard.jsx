import { BookOpenText, CalendarDays, Clock3, Signal } from 'lucide-react'
import { useState } from 'react'
import Badge from '../ui/Badge'
import Button from '../ui/Button'

function formatNextDate(value) {
  if (!value) return ''
  const date = new Date(`${value}T12:00:00`)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('es', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(date)
}

export default function CourseCard({ course, variant = 'default' }) {
  const [imageFailed, setImageFailed] = useState(false)
  const { id, imageUrl, category, title, summary, level, durationHours, durationWeeks, nextStartDate } = course

  if (variant === 'catalog') {
    const formattedDate = formatNextDate(nextStartDate)
    const duration = durationHours
      ? `${durationHours} ${durationHours === 1 ? 'hora' : 'horas'}`
      : durationWeeks
        ? `${durationWeeks} ${durationWeeks === 1 ? 'semana' : 'semanas'}`
        : ''

    return (
      <article className="flex h-full min-h-[270px] flex-col overflow-hidden rounded-xl border border-slate-200 bg-white sm:min-h-[280px] min-[800px]:min-h-[290px] xl:min-h-[310px]">
        <div className="relative h-28 w-full shrink-0 overflow-hidden bg-blue-50 min-[800px]:h-28 xl:h-32">
          {imageUrl && !imageFailed ? (
            <img
              alt=""
              aria-hidden="true"
              className="size-full object-cover"
              loading="lazy"
              onError={() => setImageFailed(true)}
              src={imageUrl}
            />
          ) : (
            <div aria-hidden="true" className="flex size-full items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
              <BookOpenText className="text-blue-300" size={25} strokeWidth={1.5} />
            </div>
          )}
          {category && (
            <Badge className="absolute left-2.5 top-2.5 border border-white/80 bg-white/95 px-2 py-0.5 text-[10px] shadow-sm">
              {category}
            </Badge>
          )}
        </div>

        <div className="flex min-h-0 flex-1 flex-col p-3">
          <h2 className="line-clamp-2 min-h-9 text-[13px] font-semibold leading-[18px] text-slate-900 sm:text-sm">{title}</h2>
          <p className="mt-1.5 line-clamp-2 min-h-8 text-[11px] leading-4 text-slate-500 sm:text-xs">{summary}</p>

          {formattedDate && (
            <span className="mt-2 flex items-center gap-1.5 text-[10px] leading-4 text-slate-500">
              <CalendarDays aria-hidden="true" className="shrink-0 text-primary" size={13} />
              <span>Próxima fecha: {formattedDate}</span>
            </span>
          )}

          <div className="mt-auto flex min-h-5 items-center justify-between gap-2 pt-2 text-[10px] leading-4 text-slate-500 sm:text-[11px]">
            {level && (
              <span className="flex min-w-0 items-center gap-1.5">
                <Signal aria-hidden="true" className="shrink-0 text-primary" size={13} />
                <span className="truncate">{level}</span>
              </span>
            )}
            {duration && (
              <span className="flex shrink-0 items-center gap-1.5">
                <Clock3 aria-hidden="true" className="shrink-0 text-primary" size={13} />
                <span>{duration}</span>
              </span>
            )}
          </div>

          <Button className="mt-3 !h-8 !px-3 !py-0 !text-xs rounded-lg" to={`/cursos/${id}`} variant="secondary">
            Ver curso
          </Button>
        </div>
      </article>
    )
  }

  return (
    <article className="flex flex-col overflow-hidden rounded-card border border-border bg-surface">
      {imageUrl && !imageFailed ? (
        <img
          alt={title}
          className="aspect-[3/1] w-full bg-secondary object-cover"
          onError={() => setImageFailed(true)}
          src={imageUrl}
        />
      ) : (
        <div aria-hidden="true" className="flex aspect-[3/1] w-full items-center justify-center bg-secondary">
          <BookOpenText className="text-primary/40" size={25} strokeWidth={1.5} />
        </div>
      )}

      <div className="flex flex-1 flex-col gap-2 p-3">
        {category && <Badge className="self-start">{category}</Badge>}
        <h3 className="text-base font-semibold text-ink">{title}</h3>
        <p className="line-clamp-2 flex-1 text-xs text-muted">{summary}</p>
        <div className="flex items-center gap-4 text-xs text-muted">
          {level && <span className="flex items-center gap-1"><Signal size={14} /> {level}</span>}
          {durationWeeks ? (
            <span className="flex items-center gap-1"><Clock3 size={14} /> {durationWeeks} semanas</span>
          ) : durationHours ? (
            <span className="flex items-center gap-1"><Clock3 size={14} /> {durationHours} horas</span>
          ) : null}
        </div>
        <Button className="mt-1 w-full py-2" to={`/cursos/${id}`} variant="secondary">
          Ver curso
        </Button>
      </div>
    </article>
  )
}