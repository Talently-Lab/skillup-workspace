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
  const { data } = await api.post('/enrollments', { courseId })
  return data
}
