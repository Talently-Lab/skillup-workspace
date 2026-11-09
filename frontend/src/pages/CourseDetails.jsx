
import { useState } from "react";
import { Link, useParams, Navigate } from "react-router-dom";
import {
  ArrowLeft,
  CalendarDays,
  ChartNoAxesColumn,
  Clock3,
  Award,
  Heart,
  Check,
} from "lucide-react";
import { courses } from "../data/courses";

const tabs = ["Descripción", "Objetivos", "Contenidos", "Instructor"];

export default function CourseDetails() {
  const { id } = useParams();
  const [activeTab, setActiveTab] = useState("Descripción");
  const [favorite, setFavorite] = useState(false);

  const course = courses.find((item) => item.id === Number(id));

  if (!course) {
    return <Navigate to="/cursos" replace />;
  }

  return (
    <div className="min-h-screen bg-background px-5 py-7 md:px-10">
      <div className="mx-auto max-w-7xl">

        {/* Volver al catálogo */}
        <Link
          to="/cursos"
          className="mb-5 inline-flex items-center gap-2 text-xs text-primary hover:underline"
        >
          <ArrowLeft size={14} />
          Volver al catálogo
        </Link>

        {/* Información principal */}
        <section className="grid grid-cols-1 gap-5 md:grid-cols-2">

          {/* Imagen */}
          <div className="h-56 overflow-hidden rounded-xl bg-slate-200 md:h-64">
            {course.image ? (
              <img
                src={course.image}
                alt={course.title}
                className="h-full w-full object-cover"
              />
            ) : null}
          </div>

          {/* Información del curso */}
          <div className="flex flex-col">
            <h1 className="text-2xl font-bold text-ink">
              {course.title}
            </h1>

            <span className="mt-2 w-fit rounded-full bg-secondary px-3 py-1 text-xs font-medium text-primary">
              {course.category}
            </span>

            <div className="mt-4 space-y-2 text-xs text-muted">
              <p className="flex items-center gap-2">
                <Clock3 size={14} />
                Duración: {course.duration || "Por definir"}
                <ChartNoAxesColumn size={14} className="ml-2" />
                Nivel: {course.level}
              </p>

              <p className="flex items-center gap-2">
                <Award size={14} />
                Certificado incluido
              </p>

              <p className="flex items-center gap-2">
                <CalendarDays size={14} />
                Próximo inicio: {course.nextDate}
              </p>
            </div>

            <div className="mt-7 flex items-center gap-3">
              <button className="rounded-lg bg-primary px-8 py-2 text-sm font-semibold text-white transition hover:bg-primary-hover">
                Inscribirme
              </button>

              <button
                onClick={() => setFavorite(!favorite)}
                aria-label="Añadir a favoritos"
                className="flex h-10 w-10 items-center justify-center rounded-lg border border-border bg-white text-primary hover:bg-secondary"
              >
                <Heart
                  size={18}
                  fill={favorite ? "currentColor" : "none"}
                />
              </button>
            </div>
          </div>
        </section>

        {/* Pestañas */}
        <section className="mt-7">
          <div className="flex gap-7 overflow-x-auto border-b border-border">
            {tabs.map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`shrink-0 pb-3 text-xs transition ${
                  activeTab === tab
                    ? "border-b-2 border-primary font-semibold text-primary"
                    : "text-muted hover:text-primary"
                }`}
              >
                {tab}
              </button>
            ))}
          </div>

          {/* Contenido de las pestañas */}
          <div className="max-w-3xl py-5">
            {activeTab === "Descripción" && (
              <>
                <h2 className="mb-3 text-base font-bold text-ink">
                  ¿Qué vas a aprender?
                </h2>

                <p className="text-sm leading-relaxed text-muted">
                  {course.description}
                </p>

                <div className="mt-5 space-y-4">
                  {(course.objectives || [
                    "Aprender los fundamentos del curso.",
                    "Desarrollar habilidades prácticas.",
                    "Aplicar los conocimientos en proyectos reales.",
                  ]).slice(0, 3).map((objective, index) => (
                    <p
                      key={index}
                      className="flex items-center gap-3 text-sm text-muted"
                    >
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary text-white">
                        <Check size={13} />
                      </span>
                      {objective}
                    </p>
                  ))}
                </div>

                <button className="mt-6 rounded-lg border border-primary px-8 py-2 text-xs font-semibold text-primary hover:bg-secondary">
                  Descargar programa
                </button>
              </>
            )}

            {activeTab === "Objetivos" && (
              <>
                <h2 className="mb-4 text-base font-bold text-ink">
                  Objetivos del curso
                </h2>

                <ul className="space-y-4">
                  {(course.objectives || [
                    "Conocer los conceptos fundamentales.",
                    "Desarrollar habilidades prácticas.",
                    "Resolver problemas mediante ejercicios.",
                  ]).map((objective, index) => (
                    <li key={index} className="flex gap-3 text-sm text-muted">
                      <Check size={17} className="shrink-0 text-primary" />
                      {objective}
                    </li>
                  ))}
                </ul>
              </>
            )}

            {activeTab === "Contenidos" && (
              <>
                <h2 className="mb-4 text-base font-bold text-ink">
                  Contenido del programa
                </h2>

                <div className="space-y-3">
                  {(course.contents || [
                    "Introducción y fundamentos",
                    "Conceptos y herramientas principales",
                    "Ejercicios prácticos",
                    "Proyecto final",
                  ]).map((content, index) => (
                    <div
                      key={index}
                      className="rounded-lg border border-border bg-white p-4 text-sm text-ink"
                    >
                      Módulo {index + 1}: {content}
                    </div>
                  ))}
                </div>
              </>
            )}

            {activeTab === "Instructor" && (
              <>
                <h2 className="mb-3 text-base font-bold text-ink">
                  Instructor
                </h2>

                <p className="text-sm text-muted">
                  {course.instructor || "Información del instructor próximamente."}
                </p>
              </>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
