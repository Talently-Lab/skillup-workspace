import { Clock3, Signal } from 'lucide-react'
import Badge from '../ui/Badge'
import Button from '../ui/Button'

export default function CourseCard({ course }) {
  const { id, imageUrl, category, title, summary, level, durationHours, durationWeeks } = course

  return (
    <article className="flex flex-col overflow-hidden rounded-card border border-border bg-surface">
      {imageUrl ? (
        <img
          src={imageUrl}
          alt={title}
          className="aspect-[3/1] w-full bg-secondary object-cover"
        />
      ) : (
        <div aria-hidden="true" className="aspect-[3/1] w-full bg-secondary" />
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

        <Button
          to={`/cursos/${id}`}
          variant="secondary"
          className="mt-1 w-full py-2"
        >
          Ver curso
        </Button>
      </div>
    </article>
  )
}