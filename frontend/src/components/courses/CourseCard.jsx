import { Calendar, Signal } from 'lucide-react'
import Badge from '../ui/Badge'
import Button from '../ui/Button'

export default function CourseCard({ course }) {
  const { id, image, category, title, description, level, duration } = course

  return (
    <article className="flex flex-col overflow-hidden rounded-card border border-border bg-surface">
      <img
        src={image}
        alt={title}
        className="aspect-[3/1] w-full bg-secondary object-cover"
      />

      <div className="flex flex-1 flex-col gap-2 p-3">
        <Badge className="self-start">{category}</Badge>
        <h3 className="text-base font-semibold text-ink">{title}</h3>

        <p className="line-clamp-2 flex-1 text-xs text-muted">{description}</p>

        <div className="flex items-center gap-4 text-xs text-muted">
          <span className="flex items-center gap-1">
            <Signal size={14} /> {level}
          </span>
          <span className="flex items-center gap-1">
            <Calendar size={14} /> {duration}
          </span>
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