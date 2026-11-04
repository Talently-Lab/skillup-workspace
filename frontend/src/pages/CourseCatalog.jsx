import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import CourseCard from '../components/courses/CourseCard'
import { getCourses } from '../services/courseService'

export default function CourseCatalog() {
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getCourses()
      .then((items) => {
        if (active) setCourses(items)
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
    <section className="mx-auto max-w-7xl px-4 py-8 sm:py-12">
      <Link className="mb-6 inline-flex min-h-10 items-center gap-2 text-sm font-medium text-primary hover:text-primary-hover" to="/">
        <ArrowLeft aria-hidden="true" size={16} />
        Inicio
      </Link>
      <h1 className="text-3xl font-bold text-ink">Catálogo de cursos</h1>
      <p className="mt-2 text-muted">Explora los cursos disponibles en SkillUp Campus.</p>

      {loading && <p className="mt-8 text-sm text-muted" role="status">Cargando cursos...</p>}
      {error && <p className="mt-8 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">{error}</p>}
      {!loading && !error && courses.length === 0 && (
        <p className="mt-8 text-sm text-muted">No hay cursos disponibles por el momento.</p>
      )}
      {!loading && !error && courses.length > 0 && (
        <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {courses.map((course) => <CourseCard key={course.id} course={course} />)}
        </div>
      )}
    </section>
  )
}
