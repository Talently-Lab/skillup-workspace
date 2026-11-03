import { useState } from 'react'
import { AuthContext } from './AuthContext'
import * as authService from '../services/authService'
import { sessionStore } from '../services/sessionStore'

export default function AuthProvider({ children }) {
  const [session, setSession] = useState(() => sessionStore.load())

  const login = async (credentials, remember = false) => {
    const data = await authService.login(credentials)
    const newSession = { token: data.token, user: data.user }
    sessionStore.save(newSession, remember)
    setSession(newSession)
    return newSession.user
  }

  const logout = () => {
    sessionStore.clear()
    setSession(null)
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