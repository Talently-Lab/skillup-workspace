import axios from 'axios'
import api from './api'

function getErrorMessage(error) {
  return (
    error.response?.data?.message ??
    error.response?.data?.error ??
    error.message ??
    'No se pudo completar la carga del archivo.'
  )
}

export async function uploadCourseAsset(file, purpose) {
  const { data: authorization } = await api.post('/uploads/presign', {
    purpose,
    fileName: file.name,
    contentType: file.type,
    fileSize: file.size,
  })

  try {
    await axios.put(authorization.uploadUrl, file, {
      headers: authorization.requiredHeaders,
      timeout: 120000,
    })
  } catch (error) {
    throw new Error(`No se pudo subir el archivo a R2: ${getErrorMessage(error)}`, {
      cause: error,
    })
  }

  const { data } = await api.post('/uploads/complete', {
    objectKey: authorization.objectKey,
    purpose,
  })
  if (typeof data?.publicUrl !== 'string' || !data.publicUrl) {
    throw new Error('El backend no devolvió una referencia válida para el archivo.')
  }
  return data.publicUrl
}
