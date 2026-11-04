import { useEffect, useMemo, useState } from 'react'
import { ChevronDown, ChevronLeft, ChevronRight, Search, X } from 'lucide-react'
import CourseCard from '../components/courses/CourseCard'
import { getCourses } from '../services/courseService'

const PAGE_SIZE = 8

function normalizeText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('es')
    .trim()
}

function getDurationValue(course) {
  return `${course.durationWeeks ?? ''}|${course.durationHours ?? ''}`
}

function getCreatedAtTimestamp(course) {
  const timestamp = Date.parse(course.createdAt ?? '')
  return Number.isNaN(timestamp) ? 0 : timestamp
}

function getDurationLabel(course) {
  const values = []
  if (course.durationWeeks) {
    values.push(`${course.durationWeeks} ${course.durationWeeks === 1 ? 'semana' : 'semanas'}`)
  }
  if (course.durationHours) {
    values.push(`${course.durationHours} ${course.durationHours === 1 ? 'hora' : 'horas'}`)
  }
  return values.join(' · ')
}

function FilterSelect({ label, value, options, onChange }) {
  return (
    <label className="relative inline-flex h-10 shrink-0 items-center gap-2 rounded-md border border-slate-300 bg-white px-2.5 transition hover:border-blue-300 focus-within:ring-2 focus-within:ring-primary/15">
      <span className="pointer-events-none max-w-32 truncate text-xs text-slate-700">
        {value ? options.find((option) => option.value === value)?.label : label}
      </span>
      <select
        aria-label={label}
        className="absolute inset-0 z-10 h-full w-full cursor-pointer appearance-none opacity-0"
        onChange={(event) => onChange(event.target.value)}
        value={value}
      >
        <option value="">{label}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
      <ChevronDown aria-hidden="true" className="pointer-events-none shrink-0 text-slate-500" size={14} />
    </label>
  )
}

function RankingButton({ label, selected, onClick, title }) {
  return (
    <button
      aria-pressed={selected}
      className={`inline-flex h-10 shrink-0 items-center rounded-md border px-3 text-xs transition ${
        selected
          ? 'border-primary bg-primary text-white'
          : 'border-slate-300 bg-white text-slate-700 hover:border-blue-300 hover:text-primary'
      }`}
      onClick={onClick}
      title={title}
      type="button"
    >
      {label}
    </button>
  )
}

function Pagination({ currentPage, totalPages, onPageChange }) {
  const firstVisiblePage = Math.max(1, Math.min(currentPage - 2, totalPages - 4))
  const visiblePages = Array.from(
    { length: Math.min(5, totalPages) },
    (_, index) => firstVisiblePage + index,
  )

  return (
    <nav aria-label="Paginación del catálogo" className="mt-4 flex items-center justify-center gap-1">
      <button
        aria-label="Página anterior"
        className="flex size-7 items-center justify-center rounded-full text-slate-500 transition hover:bg-blue-50 hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
        disabled={currentPage === 1}
        onClick={() => onPageChange(currentPage - 1)}
        type="button"
      >
        <ChevronLeft aria-hidden="true" size={18} />
      </button>
      {visiblePages.map((page) => (
        <button
          aria-current={page === currentPage ? 'page' : undefined}
          aria-label={`Página ${page}`}
          className={`size-7 rounded-full text-xs font-medium transition ${
            page === currentPage
              ? 'bg-primary text-white'
              : 'text-slate-600 hover:bg-blue-50 hover:text-primary'
          }`}
          key={page}
          onClick={() => onPageChange(page)}
          type="button"
        >
          {page}
        </button>
      ))}
      <button
        aria-label="Página siguiente"
        className="flex size-7 items-center justify-center rounded-full text-slate-500 transition hover:bg-blue-50 hover:text-primary disabled:cursor-not-allowed disabled:opacity-40"
        disabled={currentPage === totalPages}
        onClick={() => onPageChange(currentPage + 1)}
        type="button"
      >
        <ChevronRight aria-hidden="true" size={18} />
      </button>
    </nav>
  )
}

