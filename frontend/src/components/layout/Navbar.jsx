import { useEffect, useRef, useState } from 'react'
import { ChevronDown, Menu, UserRound, X } from 'lucide-react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'
import { getHomeForRole } from '../../utils/roles'
import Button from '../ui/Button'

const links = [
  { to: '/cursos', label: 'Cursos', available: true },
  { to: '/nosotros', label: 'Nosotros', available: true },
  { label: 'Blog', available: false },
]

export default function Navbar({ menuOpen = false, onMenuToggle, sticky = false }) {
  const { user, isAuthenticated, logout } = useAuth()
  const location = useLocation()
  const isHomePage = location.pathname === '/'
  const [accountMenuOpen, setAccountMenuOpen] = useState(false)
  const menuRef = useRef(null)
  const initials = [user?.firstName?.[0], user?.lastName?.[0]]
    .filter(Boolean)
    .join('')
    .toUpperCase() || user?.email?.[0]?.toUpperCase()
  const displayName = [user?.firstName, user?.lastName].filter(Boolean).join(' ')

  useEffect(() => {
    if (!accountMenuOpen) return undefined

    const closeOnOutsideClick = (event) => {
      if (!menuRef.current?.contains(event.target)) setAccountMenuOpen(false)
    }
    const closeOnEscape = (event) => {
      if (event.key === 'Escape') setAccountMenuOpen(false)
    }

    document.addEventListener('mousedown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [accountMenuOpen])

  return (
    <header className={`${sticky ? 'sticky top-0' : 'relative'} z-30 border-b border-border bg-surface`}>
      <nav
        aria-label="Navegación principal"
        className={`flex w-full items-center justify-between gap-3 px-4 sm:px-5 md:grid md:grid-cols-[auto_1fr_auto] md:gap-4 md:px-8 xl:px-12 lg:gap-6 ${
          isHomePage ? 'py-2 md:py-2' : 'py-3 md:py-5'
        }`}
      >
        <Link aria-label="SkillUp Campus, inicio" className="shrink-0" to="/">
          <img alt="" className="size-9 w-[30px] object-contain md:hidden" src="/images/logo1.png" />
          <img alt="SkillUp Campus" className="hidden h-9 md:block" src="/images/logo.svg" />
        </Link>

        <ul className="hidden justify-center gap-10 md:flex">
          {links.map(({ to, label, available }) => (
            <li key={label}>
              {available ? (
                <NavLink className="text-base text-ink/80 hover:text-primary" to={to}>
                  {label}
                </NavLink>
              ) : (
                <span
                  aria-disabled="true"
                  className="cursor-not-allowed text-base text-ink/50"
                  title="Blog todavía no está disponible"
                >
                  {label}
                </span>
              )}
            </li>
          ))}
        </ul>

        <div className="flex shrink-0 items-center gap-2 sm:gap-4 md:col-start-3 md:gap-5">
          {onMenuToggle && (
            <button
              aria-controls="student-sidebar"
              aria-expanded={menuOpen}
              aria-label={menuOpen ? 'Cerrar navegación' : 'Abrir navegación'}
              className="flex size-10 items-center justify-center rounded-md text-slate-600 transition hover:bg-slate-100 hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30 lg:hidden"
              onClick={onMenuToggle}
              type="button"
            >
              {menuOpen ? <X aria-hidden="true" size={23} /> : <Menu aria-hidden="true" size={23} />}
            </button>
          )}
          {isAuthenticated ? (
            <div className="relative" ref={menuRef}>
              <button
                aria-expanded={accountMenuOpen}
                aria-haspopup="menu"
                aria-label="Abrir menú de cuenta"
                className="flex min-h-10 items-center gap-1.5 rounded-full p-1 text-slate-700 transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/30"
                onClick={() => setAccountMenuOpen((open) => !open)}
                type="button"
              >
                <span
                  aria-hidden="true"
                  className="flex size-8 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary"
                >
                  {initials || <UserRound size={17} />}
                </span>
                <ChevronDown aria-hidden="true" size={16} />
              </button>
              {accountMenuOpen && (
                <div
                  className="absolute right-0 top-full z-40 mt-2 min-w-48 rounded-xl border border-slate-200 bg-white p-1.5 shadow-lg"
                  role="menu"
                >
                  <p className="truncate px-3 py-2 text-xs font-medium text-slate-500">
                    {displayName || user?.email}
                  </p>
                  <Link
                    className="block rounded-lg px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
                    onClick={() => setAccountMenuOpen(false)}
                    role="menuitem"
                    to={getHomeForRole(user?.role)}
                  >
                    Mi panel
                  </Link>
                  <button
                    className="w-full rounded-lg px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-slate-50"
                    onClick={() => {
                      setAccountMenuOpen(false)
                      logout()
                      window.location.replace('/')
                    }}
                    role="menuitem"
                    type="button"
                  >
                    Cerrar sesión
                  </button>
                </div>
              )}
            </div>
          ) : (
            <>
              <Link
                className="whitespace-nowrap text-sm text-ink/70 hover:text-primary sm:text-base"
                to="/login"
              >
                Iniciar sesión
              </Link>
              <Button
                className="whitespace-nowrap px-3 py-2 text-sm sm:px-4 sm:py-2.5 md:text-base"
                to="/registro"
              >
                Registrarse
              </Button>
            </>
          )}
        </div>
      </nav>
    </header>
  )
}
