import { ArrowRight } from 'lucide-react'
import Button from '../ui/Button'
import CourseCard from '../courses/CourseCard'
import { courses } from '../../data/courses'

export default function CourseSection() {
  return (
    <section className="mx-auto max-w-7xl px-4 pb-16">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="text-xl font-bold">Cursos destacados</h2>

        <Button to="/cursos" variant="text">
          Ver todos <ArrowRight size={14} />
        </Button>
      </div>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {courses.map((course) => (
          <CourseCard key={course.id} course={course} />
        ))}
      </div>
    </section>
  )
}