export default function CourseCatalog() {
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('')
  const [level, setLevel] = useState('')
  const [duration, setDuration] = useState('')
  const [rankingMode, setRankingMode] = useState(null)
  const [currentPage, setCurrentPage] = useState(1)

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

  const categories = useMemo(
    () => [...new Set(courses.map((course) => course.category).filter(Boolean))]
      .sort((left, right) => left.localeCompare(right, 'es'))
      .map((value) => ({ value, label: value })),
    [courses],
  )
  const levels = useMemo(
    () => [...new Set(courses.map((course) => course.level).filter(Boolean))]
      .sort((left, right) => left.localeCompare(right, 'es'))
      .map((value) => ({ value, label: value })),
    [courses],
  )
  const durations = useMemo(() => {
    const options = new Map()
    courses.forEach((course) => {
      const value = getDurationValue(course)
      const label = getDurationLabel(course)
      if (label) options.set(value, label)
    })
    return [...options.entries()]
      .sort(([, left], [, right]) => left.localeCompare(right, 'es', { numeric: true }))
      .map(([value, label]) => ({ value, label }))
  }, [courses])

  const filteredCourses = useMemo(() => {
    const normalizedSearch = normalizeText(search)
    const matches = courses.filter((course) => {
      if (normalizedSearch && !normalizeText(course.title).includes(normalizedSearch)) return false
      if (category && course.category !== category) return false
      if (level && course.level !== level) return false
      if (duration && getDurationValue(course) !== duration) return false
      return true
    })

    if (rankingMode === 'featured') {
      return [...matches]
        .sort((left, right) => (
          (right.activeEnrollmentCount ?? 0) - (left.activeEnrollmentCount ?? 0) ||
          getCreatedAtTimestamp(right) - getCreatedAtTimestamp(left)
        ))
        .slice(0, 4)
    }

    if (rankingMode === 'newest') {
      return [...matches].sort((left, right) => (
        getCreatedAtTimestamp(right) - getCreatedAtTimestamp(left)
      ))
    }

    return matches
  }, [courses, search, category, level, duration, rankingMode])

  const totalPages = Math.max(1, Math.ceil(filteredCourses.length / PAGE_SIZE))
  const visibleCourses = filteredCourses.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  )

  const updateFilter = (setter) => (value) => {
    setter(value)
    setCurrentPage(1)
  }

  const toggleRanking = (mode) => {
    setRankingMode((currentMode) => currentMode === mode ? null : mode)
    setCurrentPage(1)
  }

  const activeFilters = [
    ...(search ? [{ label: `Búsqueda: ${search}`, clear: () => setSearch('') }] : []),
    ...(rankingMode ? [{
      label: rankingMode === 'featured' ? 'Destacados' : 'Nuevos',
      clear: () => setRankingMode(null),
    }] : []),
    ...(category ? [{
      label: `Categoría: ${categories.find((option) => option.value === category)?.label ?? category}`,
      clear: () => setCategory(''),
    }] : []),
    ...(level ? [{
      label: `Nivel: ${levels.find((option) => option.value === level)?.label ?? level}`,
      clear: () => setLevel(''),
    }] : []),
    ...(duration ? [{
      label: `Duración: ${durations.find((option) => option.value === duration)?.label ?? duration}`,
      clear: () => setDuration(''),
    }] : []),
  ]

  const clearAllFilters = () => {
    setSearch('')
    setCategory('')
    setLevel('')
    setDuration('')
    setRankingMode(null)
    setCurrentPage(1)
  }

  return (
    <section className="mx-auto min-h-[65vh] w-full max-w-[1200px] px-4 pb-6 pt-5 sm:px-6 lg:px-8 xl:max-w-[1360px] xl:px-10">
      <header>
        <h1 className="text-2xl font-semibold leading-7 text-slate-900">Cursos</h1>
        <p className="mt-1 text-xs leading-5 text-slate-500 sm:text-[13px]">
          Explorá nuestra selección de cursos y adquirí las habilidades que necesitás para crecer profesionalmente
        </p>
      </header>

      <div className="mt-4 flex flex-col gap-2 lg:flex-row lg:items-center">
        <div className="relative block min-w-0 flex-1">
          <Search aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={17} />
          <input
            className="h-10 w-full rounded-md border border-slate-300 bg-white pl-10 pr-10 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-primary focus:ring-2 focus:ring-primary/10 [&::-webkit-search-cancel-button]:hidden"
            id="course-search"
            onChange={(event) => updateFilter(setSearch)(event.target.value)}
            placeholder="Buscar cursos"
            type="search"
            value={search}
          />
          {search && (
            <button
              aria-label="Limpiar búsqueda"
              className="absolute right-1 top-1/2 inline-flex size-8 -translate-y-1/2 items-center justify-center rounded-full text-slate-500 transition hover:bg-slate-100 hover:text-slate-800 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary"
              onClick={() => updateFilter(setSearch)('')}
              title="Limpiar búsqueda"
              type="button"
            >
              <X aria-hidden="true" size={16} />
            </button>
          )}
        </div>

        <div aria-label="Filtros de cursos" className="flex min-w-0 gap-2 overflow-x-auto pb-1 lg:shrink-0 lg:overflow-visible lg:pb-0">
          <RankingButton
            label="Destacados"
            onClick={() => toggleRanking('featured')}
            selected={rankingMode === 'featured'}
            title="Mostrar los 4 cursos con más inscripciones activas"
          />
          <RankingButton
            label="Nuevos"
            onClick={() => toggleRanking('newest')}
            selected={rankingMode === 'newest'}
            title="Ordenar por fecha de creación o publicación más reciente"
          />
          <FilterSelect
            label="Categorías"
            onChange={updateFilter(setCategory)}
            options={categories}
            value={category}
          />
          <FilterSelect
            label="Nivel"
            onChange={updateFilter(setLevel)}
            options={levels}
            value={level}
          />
          <FilterSelect
            label="Duración"
            onChange={updateFilter(setDuration)}
            options={durations}
            value={duration}
          />
        </div>
      </div>

      {activeFilters.length > 0 && (
        <div className="mt-3 flex flex-wrap items-center gap-2" aria-label="Filtros activos">
          <span className="mr-1 text-sm font-semibold text-slate-700">Filtros aplicados:</span>
          {activeFilters.map(({ label, clear }) => (
            <button
              aria-label={`Quitar filtro ${label}`}
              className="inline-flex min-h-9 items-center gap-1.5 rounded-full border border-blue-100 bg-blue-50 px-3.5 text-sm font-medium text-primary transition hover:border-blue-200 hover:bg-blue-100"
              key={label}
              onClick={() => {
                clear()
                setCurrentPage(1)
              }}
              type="button"
            >
              <span className="max-w-64 truncate">{label}</span>
              <X aria-hidden="true" size={15} />
            </button>
          ))}
          <button
            className="ml-1 inline-flex min-h-9 items-center rounded px-2 text-sm font-medium text-slate-600 underline-offset-2 transition hover:text-primary hover:underline"
            onClick={clearAllFilters}
            type="button"
          >
            Limpiar filtros
          </button>
        </div>
      )}

      {loading && <p className="mt-8 text-sm text-muted" role="status">Cargando cursos...</p>}
      {error && <p className="mt-8 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700" role="alert">{error}</p>}
      {!loading && !error && filteredCourses.length === 0 && (
        <p className="mt-8 rounded-xl border border-slate-200 bg-white px-5 py-8 text-center text-sm text-slate-500" role="status">
          {courses.length ? 'No encontramos cursos con esos criterios.' : 'No hay cursos disponibles por el momento.'}
        </p>
      )}
      {!loading && !error && visibleCourses.length > 0 && (
        <>
          <div className="mt-5 grid grid-cols-1 items-stretch gap-4 min-[480px]:grid-cols-2 lg:grid-cols-4 lg:gap-5">
            {visibleCourses.map((course) => (
              <CourseCard key={course.id} course={course} variant="catalog" />
            ))}
          </div>
          <Pagination
            currentPage={currentPage}
            onPageChange={setCurrentPage}
            totalPages={totalPages}
          />
        </>
      )}
    </section>
  )
}
