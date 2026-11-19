const KEY = 'skillup_session'

export const sessionStore = {
  load() {
    let raw = null
    try {
      raw = localStorage.getItem(KEY)
    } catch {
      // Try the tab-scoped copy when shared storage is unavailable.
    }
    if (!raw) {
      try {
        raw = sessionStorage.getItem(KEY)
      } catch {
        return null
      }
    }
    try {
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
  saveShared(session) {
    sessionStore.clear()
    try {
      localStorage.setItem(KEY, JSON.stringify(session))
    } catch {
      // Preserve the login in this tab if shared storage is unavailable.
      sessionStorage.setItem(KEY, JSON.stringify(session))
    }
  },
  saveToCurrentTab(session) {
    sessionStorage.setItem(KEY, JSON.stringify(session))
  },
  clear() {
    localStorage.removeItem(KEY)
    sessionStorage.removeItem(KEY)
  },
}
