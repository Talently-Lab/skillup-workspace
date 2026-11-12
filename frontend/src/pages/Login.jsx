import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import AuthLayout from '../components/layout/AuthLayout'
import Input from '../components/ui/Input'
import Button from '../components/ui/Button'
import { useAuth } from '../context/useAuth'
import { validateLogin } from '../utils/validators'
import { getAuthErrorMessage } from '../services/authService'
import { getHomeForRole } from '../utils/roles'
import { enrollInCourse } from '../services/courseService'
import { getPendingCourseId, isAlreadyEnrolled } from '../utils/enrollmentErrors'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const pendingCourseId = getPendingCourseId(location.state)
  const pendingCourse = location.state?.pendingEnrollment?.course

  const [form, setForm] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: undefined }))
    setServerError('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()

    const validationErrors = validateLogin(form)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    setLoading(true)
    try {
      const user = await login(form)
      if (pendingCourseId) {
        try {
          await enrollInCourse(pendingCourseId)
          navigate('/inscripcion/confirmacion', {
            replace: true,
            state: {
              courseId: pendingCourseId,
              course: pendingCourse,
              enrollmentConfirmed: true,
            },
          })
        } catch (error) {
          if (isAlreadyEnrolled(error)) {
            navigate('/inscripcion/confirmacion', {
              replace: true,
              state: { courseId: pendingCourseId, course: pendingCourse, alreadyEnrolled: true },
            })
          } else {
            navigate(`/cursos/${pendingCourseId}`, {
              replace: true,
              state: {
                enrollmentError: error.response?.status === 404
                  ? 'Este curso ya no está disponible para inscripción.'
                  : 'No pudimos completar la inscripción. Tu sesión sigue activa; puedes intentarlo nuevamente.',
              },
            })
          }
        }
        return
      }

      const returnTo = location.state?.from
      navigate(
        returnTo
          ? `${returnTo.pathname}${returnTo.search ?? ''}${returnTo.hash ?? ''}`
          : getHomeForRole(user?.role),
        { replace: true },
      )
    } catch (error) {
      setServerError(getAuthErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      legalIntro="Al iniciar sesión, estás aceptando nuestros"
      image="/images/login.jpg"
    >
      <h1 className="text-2xl font-bold text-ink">Iniciar sesión</h1>
      <p className="mt-1 text-sm text-muted">
        Ingresá con tu cuenta para acceder a la plataforma.
      </p>

      {location.state?.registered && (
        <p role="status" className="mt-4 rounded-xl bg-success/10 px-4 py-3 text-sm text-success">
          Tu cuenta se creó correctamente. Ya podés iniciar sesión.
        </p>
      )}

      <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-5">
        <Input
          label="Correo electrónico"
          name="email"
          type="email"
          autoComplete="email"
          placeholder="m@example.com"
          value={form.email}
          onChange={handleChange}
          error={errors.email}
        />

        <Input
          label="Contraseña"
          name="password"
          type="password"
          autoComplete="current-password"
          placeholder="Ingresá tu contraseña"
          value={form.password}
          onChange={handleChange}
          error={errors.password}
        />

        {serverError && (
          <p role="alert" className="rounded-xl bg-danger/10 px-4 py-3 text-sm text-danger">
            {serverError}
          </p>
        )}

        <Button type="submit" disabled={loading} className="w-full">
          {loading ? 'Ingresando...' : 'Iniciar sesión'}
        </Button>
      </form>
    </AuthLayout>
  )
}