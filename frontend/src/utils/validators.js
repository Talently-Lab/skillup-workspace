const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateEmail(email) {
  if (!email.trim()) return 'Ingresá tu correo electrónico'
  if (!EMAIL_REGEX.test(email)) return 'Ingresá un correo válido'
  return undefined
}

export function validateLogin({ email, password }) {
  const errors = {}
  const emailError = validateEmail(email)
  if (emailError) errors.email = emailError
  if (!password) errors.password = 'Ingresá tu contraseña'
  return errors
}

export function validateRegister({
  firstName,
  lastName,
  email,
  password,
  confirmPassword,
}) {
  const errors = {}

  if (!firstName.trim()) errors.firstName = 'Ingresá tu nombre'
  if (!lastName.trim()) errors.lastName = 'Ingresá tu apellido'

  const emailError = validateEmail(email)
  if (emailError) errors.email = emailError

  if (!password) {
    errors.password = 'Ingresá una contraseña'
  } else if (password.length < 8) {
    errors.password = 'La contraseña debe tener mínimo 8 caracteres'
  }

  if (!confirmPassword) {
    errors.confirmPassword = 'Repetí tu contraseña'
  } else if (password !== confirmPassword) {
    errors.confirmPassword = 'Las contraseñas no coinciden'
  }

  return errors
}