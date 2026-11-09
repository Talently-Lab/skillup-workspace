import { useMemo, useState } from "react";
import { ChevronLeft, ChevronRight, Search } from "lucide-react";
import { courses } from "../../data/courses";
import CourseCard from "./CoursesCard";

export default function Courses() {
  const [search, setSearch] = useState("");
  const [featured, setFeatured] = useState("Todos");
  const [newCourses, setNewCourses] = useState("Todos");
  const [category, setCategory] = useState("Todas");
  const [level, setLevel] = useState("Todos");
  const [duration, setDuration] = useState("Todas");

  const categories = [
    "Todas",
    ...new Set(courses.map((course) => course.category)),
  ];

  const levels = ["Todos", ...new Set(courses.map((course) => course.level))];

  const durations = [
    "Todas",
    ...new Set(courses.map((course) => course.duration)),
  ];

  // Filtramos los cursos según la búsqueda y categoría
  const filteredCourses = useMemo(() => {
    return courses.filter((course) => {
      const matchesSearch = course.title
        .toLowerCase()
        .includes(search.toLowerCase());

      const matchesFeatured = featured === "Todos" || course.featured === true;

      const matchesNew = newCourses === "Todos" || course.isNew === true;

      const matchesCategory =
        category === "Todas" || course.category === category;

      const matchesLevel = level === "Todos" || course.level === level;

      const matchesDuration =
        duration === "Todas" || course.duration === duration;

      return (
        matchesSearch &&
        matchesFeatured &&
        matchesNew &&
        matchesCategory &&
        matchesLevel &&
        matchesDuration
      );
    });
  }, [search, featured, newCourses, category, level, duration]);

  return (
    <main className="min-h-screen bg-slate-50 px-6 py-8">
      <div className="mx-auto max-w-7xl">
        {/* Encabezado */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-slate-800">Cursos</h1>

          <p className="mt-1 max-w-xl text-sm text-slate-500">
            Explorá nuestra selección de cursos y adquirí las habilidades que
            necesitás para crecer profesionalmente.
          </p>
        </div>

        {/* Buscador y filtros */}
        <div className="mb-6 flex flex-col gap-3 md:flex-row md:items-center">
          {/* Buscador */}
          <div className="relative w-full flex-1">
            <Search
              size={17}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
            />

            <input
              type="text"
              placeholder="Buscar cursos"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full rounded-full border border-slate-200 bg-white py-2 pl-10 pr-4 text-sm outline-none focus:border-blue-400"
            />
          </div>

          {/* Filtros */}
          <div className="flex flex-wrap gap-2 shrink-0">
            <select
              value={featured}
              onChange={(e) => setFeatured(e.target.value)}
              className="rounded-full bg-blue-50 px-3 py-2 text-xs text-blue-600 outline-none"
            >
              <option value="Todos">Destacados</option>
              <option value="Destacados">Solo destacados</option>
            </select>

            <select
              value={newCourses}
              onChange={(e) => setNewCourses(e.target.value)}
              className="rounded-full bg-blue-50 px-3 py-2 text-xs text-blue-600 outline-none"
            >
              <option value="Todos">Nuevos</option>
              <option value="Nuevos">Solo nuevos</option>
            </select>

            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="rounded-full bg-blue-50 px-3 py-2 text-xs text-blue-600 outline-none"
            >
              {categories.map((item) => (
                <option key={item} value={item}>
                  {item === "Todas" ? "Categorías" : item}
                </option>
              ))}
            </select>

            <select
              value={level}
              onChange={(e) => setLevel(e.target.value)}
              className="rounded-full bg-blue-50 px-3 py-2 text-xs text-blue-600 outline-none"
            >
              {levels.map((item) => (
                <option key={item} value={item}>
                  {item === "Todos" ? "Nivel" : item}
                </option>
              ))}
            </select>

            <select
              value={duration}
              onChange={(e) => setDuration(e.target.value)}
              className="rounded-full bg-blue-50 px-3 py-2 text-xs text-blue-600 outline-none"
            >
              {durations.map((item) => (
                <option key={item} value={item}>
                  {item === "Todas" ? "Duración" : item}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Listado de cursos */}
        {filteredCourses.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {filteredCourses.map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>
        ) : (
          <div className="rounded-xl border border-slate-200 bg-white py-12 text-center">
            <p className="text-sm text-slate-500">No se encontraron cursos.</p>
          </div>
        )}
        
        {/* Paginación decorativa */}
        <div className="mt-8 flex items-center justify-center gap-5 text-xs">
          <button className="text-slate-400 hover:text-blue-600">
            <ChevronLeft size={14} />
          </button>

          <button className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-600 font-medium text-white">
            1
          </button>

          <button className="text-slate-500 hover:text-blue-600">2</button>

          <button className="text-slate-500 hover:text-blue-600">3</button>

          <button className="text-slate-400 hover:text-blue-600">
            <ChevronRight size={14} />
          </button>
        </div>
      </div>
    </main>
  );
}
