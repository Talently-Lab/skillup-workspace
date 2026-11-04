import { useEffect, useState } from 'react'
import { ArrowRight } from 'lucide-react'
import Button from '../ui/Button'
import CourseCard from '../courses/CourseCard'
import { getCourses } from '../../services/courseService'

export default function CourseSection() {
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getCourses()
      .then((items) => {
        if (active) setCourses(items.slice(0, 4))
      })
      .catch((requestError) => {
        if (active) setError(requestError.message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  return (
    <section className="mx-auto max-w-7xl px-4 pb-16">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-xl font-bold">Cursos destacados</h2>

        <Button to="/cursos" variant="text">
          Ver todos <ArrowRight size={14} />
        </Button>
      </div>

      {error && <p className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">{error}</p>}
      {loading && <p className="text-sm text-muted" role="status">Cargando cursos...</p>}
      {!loading && !error && courses.length === 0 && (
        <p className="text-sm text-muted" role="status">No hay cursos disponibles por el momento.</p>
      )}
      {!loading && !error && <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {courses.map((course) => (
          <CourseCard key={course.id} course={course} />
        ))}
      </div>}
    </section>
  )
}