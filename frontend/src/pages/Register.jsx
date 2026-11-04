import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import AuthLayout from '../components/layout/AuthLayout'
import Input from '../components/ui/Input'
import Button from '../components/ui/Button'
import { validateRegister } from '../utils/validators'
import { register, getAuthErrorMessage } from '../services/authService'
import { useAuth } from '../context/useAuth'
import { enrollInCourse, getCourse } from '../services/courseService'
import { getPendingCourseId, isAlreadyEnrolled } from '../utils/enrollmentErrors'

const initialForm = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
}

export default function Register() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, login } = useAuth()
  const pendingCourseId = getPendingCourseId(location.state)
  const initialCourse = location.state?.pendingEnrollment?.course
  const validInitialCourse = initialCourse && String(initialCourse.id) === String(pendingCourseId)
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [enrollmentError, setEnrollmentError] = useState('')
  const [accountCreated, setAccountCreated] = useState(false)
  const [selectedCourse, setSelectedCourse] = useState(validInitialCourse ? initialCourse : null)
  const [loading, setLoading] = useState(false)

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
    setErrors((prev) => ({ ...prev, [name]: undefined }))
    setServerError('')
  }

  const continueEnrollment = async (course = selectedCourse) => {
    try {
      await login({ email: form.email, password: form.password })
    } catch {
      navigate('/login', {
        replace: true,
        state: { pendingEnrollment: { courseId: pendingCourseId, course } },
      })
      return
    }

    setForm((currentForm) => ({ ...currentForm, password: '' }))
    await completeEnrollment(course)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (accountCreated) return

    const validationErrors = validateRegister(form)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    setLoading(true)
    let course = selectedCourse
    try {
      if (pendingCourseId) {
        if (!course) course = await getCourse(pendingCourseId)
        if (course.isActive === false) {
          setServerError('Este curso ya no está disponible para inscripción.')
          return
        }
        setSelectedCourse(course)
      }

      try {
        await register({
          firstName: form.firstName,
          lastName: form.lastName,
          email: form.email,
          password: form.password,
        })
      } catch (error) {
        if (pendingCourseId && error.response?.status === 409) {
          setAccountCreated(true)
          await continueEnrollment(course)
          return
        }
        throw error
      }

      if (!pendingCourseId) {
        navigate('/login', { replace: true, state: { registered: true } })
        return
      }

      setAccountCreated(true)
      await continueEnrollment(course)
    } catch (error) {
      if (pendingCourseId && error.response?.status === 404) {
        setServerError('Este curso ya no está disponible para inscripción.')
      } else if (pendingCourseId) {
        setServerError('No pudimos verificar el curso. Inténtalo nuevamente.')
      } else {
        setServerError(getAuthErrorMessage(error))
      }
    } finally {
      setLoading(false)
    }
  }

  const completeEnrollment = async (course = selectedCourse) => {
    try {
      await enrollInCourse(pendingCourseId)
      navigate('/inscripcion/confirmacion', {
        replace: true,
        state: { courseId: pendingCourseId, course, enrollmentConfirmed: true },
      })
    } catch (error) {
      if (error.response?.status === 401) {
        navigate('/login', {
          replace: true,
          state: {
            registered: true,
            pendingEnrollment: { courseId: pendingCourseId, course },
          },
        })
      } else if (isAlreadyEnrolled(error)) {
        navigate('/inscripcion/confirmacion', {
          replace: true,
          state: { courseId: pendingCourseId, course, alreadyEnrolled: true },
        })
      } else {
        setEnrollmentError(
          error.response?.status === 404
            ? 'Este curso ya no está disponible para inscripción.'
            : 'No pudimos completar la inscripción. Tu sesión sigue activa; puedes intentarlo nuevamente.',
        )
      }
    }
  }

  const retryEnrollment = async () => {
    if (!isAuthenticated) {
      navigate('/login', {
        state: {
          registered: true,
          pendingEnrollment: { courseId: pendingCourseId, course: selectedCourse },
        },
      })
      return
    }
    setEnrollmentError('')
    setLoading(true)
    try {
      await completeEnrollment()
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      legalIntro="Al registrarte, estás aceptando nuestros"
    >
      <h1 className="text-2xl font-bold text-ink">Crear cuenta</h1>
      <p className="mt-1 text-sm text-muted">Completá tus datos para comenzar.</p>

      <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
        <fieldset className="flex flex-col gap-4" disabled={loading || accountCreated}>
        <Input
          label="Nombre"
          name="firstName"
          autoComplete="given-name"
          placeholder="Tu nombre"
          value={form.firstName}
          onChange={handleChange}
          error={errors.firstName}
        />
        <Input
          label="Apellido"
          name="lastName"
          autoComplete="family-name"
          placeholder="Tu apellido"
          value={form.lastName}
          onChange={handleChange}
          error={errors.lastName}
        />
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
          autoComplete="new-password"
          placeholder="Ingresá tu contraseña"
          value={form.password}
          onChange={handleChange}
          error={errors.password}
        />
        </fieldset>

        {serverError && (
          <p role="alert" className="rounded-xl bg-danger/10 px-4 py-3 text-sm text-danger">
            {serverError}
          </p>
        )}

          {enrollmentError && (
          <p role="alert" className="rounded-xl bg-danger/10 px-4 py-3 text-sm text-danger">
            {enrollmentError}
          </p>
        )}

        {accountCreated ? (
          <Button type="button" disabled={loading} onClick={retryEnrollment} className="mt-2 w-full">
            {loading ? 'Procesando inscripción...' : 'Reintentar inscripción'}
          </Button>
        ) : (
          <Button type="submit" disabled={loading} className="mt-2 w-full">
            {loading ? 'Creando cuenta...' : 'Registrarme'}
          </Button>
        )}

        {pendingCourseId && (
          <Link className="text-center text-sm font-medium text-primary hover:underline" to="/cursos">
            Cancelar y volver al catálogo
          </Link>
        )}
      </form>
    </AuthLayout>
  )
}