import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthContext } from './AuthContext'
import * as authService from '../services/authService'
import { sessionStore } from '../services/sessionStore'
import { getHomeForRole } from '../utils/roles'
import {
  claimSessionEnd,
  publishSessionEnded,
  publishSessionStarted,
  setSessionExpiredNotice,
  subscribeToSessionEvents,
} from '../services/sessionEvents'

export default function AuthProvider({ children }) {
  const navigate = useNavigate()
  const [session, setSession] = useState(() => sessionStore.load())

  useEffect(() => subscribeToSessionEvents((message) => {
    if (message.type === 'session-started' && message.session?.token) {
      try {
        sessionStore.saveToCurrentTab(message.session)
      } catch {
        // Keep the receiving tab authenticated in memory if storage is unavailable.
      }
      setSession(message.session)
      if (window.location.pathname === '/login') {
        navigate(getHomeForRole(message.session.user?.role), { replace: true })
      }
      return
    }

    if (message.type !== 'session-ended' || !claimSessionEnd()) return
    sessionStore.clear()
    setSession(null)
    if (message.reason === 'expired') setSessionExpiredNotice()
    window.location.replace('/login')
  }), [navigate])

  const login = async (credentials) => {
    const data = await authService.login(credentials)
    const newSession = { token: data.token, user: data.user }
    sessionStore.saveShared(newSession)
    setSession(newSession)
    publishSessionStarted(newSession)
    return newSession.user
  }

  const logout = () => {
    sessionStore.clear()
    setSession(null)
    publishSessionEnded('logout')
  }

  const value = {
    user: session?.user ?? null,
    token: session?.token ?? null,
    isAuthenticated: Boolean(session?.token),
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
