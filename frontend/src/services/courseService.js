import api from './api'

export async function getCourses() {
  const { data } = await api.get('/courses')
  if (!Array.isArray(data)) {
    throw new Error('La respuesta del catálogo no tiene el formato esperado.')
  }
  return data
}

export async function getCourse(id) {
  const { data } = await api.get(`/courses/${id}`)
  if (!data || typeof data !== 'object') {
    throw new Error('La respuesta del curso no tiene el formato esperado.')
  }
  return data
}

export async function enrollInCourse(courseId) {
  const numericCourseId = Number(courseId)
  if (!Number.isSafeInteger(numericCourseId) || numericCourseId < 1) {
    throw new Error('El curso seleccionado no es válido.')
  }

  const { data, status } = await api.post('/enrollments', { courseId: numericCourseId })
  if (
    status !== 201 ||
    !data ||
    data.id == null ||
    String(data.courseId) !== String(numericCourseId)
  ) {
    throw new Error('No pudimos confirmar la inscripción. Revisá tus cursos antes de volver a intentarlo.')
  }
  return data
}

export async function getMyEnrollments() {
  const { data } = await api.get('/enrollments')
  if (!Array.isArray(data)) {
    throw new Error('La respuesta de tus inscripciones no tiene el formato esperado.')
  }
  return data
}

export async function getCourseFavorite(courseId) {
  const { data } = await api.get(`/courses/${courseId}/favorite`)
  if (!data || typeof data.favorited !== 'boolean') {
    throw new Error('La respuesta del estado de favorito no tiene el formato esperado.')
  }
  return data
}

export async function addCourseFavorite(courseId) {
  await api.put(`/courses/${courseId}/favorite`)
}

export async function removeCourseFavorite(courseId) {
  await api.delete(`/courses/${courseId}/favorite`)
}
