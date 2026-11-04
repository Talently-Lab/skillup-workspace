import { getErrorMessage } from './apiErrors'

export function isAlreadyEnrolled(error) {
  if (error.response?.status !== 409) return false
  const message = getErrorMessage(error)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('es')
  return message.includes('ya se encuentra inscripto') ||
    message.includes('ya se encuentra inscrito')
}

export function getPendingCourseId(locationState) {
  const courseId = locationState?.pendingEnrollment?.courseId
  if (typeof courseId !== 'string' && typeof courseId !== 'number') return null
  const normalizedId = String(courseId)
  return /^[1-9]\d*$/.test(normalizedId) ? normalizedId : null
}
