import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthLayout from '../components/layout/AuthLayout'
import Input from '../components/ui/Input'
import Button from '../components/ui/Button'
import { validateRegister } from '../utils/validators'
import { register, getAuthErrorMessage } from '../services/authService'

const initialForm = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
}

export default function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState(initialForm)
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

    const validationErrors = validateRegister(form)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    setLoading(true)
    try {
      await register({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        password: form.password,
      })
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (error) {
      setServerError(getAuthErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      prompt="¿Ya tenés cuenta?"
      linkLabel="Iniciar sesión"
      linkTo="/login"
      legalIntro="Al registrarte, estás aceptando nuestros"
    >
      <h1 className="text-2xl font-bold text-ink">Crear cuenta</h1>
      <p className="mt-1 text-sm text-muted">Completá tus datos para comenzar.</p>

      <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
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

        {serverError && (
          <p role="alert" className="rounded-xl bg-danger/10 px-4 py-3 text-sm text-danger">
            {serverError}
          </p>
        )}

        <Button type="submit" disabled={loading} className="mt-2 w-full">
          {loading ? 'Creando cuenta...' : 'Registrarme'}
        </Button>

        {loading && (
          <p className="text-center text-xs text-muted">
            La primera vez puede tardar unos segundos mientras el servidor se activa.
          </p>
        )}
      </form>
    </AuthLayout>
  )
}