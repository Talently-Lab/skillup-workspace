import { CalendarDays, ChartNoAxesColumn, Clock3 } from "lucide-react";
import { Link } from "react-router-dom";

export default function CourseCard({ course }) {
  return (
    <article className="rounded-xl border border-slate-200 bg-white p-4">
      <span className="text-xs font-medium text-blue-600">
        {course.category}
      </span>

      <div className="h-28"></div>

      <h2 className="mt-3 text-lg font-semibold text-slate-800">
        {course.title}
      </h2>

      <p className="mt-2 text-sm text-slate-500">{course.description}</p>

      <div className="mt-4 space-y-2 text-xs text-slate-500">
        <p className="flex items-center gap-2">
          <CalendarDays size={15} />
          Próximo inicio: {course.nextDate}
        </p>

        <div className="flex flex-wrap gap-2">
          <p className="flex items-center gap-2">
            <ChartNoAxesColumn size={15} />
            Nivel: {course.level}
          </p>

          <p className="flex items-center gap-2">
            <Clock3 size={15} />
            {course.schedule}
          </p>
        </div>
      </div>

      <Link
        to={`/cursos/${course.id}`}
        className="mt-5 block rounded-3xl text-blue-600 border-blue-600 border-2 py-2 text-center text-sm hover:bg-blue-700 hover:text-white"
      >
        Ver curso
      </Link>
    </article>
  );
}
