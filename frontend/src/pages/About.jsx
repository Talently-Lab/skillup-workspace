import {
  ArrowDownRight,
  ArrowRight,
  BookOpen,
  BrainCircuit,
  BriefcaseBusiness,
  Check,
  Code2,
  Compass,
  Lightbulb,
  MonitorPlay,
  Palette,
  Rocket,
  Sparkles,
  Target,
  UsersRound,
} from 'lucide-react'
import Button from '../components/ui/Button'

const TEAM = [
  { name: 'Yusleidy Gamboa', role: 'Desarrolladora Backend (NodeJS)', initials: 'YG', icon: Code2, color: 'bg-blue-100 text-blue-800', tint: 'from-blue-500 to-indigo-600' },
  { name: 'Alexis Rodríguez', role: 'Desarrollador Backend (NodeJS)', initials: 'AR', icon: Code2, color: 'bg-blue-100 text-blue-800', tint: 'from-sky-500 to-blue-700' },
  { name: 'Sabrina Ojeda', role: 'Desarrolladora Frontend (React)', initials: 'SO', icon: MonitorPlay, color: 'bg-cyan-100 text-cyan-800', tint: 'from-cyan-400 to-blue-600' },
  { name: 'David Alejandro Pérez Pérez', role: 'Desarrollador Frontend (React)', initials: 'DP', icon: MonitorPlay, color: 'bg-cyan-100 text-cyan-800', tint: 'from-indigo-400 to-blue-600' },
  { name: 'Julieta Di Cesare', role: 'Diseñadora UX/UI', initials: 'JD', icon: Palette, color: 'bg-violet-100 text-violet-800', tint: 'from-violet-400 to-fuchsia-600' },
  { name: 'Nicolás Toloza', role: 'Project Manager', initials: 'NT', icon: Compass, color: 'bg-teal-100 text-teal-800', tint: 'from-teal-400 to-emerald-600' },
  { name: 'María Jimena Quintana', role: 'Tester QA Manual', initials: 'MQ', icon: Check, color: 'bg-orange-100 text-orange-800', tint: 'from-orange-400 to-rose-500' },
]

const VALUES = [
  {
    title: 'Aprendizaje accesible',
    description: 'Un espacio digital para acercar nuevas experiencias de aprendizaje.',
    icon: BookOpen,
    style: 'bg-blue-50 text-blue-700',
  },
  {
    title: 'Desarrollo de habilidades',
    description: 'Contenidos pensados para acompañar el crecimiento de cada persona.',
    icon: BrainCircuit,
    style: 'bg-violet-50 text-violet-700',
  },
  {
    title: 'Experiencia digital',
    description: 'Herramientas digitales que organizan y hacen más cercano el aprendizaje.',
    icon: MonitorPlay,
    style: 'bg-cyan-50 text-cyan-700',
  },
  {
    title: 'Crecimiento continuo',
    description: 'La curiosidad y las ganas de aprender pueden abrir nuevos caminos.',
    icon: Rocket,
    style: 'bg-orange-50 text-orange-700',
  },
]

const TECHNOLOGIES = [
  {
    name: 'React',
    purpose: 'Desarrollo frontend',
    logo: '/images/tech/react.svg',
  },
  {
    name: 'Java Spring Boot',
    purpose: 'Desarrollo backend',
    logo: '/images/tech/spring-boot.svg',
  },
  {
    name: 'Cloudflare R2',
    purpose: 'Almacenamiento de archivos',
    logo: '/images/tech/cloudflare.svg',
  },
  {
    name: 'Node.js',
    purpose: 'Herramientas y entorno de desarrollo',
    logo: '/images/tech/nodejs.svg',
  },
  {
    name: 'Figma',
    purpose: 'Diseño UX/UI',
    logo: '/images/tech/figma.svg',
  },
  {
    name: 'Postman',
    purpose: 'Pruebas y validación de API',
    logo: '/images/tech/postman.svg',
  },
]

