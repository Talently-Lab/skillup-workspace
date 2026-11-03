import axios from 'axios'
import { sessionStore } from './sessionStore'

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

export default api