export function getErrorMessage(error) {
  const responseMessage = error.response?.data?.message ?? error.response?.data?.error
  if (typeof responseMessage === 'string') return responseMessage
  if (responseMessage && typeof responseMessage === 'object') {
    const messages = Object.values(responseMessage).filter((value) => typeof value === 'string')
    if (messages.length > 0) return messages.join(' ')
  }

  return error.message ?? 'Ocurrió un error inesperado.'
}
