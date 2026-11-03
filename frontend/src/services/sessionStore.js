const KEY = 'skillup_session'

export const sessionStore = {
  load() {
    try {
      const raw = localStorage.getItem(KEY) ?? sessionStorage.getItem(KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  },
  save(session, remember) {
    sessionStore.clear()
    const storage = remember ? localStorage : sessionStorage
    storage.setItem(KEY, JSON.stringify(session))
  },
  clear() {
    localStorage.removeItem(KEY)
    sessionStorage.removeItem(KEY)
  },
}