import api from './api'

const ENDPOINTS = {
  login: '/auth/login',
  register: '/auth/register',
}

function normalizeRole(role) {
  const value =
    typeof role === 'string' ? role : (role?.name ?? role?.authority ?? '')
  return value.replace(/^ROLE_/, '').toUpperCase()
}

function firstNonEmptyString(...values) {
  return values.find((value) => typeof value === 'string' && value.trim())?.trim() ?? ''
}

// ⚠️ A CONFIRMAR con la respuesta real del login (Postman).
// Es el único lugar donde se interpreta lo que devuelve el servidor.
function normalizeSession(data) {
  const token = data?.token ?? data?.accessToken ?? data?.jwt
  if (!token) {
    throw new Error('La respuesta del login no incluye el token')
  }

  const payload = data?.data ?? data
  const raw = payload?.user ?? payload?.usuario ?? payload
  return {
    token,
    user: {
      id: raw?.id ?? raw?.userId ?? payload?.id ?? data?.id,
      firstName: firstNonEmptyString(raw?.firstName, payload?.firstName, data?.firstName),
      lastName: firstNonEmptyString(raw?.lastName, payload?.lastName, data?.lastName),
      email: raw?.email ?? payload?.email ?? data?.email ?? '',
      role: normalizeRole(
        raw?.role ?? raw?.rol ?? raw?.roles?.[0] ?? payload?.role ?? data?.role,
      ),
    },
  }
}

export async function login({ email, password }) {
  const { data } = await api.post(ENDPOINTS.login, { email, password })
  return normalizeSession(data)
}

export async function register({ firstName, lastName, email, password }) {
  const { data } = await api.post(ENDPOINTS.register, {
    firstName,
    lastName,
    email,
    password,
  })
  return data
}

export function getAuthErrorMessage(error) {
  if (!error.response) {
    return 'No pudimos conectar con el servidor. Probá de nuevo en unos segundos.'
  }

  const serverMessage =
    typeof error.response.data?.message === 'string'
      ? error.response.data.message
      : null

  switch (error.response.status) {
    case 400:
      return serverMessage ?? 'Revisá los datos ingresados.'
    case 401:
    case 403: // algunos backends responden 403 ante credenciales inválidas
      return 'El correo o la contraseña son incorrectos.'
    case 409:
      return 'Ya existe una cuenta con ese correo.'
    default:
      return 'Ocurrió un error inesperado. Intentá de nuevo.'
  }
}