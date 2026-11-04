const WEEK_DAY_KEYS = [
  'domingo',
  'lunes',
  'martes',
  'miercoles',
  'jueves',
  'viernes',
  'sabado',
]

function normalizeText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
}

export function parseScheduleDays(value) {
  const tokens = normalizeText(value)
    .replace(/\by\b/g, ',')
    .split(/[,;/]+/)
    .map((day) => day.trim())
    .filter(Boolean)

  return [...new Set(tokens.filter((day) => WEEK_DAY_KEYS.includes(day)))]
}

export function getSessionMinutes(start, end) {
  if (!start || !end) return 0
  const [startHour, startMinute] = start.split(':').map(Number)
  const [endHour, endMinute] = end.split(':').map(Number)
  if ([startHour, startMinute, endHour, endMinute].some(Number.isNaN)) return 0

  const minutes = endHour * 60 + endMinute - (startHour * 60 + startMinute)
  return Math.max(0, minutes)
}

export function calculateScheduleSummary({
  modality,
  days,
  sessionStartTime,
  sessionEndTime,
  durationWeeks,
  recordedContentMinutes = 0,
}) {
  const normalizedModality = normalizeText(modality).toUpperCase()
  const sessionMinutes = getSessionMinutes(sessionStartTime, sessionEndTime)
  const sessionsPerWeek = days.length
  const weeks = Number(durationWeeks) || 0
  const liveMinutes =
    sessionMinutes * sessionsPerWeek * (Number.isInteger(weeks) ? weeks : 0)
  const contentMinutes = Number(recordedContentMinutes) || 0
  const totalDurationMinutes =
    normalizedModality === 'GRABADO'
      ? contentMinutes
      : liveMinutes + (normalizedModality === 'HIBRIDO' ? contentMinutes : 0)

  return {
    sessionMinutes,
    sessionsPerWeek,
    weeklyMinutes: sessionMinutes * sessionsPerWeek,
    liveMinutes,
    totalDurationMinutes,
  }
}

export function getRecordedContentMinutes(course) {
  const savedContentValue = course.schedule?.recordedContentMinutes
  if (savedContentValue !== null && savedContentValue !== undefined && savedContentValue !== '') {
    const savedContentMinutes = Number(savedContentValue)
    if (Number.isFinite(savedContentMinutes) && savedContentMinutes >= 0) {
      return savedContentMinutes
    }
  }

  const modality = normalizeText(course.modality?.type).toUpperCase()
  const legacyTotalMinutes =
    Number(course.schedule?.totalDurationMinutes) ||
    (Number(course.durationHours) || 0) * 60

  if (modality === 'GRABADO') return legacyTotalMinutes
  if (modality !== 'HIBRIDO') return 0

  const liveMinutes = calculateScheduleSummary({
    modality: 'EN VIVO',
    days: parseScheduleDays(course.schedule?.days),
    sessionStartTime: course.schedule?.startTime,
    sessionEndTime: course.schedule?.endTime,
    durationWeeks: course.durationWeeks,
  }).liveMinutes

  return Math.max(0, legacyTotalMinutes - liveMinutes)
}

export function formatMinutes(minutes) {
  const safeMinutes = Math.max(0, Number(minutes) || 0)
  const hours = Math.floor(safeMinutes / 60)
  const remainingMinutes = safeMinutes % 60
  return [
    hours ? `${hours} ${hours === 1 ? 'hora' : 'horas'}` : '',
    remainingMinutes ? `${remainingMinutes} min` : '',
  ].filter(Boolean).join(' ') || '0 min'
}
