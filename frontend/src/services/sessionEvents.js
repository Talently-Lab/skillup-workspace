export const EXPIRATION_NOTICE_KEY = 'skillup_session_expired_notice'

const SESSION_EVENT_KEY = 'skillup_session_event'
const SESSION_CHANNEL_NAME = 'skillup-session'
let sessionEndHandled = false
let channel = null
const listeners = new Set()

function dispatchSessionEvent(message) {
  if (!message || typeof message !== 'object') return
  listeners.forEach((listener) => listener(message))
}

function getSessionChannel() {
  if (channel || typeof BroadcastChannel === 'undefined') return channel

  try {
    channel = new BroadcastChannel(SESSION_CHANNEL_NAME)
    channel.addEventListener('message', (event) => dispatchSessionEvent(event.data))
  } catch {
    channel = null
  }
  return channel
}

export function claimSessionEnd() {
  if (sessionEndHandled) return false
  sessionEndHandled = true
  return true
}

function publishSessionEvent(message) {
  const activeChannel = getSessionChannel()
  if (activeChannel) {
    activeChannel.postMessage(message)
    return
  }

  try {
    localStorage.setItem(SESSION_EVENT_KEY, JSON.stringify({
      ...message,
      id: `${Date.now()}-${Math.random()}`,
    }))
    // Keep the fallback message transient; authentication remains in sessionStorage.
    localStorage.removeItem(SESSION_EVENT_KEY)
  } catch {
    // The current tab still works locally if cross-tab storage is unavailable.
  }
}

export function publishSessionStarted(session) {
  publishSessionEvent({ type: 'session-started', session })
}

export function publishSessionEnded(reason) {
  publishSessionEvent({ type: 'session-ended', reason })
}

export function subscribeToSessionEvents(callback) {
  listeners.add(callback)
  getSessionChannel()

  const handleStorage = (event) => {
    if (event.key !== SESSION_EVENT_KEY || !event.newValue) return

    try {
      const message = JSON.parse(event.newValue)
      dispatchSessionEvent(message)
    } catch {
      // Ignore malformed cross-tab messages.
    }
  }

  window.addEventListener('storage', handleStorage)
  return () => {
    listeners.delete(callback)
    window.removeEventListener('storage', handleStorage)
  }
}

export function setSessionExpiredNotice() {
  try {
    sessionStorage.setItem(EXPIRATION_NOTICE_KEY, '1')
  } catch {
    // Authentication is still cleared and protected routes remain inaccessible.
  }
}