function TeamCard({ member, index }) {
  const Icon = member.icon
  return (
    <article className="group relative overflow-hidden rounded-[22px] border border-slate-200/80 bg-white p-5 transition duration-300 hover:-translate-y-1 hover:border-blue-200 hover:shadow-[0_20px_50px_-28px_rgba(37,76,180,0.35)]">
      <div className={`absolute inset-x-0 top-0 h-1 bg-gradient-to-r ${member.tint}`} />
      <div className="flex items-start justify-between gap-3">
        <div className={`relative flex size-[68px] items-center justify-center rounded-[21px] bg-gradient-to-br ${member.tint} text-xl font-bold tracking-tight text-white shadow-lg shadow-blue-900/10`}>
          {member.initials}
          <span className="absolute -bottom-1.5 -right-1.5 flex size-7 items-center justify-center rounded-lg border-2 border-white bg-white text-slate-600 shadow-sm">
            <Icon size={14} strokeWidth={2} />
          </span>
        </div>
        <span className="mt-1 text-xs font-semibold tracking-[0.18em] text-slate-300">
          {String(index + 1).padStart(2, '0')}
        </span>
      </div>
      <h3 className="mt-6 min-h-12 text-[17px] font-bold leading-snug text-slate-900">
        {member.name}
      </h3>
      <span className={`mt-3 inline-flex max-w-full items-center rounded-full px-3 py-1.5 text-xs font-semibold leading-snug ${member.color}`}>
        {member.role}
      </span>
    </article>
  )
}

