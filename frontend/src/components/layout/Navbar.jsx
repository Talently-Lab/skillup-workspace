import { Link, NavLink } from 'react-router-dom'
import { ChevronDown } from 'lucide-react'
import Button from '../ui/Button'

const links = [
  { to: '/cursos', label: 'Cursos' },
  { to: '/nosotros', label: 'Nosotros' },
  { to: '/blog', label: 'Blog' },
]

export default function Navbar() {
  return (
    <header className="border-b border-border bg-surface">
      <nav className="mx-auto grid max-w-7xl grid-cols-[auto_1fr_auto] items-center gap-6 px-4 py-5">
        <Link to="/">
          <img src="/images/logo.svg" alt="SkillUp Campus" className="h-9" />
        </Link>

        <ul className="hidden justify-center gap-10 md:flex">
          {links.map(({ to, label }) => (
            <li key={to}>
              <NavLink
                to={to}
                className="flex items-center gap-1 text-base text-ink/80 hover:text-primary"
              >
                {label} <ChevronDown size={16} strokeWidth={1.5} />
              </NavLink>
            </li>
          ))}
        </ul>

        <div className="col-start-3 flex items-center gap-5">
          <Link to="/login" className="text-base text-ink/70 hover:text-primary">
            Iniciar sesión
          </Link>
          <Button to="/registro">Registrarse</Button>
        </div>
      </nav>
    </header>
  )
}