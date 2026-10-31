import Button from '../ui/Button'

export default function Hero() {
  return (
    <section className="mx-auto grid max-w-7xl items-center gap-10 px-4 py-10 md:grid-cols-[1.1fr_1fr] md:py-14">
      <div className="flex flex-col items-start gap-5">
        <h1 className="text-3xl font-bold leading-tight text-ink md:text-4xl lg:text-5xl">
          Nuevas habilidades,
          <span className="block text-primary">nuevas oportunidades.</span>
        </h1>

        <p className="max-w-sm text-muted">
          Cursos online para desarrollar nuevas habilidades y potenciar tu
          futuro profesional.
        </p>

        <Button to="/cursos">Explorar cursos</Button>
      </div>

      <div className="relative">
        {/* Decoraciones: van antes de la imagen para quedar detrás */}
        <div className="absolute -left-16 top-0 h-36 w-40 rounded-3xl bg-secondary" />
        <div className="absolute -bottom-5 -right-6 h-24 w-48 rounded-3xl bg-secondary" />

        <img
          src="/images/hero.jpg"
          alt="Estudiante tomando notas frente a una laptop"
          fetchpriority="high"
          className="relative aspect-[5/3] w-full rounded-2xl bg-secondary object-cover"
        />
      </div>
    </section>
  )
}