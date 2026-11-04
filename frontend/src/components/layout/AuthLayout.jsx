import Navbar from './Navbar'

export default function AuthLayout({
  legalIntro,
  image,
  children,
}) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <Navbar />

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