import Button from '../ui/Button'

export default function Hero() {
  return (
    <section className="mx-auto grid w-full max-w-[1200px] items-center gap-8 px-4 py-8 sm:px-6 md:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] md:gap-8 md:py-8 lg:gap-10 lg:px-8 xl:max-w-[1360px] xl:px-10">
      <div className="flex flex-col items-start gap-5">
        <h1 className="text-[34px] font-bold leading-tight text-ink md:text-[36px] lg:text-[42px] xl:text-[46px]">
          Nuevas habilidades,
          <span className="block text-primary">nuevas oportunidades.</span>
        </h1>

        <p className="max-w-md text-muted">
          Cursos online para desarrollar nuevas habilidades y potenciar tu
          futuro profesional.
        </p>

        <Button to="/cursos">Explorar cursos</Button>
      </div>

      <div className="relative isolate lg:w-full lg:max-w-[440px] lg:justify-self-end xl:max-w-[500px]">
        <div aria-hidden="true" className="absolute -left-4 top-5 z-0 h-24 w-28 rounded-3xl bg-secondary sm:-left-5 sm:h-28 sm:w-36" />
        <div aria-hidden="true" className="absolute -bottom-3 -right-3 z-0 h-16 w-36 rounded-3xl bg-secondary sm:h-20 sm:w-44" />

        <img
          src="/images/hero.jpg"
          alt="Estudiante tomando notas frente a una laptop"
          fetchPriority="high"
          className="relative z-10 aspect-[5/3] w-full rounded-2xl bg-secondary object-cover"
        />
      </div>
    </section>
  )
}