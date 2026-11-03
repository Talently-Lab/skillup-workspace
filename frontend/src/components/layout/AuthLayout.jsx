import { Link } from 'react-router-dom'

export default function AuthLayout({
  prompt,
  linkLabel,
  linkTo,
  legalIntro,
  image,
  children,
}) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
          <Link to="/">
            <img src="/images/logo.svg" alt="SkillUp Campus" className="h-8" />
          </Link>
          <p className="text-sm text-muted">
            {prompt}{' '}
            <Link
              to={linkTo}
              className="font-medium text-primary underline underline-offset-4 hover:text-primary-hover"
            >
              {linkLabel}
            </Link>
          </p>
        </div>
      </header>

      <div className="grid flex-1 lg:grid-cols-[1fr_clamp(380px,41%,588px)]">
        <main className="flex flex-col justify-between px-6 py-10 sm:px-12 lg:px-20">
          <div className="mx-auto w-full max-w-sm">{children}</div>

          <p className="mx-auto mt-10 max-w-sm text-center text-xs text-muted">
            {legalIntro}{' '}
            <span className="underline">Términos y Condiciones</span> y nuestra
            política sobre <span className="underline">Protección de Datos</span>.
          </p>
        </main>

        {image && (
          <aside className="hidden bg-secondary lg:block">
            <img src={image} alt="" className="h-full w-full object-cover" />
          </aside>
        )}
      </div>
    </div>
  )
}