export default function About() {
  return (
    <div className="overflow-hidden bg-white text-slate-900">
      <section className="relative isolate overflow-hidden bg-[radial-gradient(ellipse_at_78%_45%,rgba(49,92,255,0.09),transparent_40%),linear-gradient(180deg,#f8faff_0%,#ffffff_100%)]">
        <div aria-hidden="true" className="absolute -right-28 top-0 -z-10 size-[460px] rounded-full border border-blue-100/70" />
        <div aria-hidden="true" className="absolute -right-4 top-16 -z-10 size-[310px] rounded-full border border-indigo-100/70" />
        <div className="mx-auto grid max-w-7xl items-center gap-10 px-5 py-12 sm:px-8 sm:py-16 lg:min-h-[590px] lg:grid-cols-[0.96fr_1.04fr] lg:gap-14 lg:px-10 lg:py-16">
          <div className="relative z-10">
            <div className="inline-flex items-center gap-2.5 rounded-full border border-blue-100 bg-white/90 px-4 py-2.5 shadow-sm shadow-blue-900/5">
              <img alt="Talently Lab" className="size-7 object-contain" src="/images/logo_talenty_lab.png" />
              <span className="text-[11px] font-bold uppercase tracking-[0.17em] text-blue-900 sm:text-xs">
                Impulsado por <span className="ml-1 text-primary">Talently Lab</span>
              </span>
            </div>
            <h1 className="mt-7 max-w-[590px] text-[clamp(2.5rem,4.2vw,3.125rem)] font-bold leading-[1.12] tracking-[-0.045em] text-slate-950">
              Aprender,{' '}
              <span className="bg-gradient-to-r from-primary to-blue-500 bg-clip-text text-transparent">practicar</span>{' '}
              y{' '}
              <span className="bg-gradient-to-r from-primary to-indigo-500 bg-clip-text text-transparent">crecer</span>{' '}
              en tecnología.
            </h1>
            <p className="mt-6 max-w-[570px] text-[15px] leading-7 text-slate-600 sm:text-base sm:leading-7">
              SkillUp Campus es una plataforma educativa que conecta el aprendizaje con experiencias prácticas, brindando a las personas la oportunidad de desarrollar habilidades y acercarse al mundo profesional de la tecnología.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3 sm:gap-4">
              <Button className="gap-2 px-6 py-3.5 shadow-lg shadow-blue-600/20" to="/cursos">
                Explorar cursos <ArrowRight aria-hidden="true" size={18} />
              </Button>
              <a className="inline-flex items-center gap-2 rounded-md px-2 py-3 text-sm font-semibold text-slate-600 transition hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30" href="#equipo">
                Conoce nuestro equipo <ArrowDownRight aria-hidden="true" size={17} />
              </a>
            </div>
            <div className="mt-8 flex items-center gap-3 border-t border-blue-100 pt-5 text-sm font-medium text-slate-600">
              <span className="flex -space-x-2">
                {TEAM.slice(0, 3).map((member) => (
                  <span
                    aria-hidden="true"
                    className={`flex size-9 items-center justify-center rounded-full border-2 border-white bg-gradient-to-br text-[10px] font-bold text-white ${member.tint}`}
                    key={member.initials}
                  >
                    {member.initials}
                  </span>
                ))}
              </span>
              <span>Un proyecto que conecta aprendizaje y tecnología</span>
            </div>
          </div>
          <div className="relative mx-auto w-full max-w-[600px]">
            <div aria-hidden="true" className="absolute -inset-3 rounded-[34px] bg-gradient-to-br from-blue-100 via-indigo-50 to-cyan-100 sm:-inset-4 sm:rounded-[40px]" />
            <div className="relative overflow-hidden rounded-[28px] bg-blue-100 shadow-[0_30px_75px_-34px_rgba(37,76,180,0.45)] sm:rounded-[34px]">
              <img
                alt="Estudiante aprendiendo con una laptop y cuadernos"
                className="aspect-[1.2] w-full object-cover object-center"
                src="/images/hero.jpg"
              />
              <div aria-hidden="true" className="absolute inset-0 bg-gradient-to-t from-blue-950/25 via-transparent to-white/10" />
              <div className="absolute bottom-4 left-4 right-4 flex items-center gap-3 rounded-2xl border border-white/70 bg-white/90 p-3.5 shadow-lg shadow-blue-950/10 backdrop-blur-sm sm:bottom-5 sm:left-5 sm:right-5 sm:p-4">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-primary">
                  <BookOpen size={20} />
                </span>
                <span className="min-w-0">
                  <span className="block text-sm font-bold text-slate-900">Aprendizaje con propósito</span>
                  <span className="mt-0.5 block text-xs text-slate-600">Conocimiento que se lleva a la práctica</span>
                </span>
                <ArrowRight aria-hidden="true" className="ml-auto shrink-0 text-primary" size={18} />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="relative mx-auto grid max-w-7xl gap-10 px-5 py-16 sm:px-8 sm:py-20 lg:grid-cols-[0.8fr_1.2fr] lg:gap-20 lg:px-10 lg:py-28">
        <div>
          <span className="inline-flex items-center gap-2 rounded-full bg-indigo-50 px-3 py-1.5 text-xs font-bold uppercase tracking-[0.14em] text-indigo-700">
            <Lightbulb size={14} /> Nuestra historia
          </span>
          <h2 className="mt-5 max-w-lg text-3xl font-bold leading-tight tracking-tight text-slate-950 sm:text-4xl">
            Una idea que se convirtió en proyecto.
          </h2>
        </div>
        <div className="relative overflow-hidden rounded-[28px] bg-slate-950 p-7 text-white sm:p-10">
          <div aria-hidden="true" className="absolute -right-14 -top-20 size-64 rounded-full bg-blue-600/30 blur-3xl" />
          <div aria-hidden="true" className="absolute -bottom-28 left-1/3 size-64 rounded-full bg-violet-600/20 blur-3xl" />
          <div className="relative">
            <div className="mb-8 flex items-center gap-2">
              <span className="size-2 rounded-full bg-cyan-300" />
              <span className="h-px flex-1 bg-gradient-to-r from-cyan-300/80 via-blue-400/50 to-transparent" />
              <span className="size-2 rounded-full bg-violet-300" />
              <span className="h-px flex-1 bg-gradient-to-r from-violet-300/70 to-transparent" />
              <span className="size-2 rounded-full bg-blue-300" />
            </div>
            <p className="max-w-3xl text-lg leading-8 text-blue-50 sm:text-[21px] sm:leading-9">
              SkillUp Campus surge de la iniciativa de construir un espacio digital de aprendizaje donde las personas puedan acceder a cursos, desarrollar nuevas habilidades y hacer seguimiento a su progreso académico.
            </p>
            <div className="mt-9 flex flex-wrap gap-2">
              {[
                { label: 'Aprender', icon: BookOpen },
                { label: 'Desarrollar', icon: BrainCircuit },
                { label: 'Avanzar', icon: ArrowRight },
              ].map(({ label, icon: Icon }) => (
                <span className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/[0.07] px-3.5 py-2 text-sm font-medium text-white/90" key={label}>
                  <Icon size={15} /> {label}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="bg-[#f6f8ff]">
        <div className="mx-auto max-w-7xl px-5 py-16 sm:px-8 sm:py-20 lg:px-10 lg:py-24">
          <div className="mb-9 max-w-2xl">
            <span className="text-sm font-bold uppercase tracking-[0.18em] text-primary">Lo que nos impulsa</span>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">Una visión compartida del aprendizaje.</h2>
          </div>
          <div className="grid gap-5 lg:grid-cols-2">
            <article className="group relative overflow-hidden rounded-[26px] bg-gradient-to-br from-blue-600 via-primary to-indigo-600 p-7 text-white shadow-xl shadow-blue-900/10 sm:p-9">
              <div aria-hidden="true" className="absolute -right-16 -top-16 size-56 rounded-full border-[34px] border-white/[0.08]" />
              <div className="relative">
                <span className="flex size-12 items-center justify-center rounded-2xl bg-white/15 ring-1 ring-white/20">
                  <Target size={25} />
                </span>
                <h3 className="mt-7 text-2xl font-bold">Nuestra misión</h3>
                <p className="mt-4 max-w-xl text-[15px] leading-7 text-blue-50 sm:text-base">
                  Facilitar experiencias de aprendizaje accesibles y organizadas mediante herramientas digitales que permitan a las personas adquirir conocimientos y desarrollar habilidades para su crecimiento personal y profesional.
                </p>
              </div>
            </article>
            <article className="group relative overflow-hidden rounded-[26px] border border-blue-100 bg-white p-7 shadow-lg shadow-blue-900/[0.04] sm:p-9">
              <div aria-hidden="true" className="absolute -right-12 -top-16 size-52 rounded-full bg-gradient-to-br from-indigo-50 to-cyan-50" />
              <div className="relative">
                <span className="flex size-12 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-700">
                  <Sparkles size={25} />
                </span>
                <h3 className="mt-7 text-2xl font-bold text-slate-950">Nuestra visión</h3>
                <p className="mt-4 max-w-xl text-[15px] leading-7 text-slate-600 sm:text-base">
                  Ser una plataforma de formación digital que impulse el aprendizaje continuo y contribuya al desarrollo de nuevas oportunidades académicas y profesionales.
                </p>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section className="relative overflow-hidden bg-slate-950">
        <div aria-hidden="true" className="absolute -left-40 top-0 size-[480px] rounded-full bg-blue-600/20 blur-3xl" />
        <div aria-hidden="true" className="absolute -right-24 bottom-0 size-[400px] rounded-full bg-violet-600/20 blur-3xl" />
        <div className="relative mx-auto max-w-7xl px-5 py-16 sm:px-8 sm:py-20 lg:px-10 lg:py-24" id="equipo">
          <div className="flex flex-col justify-between gap-5 md:flex-row md:items-end">
            <div className="max-w-2xl">
              <span className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/[0.07] px-3 py-1.5 text-xs font-semibold text-blue-200">
                <UsersRound size={14} /> Las personas detrás de la plataforma
              </span>
              <h2 className="mt-5 text-3xl font-bold leading-tight tracking-tight text-white sm:text-4xl lg:text-[44px]">
                Detrás de SkillUp Campus hay un gran equipo.
              </h2>
              <p className="mt-4 max-w-xl text-base leading-7 text-slate-300">
                Cada integrante aporta sus conocimientos y habilidades para hacer realidad esta plataforma.
              </p>
            </div>
            <span className="inline-flex w-fit items-center gap-2 rounded-full bg-white/10 px-4 py-2 text-sm font-medium text-white/80">
              <UsersRound size={16} /> 7 integrantes
            </span>
          </div>
          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4 lg:gap-5">
            {TEAM.map((member, index) => (
              <TeamCard index={index} key={member.name} member={member} />
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-5 py-16 sm:px-8 sm:py-20 lg:px-10 lg:py-24">
        <div className="mb-10 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="max-w-2xl">
            <span className="text-sm font-bold uppercase tracking-[0.18em] text-primary">Lo que nos caracteriza</span>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">Aprender para seguir creciendo.</h2>
          </div>
          <p className="max-w-md text-sm leading-6 text-slate-500">
            Una propuesta que conecta el aprendizaje con el desarrollo personal y profesional.
          </p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {VALUES.map(({ title, description, icon: Icon, style }, index) => (
            <article className={`relative overflow-hidden rounded-[22px] border border-slate-100 p-6 transition hover:-translate-y-1 hover:shadow-lg hover:shadow-blue-900/[0.06] ${index === 1 ? 'bg-[#f8f6ff]' : index === 3 ? 'bg-[#f4fbfa]' : 'bg-slate-50'}`} key={title}>
              <span className={`flex size-12 items-center justify-center rounded-2xl ${style}`}>
                <Icon size={23} strokeWidth={1.8} />
              </span>
              <h3 className="mt-5 text-lg font-bold text-slate-900">{title}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
              <span aria-hidden="true" className="absolute -bottom-5 -right-4 text-7xl font-black leading-none text-slate-900/[0.025]">
                0{index + 1}
              </span>
            </article>
          ))}
        </div>
      </section>

      <section className="bg-[#f6f8ff]">
        <div className="mx-auto max-w-7xl px-5 py-16 sm:px-8 sm:py-20 lg:px-10 lg:py-24">
          <div className="mx-auto mb-9 max-w-2xl text-center">
            <span className="text-sm font-bold uppercase tracking-[0.18em] text-primary">
              Tecnología que hace posible SkillUp Campus
            </span>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
              Tecnología que impulsa nuestras ideas.
            </h2>
            <p className="mx-auto mt-4 max-w-xl text-sm leading-6 text-slate-600 sm:text-base">
              Combinamos herramientas y tecnologías para construir una plataforma educativa moderna, funcional y preparada para crecer.
            </p>
          </div>
          <div className="grid grid-cols-1 gap-4 min-[420px]:grid-cols-2 lg:grid-cols-3 lg:gap-5">
            {TECHNOLOGIES.map(({ name, purpose, logo }) => (
              <article
                className="group flex min-h-[112px] items-center gap-4 rounded-2xl border border-blue-100/80 bg-white p-4 shadow-sm shadow-blue-900/[0.025] transition duration-200 hover:-translate-y-0.5 hover:border-blue-200 hover:shadow-md hover:shadow-blue-900/[0.06] sm:p-5"
                key={name}
              >
                <span className="flex size-14 shrink-0 items-center justify-center rounded-xl bg-slate-50 ring-1 ring-slate-100 transition group-hover:bg-blue-50">
                  <img alt={`${name} logo`} className="size-8 object-contain" loading="lazy" src={logo} />
                </span>
                <span className="min-w-0">
                  <span className="block text-base font-bold text-slate-900">{name}</span>
                  <span className="mt-1 block text-sm leading-5 text-slate-500">{purpose}</span>
                </span>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="px-5 pb-16 sm:px-8 sm:pb-20 lg:px-10 lg:pb-24">
        <div className="relative mx-auto max-w-7xl overflow-hidden rounded-[30px] bg-gradient-to-br from-blue-700 via-primary to-indigo-600 px-6 py-12 text-center text-white shadow-2xl shadow-blue-900/15 sm:px-12 sm:py-16">
          <div aria-hidden="true" className="absolute -left-20 -top-28 size-72 rounded-full border-[42px] border-white/[0.08]" />
          <div aria-hidden="true" className="absolute -bottom-36 -right-12 size-80 rounded-full bg-cyan-300/15 blur-2xl" />
          <div className="relative mx-auto max-w-3xl">
            <span className="mx-auto flex size-12 items-center justify-center rounded-2xl bg-white/15 ring-1 ring-white/20">
              <BriefcaseBusiness size={24} />
            </span>
            <h2 className="mt-6 text-3xl font-bold leading-tight tracking-tight sm:text-4xl lg:text-[42px]">
              El aprendizaje es el comienzo de nuevas oportunidades.
            </h2>
            <p className="mx-auto mt-4 max-w-2xl text-base leading-7 text-blue-50 sm:text-lg">
              Explora SkillUp Campus y descubre nuevas formas de aprender y desarrollar tus habilidades.
            </p>
            <Button
              className="mt-8 gap-2 border-white bg-white px-6 py-3.5 text-primary hover:border-blue-50 hover:bg-blue-50 hover:text-primary-hover"
              to="/cursos"
              variant="secondary"
            >
              Explorar cursos <ArrowRight aria-hidden="true" size={18} />
            </Button>
          </div>
        </div>
      </section>
    </div>
  )
}
