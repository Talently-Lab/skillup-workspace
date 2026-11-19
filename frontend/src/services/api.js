import axios from 'axios'
import { sessionStore } from './sessionStore'
import {
  claimSessionEnd,
  publishSessionEnded,
  setSessionExpiredNotice,
} from './sessionEvents'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 60000, // el servidor gratuito puede tardar en despertar
})

api.interceptors.request.use((config) => {
  const session = sessionStore.load()
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const requestUrl = error.config?.url ?? ''
    const isLoginRequest = /\/auth\/login(?:\?|$)/.test(requestUrl)

    if (
      status === 401 &&
      !isLoginRequest &&
      sessionStore.load()?.token &&
      claimSessionEnd()
    ) {
      sessionStore.clear()
      setSessionExpiredNotice()
      publishSessionEnded('expired')
      window.location.replace('/login')
    }

    return Promise.reject(error)
  },
)

export default api
