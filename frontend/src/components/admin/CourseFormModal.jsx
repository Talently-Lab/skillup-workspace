import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  BookOpen,
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  ExternalLink,
  FileText,
  Image as ImageIcon,
  Layers,
  Layers3,
  LoaderCircle,
  Minus,
  Plus,
  Radio,
  Target,
  Trash2,
  UploadCloud,
  Video,
  X,
} from 'lucide-react'
import api from '../../services/api'
import { getErrorMessage } from '../../utils/apiErrors'
import {
  calculateScheduleSummary,
  formatMinutes,
  getRecordedContentMinutes,
  getSessionMinutes,
} from '../../utils/courseSchedule'

const OTHER_MODALITY_VALUE = '__other__'
const MODALITY_OPTIONS = {
  type: ['EN VIVO', 'GRABADO', 'HÍBRIDO'],
}
const MODALITY_VALUE_FIELDS = { type: 'modalityType' }
const COURSE_FORM_SECTIONS = [
  { key: 'general', label: 'Información general', icon: Layers },
  { key: 'planning', label: 'Modalidad y planificación', icon: CalendarDays },
  { key: 'academic', label: 'Programa académico', icon: FileText },
  { key: 'objectives', label: 'Objetivos', icon: Target },
  { key: 'modules', label: 'Módulos y Temas', icon: BookOpen },
]
const WEEK_DAYS = [
  { key: 'lunes', short: 'L', label: 'Lunes' },
  { key: 'martes', short: 'M', label: 'Martes' },
  { key: 'miercoles', short: 'X', label: 'Miércoles' },
  { key: 'jueves', short: 'J', label: 'Jueves' },
  { key: 'viernes', short: 'V', label: 'Viernes' },
  { key: 'sabado', short: 'S', label: 'Sábado' },
  { key: 'domingo', short: 'D', label: 'Domingo' },
]
const CATEGORY_SUGGESTIONS = [
  'Diseño',
  'Tecnología',
  'Marketing',
  'Negocios',
  'Desarrollo personal',
  'Idiomas',
]
const LEVEL_OPTIONS = ['Principiante', 'Intermedio', 'Avanzado']

function getSelectedDays(value) {
  const normalizeDay = (day) =>
    day
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
  const selectedDays = new Set(
    String(value ?? '')
      .toLowerCase()
      .replace(/\by\b/g, ',')
      .split(/[,;/]+/)
      .map(normalizeDay)
      .filter(Boolean),
  )
  return WEEK_DAYS
    .filter(({ key, label }) => selectedDays.has(key) || selectedDays.has(normalizeDay(label)))
    .map(({ key }) => key)
}

function formatSelectedDays(selectedDays) {
  return WEEK_DAYS.filter(({ key }) => selectedDays.includes(key))
    .map(({ label }) => label)
    .join(', ')
}

function getModalityChoice(value, options) {
  if (!value) return ''
  return options.includes(value) ? value : OTHER_MODALITY_VALUE
}

function getCustomModalityValue(value, options) {
  return value && !options.includes(value) ? value : ''
}

const emptyCourse = {
  title: '',
  category: '',
  level: '',
  summary: '',
  description: '',
  imageUrl: '',
  nextStartDate: '',
  durationHours: '',
  durationWeeks: '',
  recordedContentHours: '',
  recordedContentRemainderMinutes: '',
  scheduleDays: '',
  scheduleStartTime: '',
  scheduleEndTime: '',
  modalityType: '',
  modalityFrequency: '',
  modalityDuration: '',
  syllabusUrl: '',
  learningGoals: [''],
  objectives: [''],
  contentModules: [
    {
      title: '',
      topics: [''],
      isExpanded: true,
    },
  ],
}

function getSafeImageUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return ''

  try {
    const url = new URL(value.trim(), window.location.origin)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : ''
  } catch {
    return ''
  }
}

function getCourseFormValues(course) {
  const scheduleDays = course.schedule?.days ?? ''
  const recordedContentMinutes = getRecordedContentMinutes(course)

  return {
    ...emptyCourse,
    title: course.title ?? '',
    category: course.category ?? '',
    level: course.level ?? '',
    summary: course.summary ?? '',
    description: course.description ?? '',
    imageUrl: course.imageUrl ?? '',
    nextStartDate: course.nextStartDate ?? '',
    durationHours: course.durationHours ?? '',
    durationWeeks: course.durationWeeks ?? '',
    recordedContentHours: recordedContentMinutes
      ? Math.floor(recordedContentMinutes / 60)
      : '',
    recordedContentRemainderMinutes: recordedContentMinutes
      ? recordedContentMinutes % 60
      : '',
    scheduleDays,
    scheduleStartTime: course.schedule?.startTime ?? '',
    scheduleEndTime: course.schedule?.endTime ?? '',
    modalityType: course.modality?.type ?? '',
    modalityFrequency: course.modality?.frequency ?? '',
    modalityDuration: course.modality?.durationPerClass ?? '',
    syllabusUrl: course.syllabusUrl ?? '',
    learningGoals:
      Array.isArray(course.learningGoals) && course.learningGoals.length
        ? [...course.learningGoals]
        : [''],
    objectives:
      Array.isArray(course.objectives) && course.objectives.length
        ? [...course.objectives]
        : [''],
    contentModules: Array.isArray(course.contentModules)
      ? course.contentModules.map((module) => ({
          title: module.title ?? '',
          topics: Array.isArray(module.topics) && module.topics.length ? [...module.topics] : [''],
          isExpanded: true,
        }))
      : [],
  }
}

function CourseFormModal({ course, onClose, onSaved }) {
  const [values, setValues] = useState(() =>
    course ? getCourseFormValues(course) : emptyCourse,
  )
  const [selectedDays, setSelectedDays] = useState(() =>
    getSelectedDays(course?.schedule?.days ?? ''),
  )
  const [modalityChoices, setModalityChoices] = useState(() => ({
    type: getModalityChoice(course?.modality?.type ?? '', MODALITY_OPTIONS.type),
  }))
  const [customModality, setCustomModality] = useState(() => ({
    type: getCustomModalityValue(course?.modality?.type ?? '', MODALITY_OPTIONS.type),
  }))
  const [modalityErrors, setModalityErrors] = useState({})
  const [activeFormSection, setActiveFormSection] = useState('general')
  const [showCloseConfirmation, setShowCloseConfirmation] = useState(false)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveStatus, setSaveStatus] = useState('')
  const [savedAsDraft, setSavedAsDraft] = useState(false)
  const [savedResult, setSavedResult] = useState(null)
  const [imageFile, setImageFile] = useState(null)
  const [imagePreviewUrl, setImagePreviewUrl] = useState('')
  const [imageDragOver, setImageDragOver] = useState(false)
  const [syllabusFile, setSyllabusFile] = useState(null)
  const [imageError, setImageError] = useState('')
  const [imagePreviewError, setImagePreviewError] = useState('')
  const [currentImageFailed, setCurrentImageFailed] = useState(false)
  const [syllabusError, setSyllabusError] = useState('')
  const savingRef = useRef(false)
  const formScrollRef = useRef(null)
  const imageInputRef = useRef(null)
  const imagePreviewUrlRef = useRef('')
  const syllabusInputRef = useRef(null)
  const isEditing = Boolean(course?.id)
  const currentImageUrl = getSafeImageUrl(values.imageUrl)
  const currentSyllabusUrl = getSafeImageUrl(values.syllabusUrl)
  const currentSyllabusName = useMemo(() => {
    if (!currentSyllabusUrl) return ''
    try {
      return decodeURIComponent(new URL(currentSyllabusUrl).pathname.split('/').pop()) || 'Programa del curso.pdf'
    } catch {
      return 'Programa del curso.pdf'
    }
  }, [currentSyllabusUrl])
  const initialValues = useMemo(
    () => (course ? getCourseFormValues(course) : emptyCourse),
    [course],
  )
  const hasChanges = useMemo(() => {
    const normalizeModules = (modules) =>
      modules.map((module) => {
        const normalizedModule = { ...module }
        delete normalizedModule.isExpanded
        return normalizedModule
      })
    const current = { ...values, contentModules: normalizeModules(values.contentModules) }
    const initial = {
      ...initialValues,
      contentModules: normalizeModules(initialValues.contentModules),
    }
    return (
      Boolean(imageFile || syllabusFile) ||
      JSON.stringify(current) !== JSON.stringify(initial)
    )
  }, [imageFile, initialValues, syllabusFile, values])
  const modalityType = values.modalityType.normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toUpperCase()
  const isLiveModality = modalityType === 'EN VIVO'
  const isRecordedModality = modalityType === 'GRABADO'
  const isHybridModality = modalityType === 'HIBRIDO'
  const hasLiveSessions = Boolean(modalityChoices.type && !isRecordedModality)
  const recordedContentMinutes =
    Number(values.recordedContentHours || 0) * 60 +
    Number(values.recordedContentRemainderMinutes || 0)
  const scheduleSummary = calculateScheduleSummary({
    modality: modalityType,
    days: selectedDays,
    sessionStartTime: values.scheduleStartTime,
    sessionEndTime: values.scheduleEndTime,
    durationWeeks: values.durationWeeks,
    recordedContentMinutes,
  })
  const sessionMinutes = getSessionMinutes(values.scheduleStartTime, values.scheduleEndTime)
  const canPreserveLegacyDuration =
    isEditing &&
    !course?.schedule?.days &&
    !course?.schedule?.startTime &&
    !course?.schedule?.endTime &&
    course?.schedule?.totalDurationMinutes == null &&
    course?.schedule?.recordedContentMinutes == null
  const calculatedDurationMinutes =
    scheduleSummary.totalDurationMinutes ||
    (canPreserveLegacyDuration ? (Number(course?.durationHours) || 0) * 60 : 0)
  const calculatedDurationHours = Math.ceil(calculatedDurationMinutes / 60)
  const requestClose = useCallback(() => {
    if (savingRef.current || saveStatus) return
    if (hasChanges) {
      setShowCloseConfirmation(true)
      return
    }
    onClose()
  }, [hasChanges, onClose, saveStatus])
  useEffect(() => {
    const body = document.body
    const root = document.documentElement
    const scrollY = window.scrollY
    const previousBodyStyles = {
      position: body.style.position,
      top: body.style.top,
      width: body.style.width,
      overflow: body.style.overflow,
    }
    const previousRootOverflow = root.style.overflow

    body.style.position = 'fixed'
    body.style.top = `-${scrollY}px`
    body.style.width = '100%'
    body.style.overflow = 'hidden'
    root.style.overflow = 'hidden'

    return () => {
      body.style.position = previousBodyStyles.position
      body.style.top = previousBodyStyles.top
      body.style.width = previousBodyStyles.width
      body.style.overflow = previousBodyStyles.overflow
      root.style.overflow = previousRootOverflow
      window.scrollTo(0, scrollY)
    }
  }, [])
  useEffect(() => () => {
    if (imagePreviewUrlRef.current) URL.revokeObjectURL(imagePreviewUrlRef.current)
  }, [])

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key !== 'Escape') return
      event.preventDefault()
      if (saveStatus) return
      if (showCloseConfirmation) {
        setShowCloseConfirmation(false)
      } else {
        requestClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [requestClose, saveStatus, showCloseConfirmation])

  const syncActiveFormSection = useCallback(() => {
    const form = formScrollRef.current
    if (!form) return

    const activationLine = form.getBoundingClientRect().top + 100
    let activeSection = 'general'
    let closestTop = Number.NEGATIVE_INFINITY
    form.querySelectorAll('[data-course-section]').forEach((section) => {
      const top = section.getBoundingClientRect().top
      if (section.getBoundingClientRect().height > 0 && top <= activationLine && top > closestTop) {
        activeSection = section.dataset.courseSection
        closestTop = top
      }
    })
    setActiveFormSection(activeSection)
  }, [])

  useEffect(() => {
    const form = formScrollRef.current
    if (!form) return undefined

    form.addEventListener('scroll', syncActiveFormSection, { passive: true })
    syncActiveFormSection()
    return () => form.removeEventListener('scroll', syncActiveFormSection)
  }, [syncActiveFormSection])

  const navigateToFormSection = (sectionKey) => {
    setActiveFormSection(sectionKey)
    const form = formScrollRef.current
    const section = form?.querySelector(`[data-course-section="${sectionKey}"]`)
    if (form && section) {
      form.scrollTo({
        top:
          form.scrollTop +
          section.getBoundingClientRect().top -
          form.getBoundingClientRect().top -
          20,
        behavior: 'smooth',
      })
    }
  }

  const updateValue = (event) => {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    if (name === 'scheduleDays') setSelectedDays(getSelectedDays(value))
  }

  const toggleSelectedDay = (dayKey) => {
    const next = selectedDays.includes(dayKey)
      ? selectedDays.filter((day) => day !== dayKey)
      : WEEK_DAYS.filter(({ key }) => selectedDays.includes(key) || key === dayKey).map(({ key }) => key)
    setSelectedDays(next)
    setValues((currentValues) => ({
      ...currentValues,
      scheduleDays: formatSelectedDays(next),
    }))
  }

  const selectModalityChoice = (field, value) => {
    setModalityChoices((current) => ({ ...current, [field]: value }))
    setModalityErrors((current) => ({ ...current, [field]: '' }))
    if (value !== OTHER_MODALITY_VALUE) {
      setValues((current) => ({ ...current, [MODALITY_VALUE_FIELDS[field]]: value }))
    } else {
      setValues((current) => ({
        ...current,
        [MODALITY_VALUE_FIELDS[field]]: customModality[field],
      }))
    }
  }

  const updateCustomModality = (field, value) => {
    setCustomModality((current) => ({ ...current, [field]: value }))
    setValues((current) => ({
      ...current,
      [MODALITY_VALUE_FIELDS[field]]: value,
    }))
    if (value.trim()) setModalityErrors((current) => ({ ...current, [field]: '' }))
  }

  const updateListItem = (field, index, value) => {
    setValues((current) => ({
      ...current,
      [field]: current[field].map((item, itemIndex) => (itemIndex === index ? value : item)),
    }))
  }

  const updateModule = (moduleIndex, field, value) => {
    setValues((current) => ({
      ...current,
      contentModules: current.contentModules.map((module, index) =>
        index === moduleIndex ? { ...module, [field]: value } : module,
      ),
    }))
  }

  const updateModuleTopic = (moduleIndex, topicIndex, value) => {
    setValues((current) => ({
      ...current,
      contentModules: current.contentModules.map((module, index) =>
        index === moduleIndex
          ? {
              ...module,
              topics: module.topics.map((topic, itemIndex) =>
                itemIndex === topicIndex ? value : topic,
              ),
            }
          : module,
      ),
    }))
  }

  const processImageFile = (file) => {
    setImageError('')
    if (!file) return

    const extension = file.name.split('.').pop()?.toLowerCase()
    const validMimeTypes = {
      png: 'image/png',
      jpg: 'image/jpeg',
      jpeg: 'image/jpeg',
      webp: 'image/webp',
    }
    if (
      !validMimeTypes[extension] ||
      (file.type && validMimeTypes[extension] !== file.type)
    ) {
      setImageError('Selecciona una imagen PNG, JPG, JPEG o WEBP válida.')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setImageError('La imagen supera el límite de 5 MB.')
      return
    }

    if (imagePreviewUrlRef.current) URL.revokeObjectURL(imagePreviewUrlRef.current)
    imagePreviewUrlRef.current = URL.createObjectURL(file)
    setImagePreviewUrl(imagePreviewUrlRef.current)
    setImagePreviewError('')
    setCurrentImageFailed(false)
    setImageFile(file)
  }

  const selectImage = (event) => {
    processImageFile(event.target.files?.[0])
    event.target.value = ''
  }

  const dropImage = (event) => {
    event.preventDefault()
    setImageDragOver(false)
    processImageFile(event.dataTransfer.files?.[0])
  }

  const selectSyllabus = (event) => {
    const file = event.target.files?.[0]
    setSyllabusError('')
    if (!file) return

    if (
      file.name.split('.').pop()?.toLowerCase() !== 'pdf' ||
      (file.type && file.type !== 'application/pdf')
    ) {
      setSyllabusError('Selecciona un documento PDF válido.')
      event.target.value = ''
      return
    }
    if (file.size > 20 * 1024 * 1024) {
      setSyllabusError('El PDF supera el límite de 20 MB.')
      event.target.value = ''
      return
    }
    setSyllabusFile(file)
    event.target.value = ''
  }

  const clearImageSelection = () => {
    if (imagePreviewUrlRef.current) {
      URL.revokeObjectURL(imagePreviewUrlRef.current)
      imagePreviewUrlRef.current = ''
    }
    setImagePreviewUrl('')
    setImagePreviewError('')
    setImageFile(null)
    if (imageInputRef.current) imageInputRef.current.value = ''
  }

  const handleImagePreviewError = (event) => {
    if (imageFile) {
      if (event.currentTarget.src !== imagePreviewUrl) return
      setImagePreviewError('No se pudo mostrar la imagen seleccionada. Elige otro archivo.')
      return
    }
    if (event.currentTarget.src === currentImageUrl) setCurrentImageFailed(true)
  }

  const clearSyllabusSelection = () => {
    setSyllabusFile(null)
    if (syllabusInputRef.current) syllabusInputRef.current.value = ''
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (savingRef.current) return
    const createAsDraft =
      !isEditing && event.nativeEvent.submitter?.value === 'BORRADOR'
    const invalidField = formScrollRef.current?.querySelector(':invalid')
    if (invalidField) {
      invalidField.reportValidity()
      return
    }
    const customFields = ['type']
    const nextModalityErrors = Object.fromEntries(
      customFields
        .filter(
          (field) =>
            (field === 'type' && !modalityChoices[field]) ||
            (modalityChoices[field] === OTHER_MODALITY_VALUE &&
              !customModality[field].trim()),
        )
        .map((field) => [
          field,
          field === 'type' && !modalityChoices[field]
            ? 'Selecciona una modalidad o escribe un valor personalizado.'
            : 'Escribe un valor personalizado o selecciona una opción predefinida.',
        ]),
    )
    setModalityErrors(nextModalityErrors)
    if (Object.keys(nextModalityErrors).length > 0) {
      setError('')
      return
    }
    if (hasLiveSessions) {
      if (!selectedDays.length) {
        setError('Selecciona al menos un día para las sesiones en vivo.')
        return
      }
      if (!values.scheduleStartTime || !values.scheduleEndTime || !sessionMinutes) {
        setError('Indica una hora de finalización posterior a la hora de inicio.')
        return
      }
    }
    if (isRecordedModality && recordedContentMinutes <= 0) {
      setError('Indica una estimación positiva de horas o minutos de contenido.')
      return
    }
    if (isHybridModality && recordedContentMinutes <= 0) {
      setError('Indica una estimación positiva de horas o minutos de contenido grabado.')
      return
    }
    if (calculatedDurationMinutes <= 0) {
      setError('La carga horaria total debe ser mayor a cero.')
      return
    }
    const invalidModuleIndex = values.contentModules.findIndex(
      (module) => !module.title.trim(),
    )
    if (invalidModuleIndex >= 0) {
      setValues((current) => ({
        ...current,
        contentModules: current.contentModules.map((module, index) =>
          index === invalidModuleIndex ? { ...module, isExpanded: true } : module,
        ),
      }))
      setError(`Completa el título del módulo ${invalidModuleIndex + 1}.`)
      return
    }
    if (imageError || syllabusError) {
      setError('Corrige las validaciones de los archivos seleccionados antes de guardar.')
      return
    }

    setError('')
    savingRef.current = true
    setSaving(true)
    setSaveStatus('saving')

    try {
      const payload = {
        title: values.title.trim(),
        category: values.category.trim(),
        level: values.level.trim(),
        summary: values.summary.trim(),
        description: values.description.trim(),
        durationHours: calculatedDurationHours,
        durationWeeks: values.durationWeeks === '' ? null : Number(values.durationWeeks),
        nextStartDate: values.nextStartDate || null,
        ...(!isEditing ? { status: createAsDraft ? 'BORRADOR' : 'PUBLICADO' } : {}),
        schedule:
          isRecordedModality ||
          isLiveModality ||
          isHybridModality ||
          calculatedDurationMinutes > 0 ||
          values.scheduleDays.trim() ||
          values.scheduleStartTime.trim() ||
          values.scheduleEndTime.trim()
            ? {
              days: isRecordedModality ? '' : values.scheduleDays.trim(),
              startTime: isRecordedModality ? '' : values.scheduleStartTime.trim(),
              endTime: isRecordedModality ? '' : values.scheduleEndTime.trim(),
              ...(isRecordedModality || isHybridModality
                ? { recordedContentMinutes }
                : {}),
              totalDurationMinutes: calculatedDurationMinutes,
            }
            : null,
        modality: {
          type: values.modalityType.trim(),
          // Keep the existing API fields populated for consumers that still read them.
          frequency: hasLiveSessions
            ? `${scheduleSummary.sessionsPerWeek} clase${scheduleSummary.sessionsPerWeek === 1 ? '' : 's'} semanal${scheduleSummary.sessionsPerWeek === 1 ? '' : 'es'}`
            : isRecordedModality
              ? ''
              : values.modalityFrequency.trim(),
          durationPerClass: hasLiveSessions
            ? formatMinutes(scheduleSummary.sessionMinutes)
            : isRecordedModality
              ? ''
              : values.modalityDuration.trim(),
        },
        learningGoals: values.learningGoals.map((goal) => goal.trim()).filter(Boolean),
        objectives: values.objectives.map((objective) => objective.trim()).filter(Boolean),
        contentModules: values.contentModules.map((module, index) => ({
          moduleNumber: index + 1,
          title: module.title.trim(),
          topics: module.topics.map((topic) => topic.trim()).filter(Boolean),
        })),
      }

      const formData = new FormData()
      formData.append('course', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
      if (imageFile) formData.append('image', imageFile, imageFile.name)
      if (syllabusFile) formData.append('syllabus', syllabusFile, syllabusFile.name)

      if (isEditing) {
        const { data } = await api.put(`/courses/${course.id}`, formData)
        setSavedResult({ data, courseId: course.id })
      } else {
        const { data } = await api.post('/courses', formData)
        setSavedResult({ data, courseId: null })
        setSavedAsDraft(createAsDraft)
      }
      setSaveStatus('success')
    } catch (requestError) {
      setSaveStatus('')
      setError(getErrorMessage(requestError))
    } finally {
      savingRef.current = false
      setSaving(false)
    }
  }

  const finishSuccessfulSave = () => {
    if (!savedResult) return
    onSaved(savedResult.data, savedResult.courseId)
  }

  const inputClassName =
    'mt-1.5 w-full rounded-lg border border-border px-3 py-2.5 text-sm font-normal outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15'
  const labelClassName = 'block min-w-0 text-sm font-medium text-ink'

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-hidden bg-slate-900/60 p-3 backdrop-blur-sm sm:p-5"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) requestClose()
      }}
    >
      <section
        aria-labelledby="course-form-title"
        aria-modal="true"
        className="flex max-h-[94dvh] w-full max-w-5xl flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl"
        role="dialog"
        style={{ fontFamily: '"Plus Jakarta Sans", Inter, system-ui, sans-serif' }}
      >
        <div className="sticky top-0 z-10 shrink-0 border-b border-slate-200 bg-white">
          <div className="flex items-start justify-between gap-4 px-5 py-4 sm:px-8 sm:py-5">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <p className="text-xs font-bold uppercase tracking-wider text-primary">
                  Gestión de cursos
                </p>
                {hasChanges && (
                  <span className="rounded-full bg-amber-50 px-2 py-0.5 text-[11px] font-semibold text-amber-700">
                    Cambios sin guardar
                  </span>
                )}
              </div>
              <h2 id="course-form-title" className="mt-1 truncate text-xl font-bold tracking-tight text-ink sm:text-2xl">
                {isEditing ? `Editar curso${values.title ? `: ${values.title}` : ''}` : 'Crear curso'}
              </h2>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <button
                aria-label="Cerrar formulario"
                className="flex size-11 shrink-0 items-center justify-center rounded-xl text-muted transition hover:bg-slate-100 hover:text-ink disabled:opacity-50"
                disabled={saving}
                onClick={requestClose}
                type="button"
              >
                <X size={20} />
              </button>
            </div>
          </div>
          <div className="border-t border-slate-100 bg-slate-50/80 px-4 sm:px-7">
            <nav
              aria-label="Secciones del formulario"
              className="flex min-w-0 gap-1 overflow-x-auto py-2"
            >
              {COURSE_FORM_SECTIONS.map(({ key, label, icon: Icon }, index) => (
                <button
                  aria-current={activeFormSection === key ? 'step' : undefined}
                  className={`inline-flex min-h-10 shrink-0 items-center gap-2 rounded-xl border px-3 py-2 text-xs font-semibold transition ${
                    activeFormSection === key
                      ? 'border-blue-200 bg-white text-blue-700 shadow-sm'
                      : 'border-transparent text-slate-500 hover:bg-white/70 hover:text-slate-900'
                  }`}
                  key={`${key}-${index}`}
                  onClick={() => navigateToFormSection(key)}
                  type="button"
                >
                  <span className={`flex size-6 items-center justify-center rounded-full ${
                    activeFormSection === key ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-600'
                  }`}>
                    <Icon aria-hidden="true" size={13} />
                  </span>
                  <span>{index + 1}. </span>{label}
                </button>
              ))}
            </nav>
          </div>
        </div>

        <form className="flex min-h-0 flex-1 flex-col overflow-hidden" noValidate onSubmit={handleSubmit}>
          <div className="min-h-0 flex-1 overflow-x-hidden overflow-y-auto overscroll-contain" ref={formScrollRef}>
            <div className="px-5 py-6 sm:px-8">
            <div className="space-y-10">
            <section
              aria-labelledby="course-general-heading"
              className="scroll-mt-5 space-y-6"
              data-course-section="general"
            >
              <div className="flex flex-col gap-1 border-b border-slate-200 pb-3">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="text-base font-semibold text-slate-900" id="course-general-heading">
                    Información general
                  </h3>
                  <span className="text-xs font-medium text-slate-500">Paso 1 de 5</span>
                </div>
                <p className="text-xs text-slate-500">
                  Define el título, nivel de dificultad y portada visual para el catálogo.
                </p>
              </div>
              <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
                <label className={`${labelClassName} md:col-span-2`} htmlFor="title">
                  Nombre del curso <span className="text-red-500">*</span>
                  <input
                    className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                    id="title"
                    name="title"
                    onChange={updateValue}
                    placeholder="Ej. Diseño UX/UI Avanzado"
                    required
                    value={values.title}
                  />
                </label>
                <label className={labelClassName} htmlFor="category">
                  Categoría <span className="text-red-500">*</span>
                  <input
                    className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                    id="category"
                    list="course-category-suggestions"
                    name="category"
                    onChange={updateValue}
                    placeholder="Selecciona o escribe una categoría"
                    required
                    value={values.category}
                  />
                  <datalist id="course-category-suggestions">
                    {CATEGORY_SUGGESTIONS.map((category) => <option key={category} value={category} />)}
                  </datalist>
                </label>
                <label className={labelClassName} htmlFor="level">
                  Nivel del curso <span className="text-red-500">*</span>
                  <select
                    className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                    id="level"
                    name="level"
                    onChange={updateValue}
                    required
                    value={values.level}
                  >
                    <option value="">Selecciona el nivel</option>
                    {values.level && !LEVEL_OPTIONS.includes(values.level) && (
                      <option value={values.level}>{values.level}</option>
                    )}
                    {LEVEL_OPTIONS.map((level) => <option key={level} value={level}>{level}</option>)}
                  </select>
                </label>
                <div className="md:col-span-2">
                  <div className="flex items-center justify-between gap-3">
                    <label className="block text-sm font-semibold text-slate-800">
                      Imagen de portada del curso
                    </label>
                    <span className="text-xs text-slate-500">Recomendado: 1200 × 675 px (16:9)</span>
                  </div>
                  <input
                    accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
                    className="sr-only"
                    disabled={saving}
                    onChange={selectImage}
                    ref={imageInputRef}
                    type="file"
                  />
                  <div className="mt-2 grid items-stretch gap-4 lg:grid-cols-12">
                    <div
                      className={`flex min-h-56 flex-col justify-between rounded-xl border-2 border-dashed p-5 transition-colors lg:col-span-7 ${
                        imageDragOver
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-slate-300 bg-slate-50/70 hover:border-blue-400 hover:bg-blue-50/30'
                      }`}
                      onDragEnter={(event) => {
                        event.preventDefault()
                        setImageDragOver(true)
                      }}
                      onDragOver={(event) => event.preventDefault()}
                      onDragLeave={(event) => {
                        if (!event.currentTarget.contains(event.relatedTarget)) setImageDragOver(false)
                      }}
                      onDrop={dropImage}
                    >
                      <button
                        className="flex flex-1 flex-col items-center justify-center py-3 text-center"
                        disabled={saving}
                        onClick={() => imageInputRef.current?.click()}
                        type="button"
                      >
                        <span className="mb-2 flex size-12 items-center justify-center rounded-full bg-blue-100 text-blue-600">
                          <UploadCloud aria-hidden="true" size={24} />
                        </span>
                        <p className="text-sm font-semibold text-slate-800">
                          Haz clic para subir o arrastra la imagen aquí
                        </p>
                        <p className="mt-1 text-xs text-slate-500">
                          Formatos compatibles: PNG, JPG o WEBP (máximo 5 MB)
                        </p>
                      </button>
                      <div className="flex flex-wrap items-center justify-center gap-3 border-t border-slate-200 pt-3">
                        <button
                          className="inline-flex min-h-10 items-center gap-2 rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-xs font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 disabled:opacity-60"
                          disabled={saving}
                          onClick={() => imageInputRef.current?.click()}
                          type="button"
                        >
                          <ImageIcon aria-hidden="true" className="text-blue-600" size={15} />
                          Examinar archivos
                        </button>
                        {(imageFile || currentImageUrl) && (
                          <button
                            className="inline-flex min-h-10 items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-medium text-red-600 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-45"
                            disabled={saving || !imageFile}
                            onClick={clearImageSelection}
                            title={!imageFile ? 'La API actual no permite eliminar la portada sin reemplazarla.' : undefined}
                            type="button"
                          >
                            <Trash2 aria-hidden="true" size={14} />
                            Quitar portada
                          </button>
                        )}
                      </div>
                      <div className="mt-3 flex min-w-0 items-center justify-between gap-3 text-xs">
                        <span className="min-w-0 break-all font-medium text-slate-700">
                          {imageFile?.name ??
                            (currentImageUrl
                              ? currentImageFailed
                                ? 'Imagen actual no disponible'
                                : 'Imagen actual del curso'
                              : 'Sin imagen seleccionada')}
                        </span>
                        {imageFile ? (
                          <span className="shrink-0 rounded-full bg-amber-50 px-2 py-1 font-semibold text-amber-700">
                            Nueva · pendiente de guardar
                          </span>
                        ) : currentImageUrl ? (
                          <span className="shrink-0 rounded-full bg-emerald-50 px-2 py-1 font-semibold text-emerald-700">
                            Imagen actual
                          </span>
                        ) : null}
                      </div>
                      {imageError && <p className="mt-2 text-sm text-red-600" role="alert">{imageError}</p>}
                      {imagePreviewError && <p className="mt-2 text-sm text-red-600" role="alert">{imagePreviewError}</p>}
                    </div>
                    <div className="flex flex-col rounded-xl border border-slate-200 bg-white p-3.5 shadow-sm lg:col-span-5">
                      <div
                        aria-label={
                          imageFile
                            ? 'Vista previa de la nueva imagen pendiente de guardar'
                            : currentImageUrl && !currentImageFailed
                              ? 'Vista previa de la imagen actual del curso'
                              : 'Sin vista previa de imagen'
                        }
                        className="relative aspect-video w-full overflow-hidden rounded-lg border border-slate-100 bg-slate-100"
                        role="img"
                      >
                        {imageFile && !imagePreviewError ? (
                          <img alt="" aria-hidden="true" className="h-full w-full object-cover" onError={handleImagePreviewError} src={imagePreviewUrl} />
                        ) : !imageFile && currentImageUrl && !currentImageFailed ? (
                          <img alt="" aria-hidden="true" className="h-full w-full object-cover" onError={handleImagePreviewError} src={currentImageUrl} />
                        ) : (
                          <div className="flex h-full flex-col items-center justify-center gap-2 px-4 text-center text-slate-500">
                            <ImageIcon aria-hidden="true" className="text-slate-400" size={30} />
                            <span className="text-sm">
                              {currentImageFailed && !imageFile
                                ? 'No se pudo cargar la imagen actual'
                                : imagePreviewError || 'Aún no hay una imagen para mostrar'}
                            </span>
                          </div>
                        )}
                      </div>
                      <p className="mt-2 text-center text-xs font-medium text-slate-500" role="status">
                        {imageFile
                          ? 'Nueva imagen pendiente de guardar'
                          : currentImageUrl && !currentImageFailed
                            ? 'Imagen actual del curso'
                            : 'Sin imagen actual'}
                      </p>
                    </div>
                  </div>
                </div>
                <label className={`${labelClassName} md:col-span-2`} htmlFor="summary">
                  <span className="flex items-center justify-between gap-3">
                    <span>Resumen <span className="text-red-500">*</span></span>
                    <span className="text-xs font-normal tabular-nums text-slate-400">{values.summary.length} / 180 caracteres</span>
                  </span>
                  <span className="mt-0.5 block text-xs font-normal text-slate-500">
                    Aparecerá en la tarjeta del catálogo y resultados de búsqueda.
                  </span>
                  <textarea
                    className={`${inputClassName} min-h-20 rounded-xl px-3.5 py-2.5`}
                    id="summary"
                    name="summary"
                    onChange={updateValue}
                    required
                    rows={2}
                    value={values.summary}
                  />
                </label>
                <label className={`${labelClassName} md:col-span-2`} htmlFor="description">
                  <span className="flex items-center justify-between gap-3">
                    <span>Descripción completa del curso <span className="text-red-500">*</span></span>
                    <span className="text-xs font-normal tabular-nums text-slate-400">{values.description.length} caracteres</span>
                  </span>
                  <span className="mt-0.5 block text-xs font-normal text-slate-500">
                    Detalla el enfoque formativo, metodología y a quién va dirigido.
                  </span>
                  <textarea
                    className={`${inputClassName} min-h-28 rounded-xl px-3.5 py-2.5`}
                    id="description"
                    name="description"
                    onChange={updateValue}
                    required
                    rows={3}
                    value={values.description}
                  />
                </label>
              </div>
            </section>

            <section
              aria-labelledby="course-planning-heading"
              className="scroll-mt-5 space-y-6"
              data-course-section="planning"
            >
              <div className="flex flex-col gap-1 border-b border-slate-200 pb-3">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="text-base font-semibold text-slate-900" id="course-planning-heading">
                    Modalidad y planificación
                  </h3>
                  <span className="text-xs font-medium text-slate-500">Paso 2 de 5</span>
                </div>
                <p className="text-xs text-slate-500">
                  Primero elige cómo se impartirá el curso para mostrar solo la planificación necesaria.
                </p>
              </div>

              <div>
                <p className="mb-3 block text-sm font-semibold text-slate-800">
                  Modalidad <span className="text-red-500">*</span>
                </p>
                <div className="grid gap-3 sm:grid-cols-3">
                  {[
                    { value: 'EN VIVO', title: 'En Vivo', description: 'Sesiones en tiempo real con instructor', Icon: Radio },
                    { value: 'GRABADO', title: 'Grabado', description: 'Contenido para aprender a tu ritmo', Icon: Video },
                    { value: 'HÍBRIDO', title: 'Híbrido', description: 'Sesiones en vivo y contenido grabado', Icon: Layers3 },
                  ]
                    .filter(({ value }) => value !== 'HÍBRIDO' || (isEditing && modalityChoices.type === value))
                    .map(({ value, title, description, Icon }) => {
                    const selected = modalityChoices.type === value
                    return (
                      <button
                        aria-pressed={selected}
                        className={`relative flex min-h-28 flex-col items-start rounded-xl border p-4 text-left transition ${
                          selected
                            ? 'border-blue-500 bg-blue-50/60 ring-2 ring-blue-100'
                            : 'border-slate-200 bg-white hover:border-blue-300 hover:bg-slate-50'
                        }`}
                        key={value}
                        onClick={() => selectModalityChoice('type', value)}
                        type="button"
                      >
                        {selected && <CheckCircle2 aria-hidden="true" className="absolute right-3 top-3 text-blue-600" size={17} />}
                        <Icon aria-hidden="true" className={selected ? 'text-blue-600' : 'text-slate-500'} size={21} />
                        <span className="mt-2 text-sm font-semibold text-slate-900">{title}</span>
                        <span className="mt-1 text-xs leading-5 text-slate-500">{description}</span>
                      </button>
                    )
                  })}
                </div>
                <button
                  aria-pressed={modalityChoices.type === OTHER_MODALITY_VALUE}
                  className={`mt-3 rounded-lg border px-3 py-2 text-xs font-semibold transition ${
                    modalityChoices.type === OTHER_MODALITY_VALUE
                      ? 'border-blue-300 bg-blue-50 text-blue-700'
                      : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                  onClick={() => selectModalityChoice('type', OTHER_MODALITY_VALUE)}
                  type="button"
                >
                  Otra modalidad
                </button>
                {modalityChoices.type === OTHER_MODALITY_VALUE && (
                  <div className="mt-3 max-w-xl">
                    <label className="block text-xs font-semibold text-slate-700" htmlFor="modality-type-custom">
                      Nombre de la modalidad
                    </label>
                    <input
                      aria-invalid={Boolean(modalityErrors.type)}
                      className={`${inputClassName} rounded-xl border-blue-200 bg-blue-50/30`}
                      id="modality-type-custom"
                      onChange={(event) => updateCustomModality('type', event.target.value)}
                      placeholder="Ej. Semipresencial"
                      value={customModality.type}
                    />
                    {modalityErrors.type && <p className="mt-1 text-xs text-red-600" role="alert">{modalityErrors.type}</p>}
                  </div>
                )}
                {!modalityChoices.type && modalityErrors.type && (
                  <p className="mt-2 text-xs text-red-600" role="alert">{modalityErrors.type}</p>
                )}
              </div>

              <label className={`${labelClassName} block max-w-sm`} htmlFor="nextStartDate">
                Próxima fecha
                <input
                  className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                  id="nextStartDate"
                  name="nextStartDate"
                  onChange={updateValue}
                  type="date"
                  value={values.nextStartDate}
                />
                <span className="mt-1 block text-xs font-normal text-slate-500">
                  Se mostrará en la tarjeta pública del curso.
                </span>
              </label>

              {!modalityChoices.type ? (
                <p className="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-500">
                  Selecciona una modalidad para configurar la duración y el horario.
                </p>
              ) : (
                <div className="space-y-5 rounded-xl border border-slate-200 bg-slate-50/60 p-4 sm:p-5">
                  {hasLiveSessions && (
                    <>
                      <div className="grid gap-4 sm:grid-cols-2">
                        <label className={labelClassName} htmlFor="durationWeeks">
                          Duración estimada (semanas)
                          <input
                            className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                            id="durationWeeks"
                            min="1"
                            name="durationWeeks"
                            onChange={updateValue}
                            required
                            step="1"
                            type="number"
                            value={values.durationWeeks}
                          />
                        </label>
                        <div className="flex items-end">
                          <p className="text-xs leading-5 text-slate-500">
                            La duración en semanas se utiliza para calcular la carga de las sesiones programadas.
                          </p>
                        </div>
                      </div>

                      <div className="space-y-4 border-t border-slate-200 pt-4">
                        <div>
                          <h4 className="text-sm font-semibold text-slate-800">
                            {isHybridModality ? 'Sesiones en vivo' : 'Horario de clases'}
                          </h4>
                          <p className="mt-1 text-xs text-slate-500">Selecciona los días y el horario de cada sesión.</p>
                        </div>
                        <div>
                          <p className="mb-2 block text-sm font-medium text-slate-800">Días de clase</p>
                          <div aria-label="Selecciona los días de clase" className="flex flex-wrap gap-2" role="group">
                            {WEEK_DAYS.map(({ key, short, label }) => (
                              <button
                                aria-pressed={selectedDays.includes(key)}
                                className={`flex size-10 items-center justify-center rounded-full border text-sm font-semibold transition ${
                                  selectedDays.includes(key)
                                    ? 'border-blue-600 bg-blue-600 text-white'
                                    : 'border-slate-300 bg-white text-slate-600 hover:border-blue-300 hover:bg-blue-50'
                                }`}
                                key={key}
                                onClick={() => toggleSelectedDay(key)}
                                title={label}
                                type="button"
                              >
                                {short}
                              </button>
                            ))}
                          </div>
                        </div>
                        <div className="grid gap-4 sm:grid-cols-2">
                          <label className={labelClassName} htmlFor="scheduleStartTime">
                            Hora de inicio
                            <input
                              className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                              id="scheduleStartTime"
                              name="scheduleStartTime"
                              onChange={updateValue}
                              required
                              type="time"
                              value={values.scheduleStartTime}
                            />
                          </label>
                          <label className={labelClassName} htmlFor="scheduleEndTime">
                            Hora de finalización
                            <input
                              className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                              id="scheduleEndTime"
                              name="scheduleEndTime"
                              onChange={updateValue}
                              required
                              type="time"
                              value={values.scheduleEndTime}
                            />
                          </label>
                        </div>
                      </div>
                    </>
                  )}

                  {(isRecordedModality || isHybridModality) && (
                    <div className="space-y-4 border-t border-slate-200 pt-4">
                      <div>
                        <h4 className="text-sm font-semibold text-slate-800">Contenido grabado</h4>
                        <p className="mt-1 text-xs text-slate-500">Indica las horas estimadas de contenido.</p>
                      </div>
                      <div className="grid gap-4 sm:grid-cols-2">
                        <label className={labelClassName} htmlFor="recordedContentHours">
                          Horas estimadas
                          <input
                            className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                            id="recordedContentHours"
                            min="0"
                            onChange={(event) => setValues((current) => ({
                              ...current,
                              recordedContentHours: event.target.value,
                            }))}
                            required
                            step="1"
                            type="number"
                            value={values.recordedContentHours}
                          />
                        </label>
                        <label className={labelClassName} htmlFor="recordedContentRemainderMinutes">
                          Minutos adicionales (opcional)
                          <input
                            className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                            id="recordedContentRemainderMinutes"
                            max="59"
                            min="0"
                            onChange={(event) => setValues((current) => ({
                              ...current,
                              recordedContentRemainderMinutes: event.target.value,
                            }))}
                            step="1"
                            type="number"
                            value={values.recordedContentRemainderMinutes}
                          />
                        </label>
                      </div>
                    </div>
                  )}

                  {isRecordedModality && (
                    <label className={`${labelClassName} block border-t border-slate-200 pt-4`} htmlFor="durationWeeks">
                      Duración estimada en semanas (opcional)
                      <input
                        className={`${inputClassName} rounded-xl px-3.5 py-2.5`}
                        id="durationWeeks"
                        min="1"
                        name="durationWeeks"
                        onChange={updateValue}
                        step="1"
                        type="number"
                        value={values.durationWeeks}
                      />
                    </label>
                  )}

                  <div className="space-y-4 border-t border-slate-200 pt-4">
                    <h4 className="text-sm font-semibold text-slate-800">Resumen de programación</h4>
                    <div className="grid gap-3 sm:grid-cols-2">
                      {hasLiveSessions && (
                        <>
                          <div className="rounded-lg border border-slate-200 bg-white p-3">
                            <p className="text-xs font-medium text-slate-500">Frecuencia semanal</p>
                            <p className="mt-1 text-sm font-semibold text-slate-800">
                              {scheduleSummary.sessionsPerWeek
                                ? `${scheduleSummary.sessionsPerWeek} clase${scheduleSummary.sessionsPerWeek === 1 ? '' : 's'} por semana`
                                : 'Selecciona los días de clase'}
                            </p>
                          </div>
                          <div className="rounded-lg border border-slate-200 bg-white p-3">
                            <p className="text-xs font-medium text-slate-500">Duración por sesión</p>
                            <p className="mt-1 text-sm font-semibold text-slate-800">
                              {sessionMinutes ? formatMinutes(sessionMinutes) : 'Completa el horario'}
                            </p>
                          </div>
                        </>
                      )}
                      <div className="rounded-lg border border-slate-200 bg-white p-3">
                        <p className="text-xs font-medium text-slate-500">Duración estimada</p>
                        <p className="mt-1 text-sm font-semibold text-slate-800">
                          {values.durationWeeks
                            ? `${values.durationWeeks} ${Number(values.durationWeeks) === 1 ? 'semana' : 'semanas'}`
                            : isRecordedModality ? 'Sin especificar' : 'Indica las semanas'}
                        </p>
                      </div>
                      {(isRecordedModality || isHybridModality) && (
                        <div className="rounded-lg border border-slate-200 bg-white p-3">
                          <p className="text-xs font-medium text-slate-500">Contenido grabado</p>
                          <p className="mt-1 text-sm font-semibold text-slate-800">
                            {recordedContentMinutes > 0
                              ? formatMinutes(recordedContentMinutes)
                              : 'Indica las horas estimadas'}
                          </p>
                        </div>
                      )}
                      <div className="rounded-lg border border-blue-200 bg-blue-50 p-3 sm:col-span-2">
                        <p className="text-xs font-semibold text-blue-700">Carga horaria total</p>
                        <div className="mt-1 flex flex-wrap items-center gap-3">
                          <p className="text-base font-bold text-slate-900">
                            {calculatedDurationMinutes > 0
                              ? formatMinutes(calculatedDurationMinutes)
                              : 'Completa la información de la modalidad'}
                          </p>
                          {calculatedDurationHours > 0 && (
                            <label className="flex items-center gap-2 text-xs font-medium text-slate-600" htmlFor="durationHours">
                              Horas
                              <input
                                className="w-20 rounded-lg border border-blue-200 bg-white px-2.5 py-1.5 text-sm font-semibold text-slate-800"
                                id="durationHours"
                                readOnly
                                type="number"
                                value={calculatedDurationHours}
                              />
                            </label>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </section>

            <section
              aria-labelledby="course-syllabus-heading"
              className="scroll-mt-5 space-y-5"
              data-course-section="academic"
            >
              <div className="flex flex-col gap-1 border-b border-slate-200 pb-3">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="text-base font-semibold text-slate-900" id="course-syllabus-heading">
                    Programa académico
                  </h3>
                  <span className="text-xs font-medium text-slate-500">Paso 3 de 5</span>
                </div>
                <p className="text-xs text-slate-500">Documento oficial con el temario detallado del curso.</p>
              </div>
              <input
                accept=".pdf,application/pdf"
                className="sr-only"
                disabled={saving}
                onChange={selectSyllabus}
                ref={syllabusInputRef}
                type="file"
              />
              {syllabusFile || currentSyllabusUrl ? (
                <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-4 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex min-w-0 items-center gap-3.5">
                    <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-red-100 text-red-600">
                      <FileText aria-hidden="true" size={23} />
                    </span>
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="max-w-full truncate text-sm font-semibold text-slate-900">
                          {syllabusFile?.name ?? currentSyllabusName}
                        </p>
                        <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium ${
                          syllabusFile ? 'bg-amber-50 text-amber-700' : 'bg-emerald-50 text-emerald-700'
                        }`}>
                          {syllabusFile ? 'Pendiente de guardar' : <><CheckCircle2 aria-hidden="true" size={12} /> Cargado</>}
                        </span>
                      </div>
                      <p className="mt-1 text-xs text-slate-500">Documento PDF · máximo 20 MB</p>
                    </div>
                  </div>
                  <div className="flex shrink-0 flex-wrap items-center gap-2">
                    {currentSyllabusUrl && !syllabusFile && (
                      <a
                        className="inline-flex min-h-10 items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-blue-600 transition hover:bg-slate-50"
                        href={currentSyllabusUrl}
                        rel="noreferrer"
                        target="_blank"
                      >
                        <ExternalLink aria-hidden="true" size={14} />
                        Abrir PDF
                      </a>
                    )}
                    <button
                      className="inline-flex min-h-10 items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 disabled:opacity-60"
                      disabled={saving}
                      onClick={() => syllabusInputRef.current?.click()}
                      type="button"
                    >
                      <UploadCloud aria-hidden="true" size={14} />
                      Reemplazar
                    </button>
                    <button
                      aria-label={syllabusFile ? 'Quitar PDF seleccionado' : 'Eliminación de PDF no disponible'}
                      className="flex size-10 items-center justify-center rounded-lg text-slate-400 transition hover:bg-red-50 hover:text-red-600 disabled:cursor-not-allowed disabled:opacity-40"
                      disabled={saving || !syllabusFile}
                      onClick={clearSyllabusSelection}
                      title={!syllabusFile ? 'La API actual no permite eliminar el PDF sin reemplazarlo.' : undefined}
                      type="button"
                    >
                      <Trash2 aria-hidden="true" size={16} />
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  className="flex w-full flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300 bg-slate-50/70 p-6 text-center transition hover:border-blue-400 hover:bg-blue-50/30"
                  disabled={saving}
                  onClick={() => syllabusInputRef.current?.click()}
                  type="button"
                >
                  <span className="mb-2 flex size-11 items-center justify-center rounded-xl bg-red-100 text-red-600">
                    <FileText aria-hidden="true" size={22} />
                  </span>
                  <span className="text-sm font-semibold text-slate-800">Seleccionar programa en PDF</span>
                  <span className="mt-1 text-xs text-slate-500">PDF; máximo 20 MB.</span>
                </button>
              )}
              {syllabusError && <p className="text-sm text-red-600" role="alert">{syllabusError}</p>}
            </section>

            <section
              aria-labelledby="course-objectives-heading"
              className="scroll-mt-5 space-y-5"
              data-course-section="objectives"
            >
              <div className="flex flex-col gap-1 border-b border-slate-200 pb-3">
                <div className="flex items-center justify-between gap-3">
                  <h3 className="text-base font-semibold text-slate-900" id="course-objectives-heading">
                    Objetivos y competencias
                  </h3>
                  <span className="text-xs font-medium text-slate-500">Paso 4 de 5</span>
                </div>
                <p className="text-xs text-slate-500">Define los resultados de aprendizaje y las competencias específicas.</p>
              </div>
              <div className="grid gap-5 lg:grid-cols-2">
                {[
                  { field: 'learningGoals', heading: 'Objetivos de aprendizaje', placeholder: 'Escribe un objetivo de aprendizaje', description: 'Lo que el estudiante podrá lograr al finalizar.' },
                  { field: 'objectives', heading: 'Objetivos específicos', placeholder: 'Escribe un objetivo específico', description: 'Competencias concretas que abordará el curso.' },
                ].map(({ field, heading, placeholder, description }) => (
                  <article className="min-w-0 rounded-xl border border-slate-200 bg-white p-4 sm:p-5" key={field}>
                    <div className="flex items-start justify-between gap-3 border-b border-slate-100 pb-3">
                      <div>
                        <h4 className="text-sm font-semibold text-slate-900">{heading}</h4>
                        <p className="mt-1 text-xs text-slate-500">{description}</p>
                      </div>
                      <button
                        className="inline-flex min-h-9 shrink-0 items-center gap-1.5 rounded-lg border border-blue-200 px-3 py-1.5 text-xs font-semibold text-blue-700 transition hover:bg-blue-50"
                        onClick={() => setValues((current) => ({ ...current, [field]: [...current[field], ''] }))}
                        type="button"
                      >
                        <Plus aria-hidden="true" size={14} />
                        Agregar
                      </button>
                    </div>
                    <div className="space-y-3 pt-4">
                        {values[field].map((item, index) => (
                          <div className="flex min-w-0 items-start gap-2" key={`${field}-${index}`}>
                            <span aria-hidden="true" className="mt-3 flex size-6 shrink-0 items-center justify-center rounded-full bg-blue-50 text-xs font-semibold text-blue-700">
                              {index + 1}
                            </span>
                            <label className="sr-only" htmlFor={`${field}-${index}`}>{`${heading} ${index + 1}`}</label>
                            <textarea
                              className={`${inputClassName} mt-0 min-h-20 flex-1 rounded-xl`}
                              id={`${field}-${index}`}
                              onChange={(event) => updateListItem(field, index, event.target.value)}
                              placeholder={placeholder}
                              rows={2}
                              value={item}
                            />
                            <button
                              aria-label={`Eliminar ${heading.toLocaleLowerCase()} ${index + 1}`}
                              className="mt-1 flex size-10 shrink-0 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:border-red-200 hover:bg-red-50 hover:text-red-600"
                              onClick={() => setValues((current) => ({
                                ...current,
                                [field]:
                                  current[field].length === 1
                                    ? ['']
                                    : current[field].filter((_, itemIndex) => itemIndex !== index),
                              }))}
                              type="button"
                            >
                              <Minus aria-hidden="true" size={17} />
                            </button>
                          </div>
                        ))}
                    </div>
                  </article>
                ))}
              </div>
            </section>

            <section
              aria-labelledby="course-modules-heading"
              className="scroll-mt-5 space-y-5"
              data-course-section="modules"
            >
              <div className="flex flex-wrap items-start justify-between gap-3 border-b border-slate-200 pb-3">
                <div>
                  <div className="flex items-center gap-3">
                    <h3 className="text-base font-semibold text-slate-900" id="course-modules-heading">
                      Módulos y estructura de contenidos
                    </h3>
                    <span className="text-xs font-medium text-slate-500">Paso 5 de 5</span>
                  </div>
                  <p className="mt-1 text-xs text-slate-500">Organiza el programa en módulos y temas.</p>
                </div>
                <button
                  className="inline-flex min-h-10 items-center gap-1.5 rounded-lg border border-blue-200 px-3 py-2 text-sm font-semibold text-blue-700 transition hover:bg-blue-50"
                  onClick={() =>
                    setValues((current) => ({
                      ...current,
                      contentModules: [
                        ...current.contentModules,
                        {
                          title: '',
                          topics: [''],
                          isExpanded: true,
                        },
                      ],
                    }))
                  }
                  type="button"
                >
                  <Plus aria-hidden="true" size={16} />
                  Agregar módulo
                </button>
              </div>
              {values.contentModules.length === 0 && (
                <p className="rounded-xl border border-dashed border-slate-300 bg-white p-6 text-center text-sm text-slate-500">
                  Todavía no hay módulos. Usa «Agregar módulo» para empezar.
                </p>
              )}
              <div className="space-y-4">
                {values.contentModules.map((module, moduleIndex) => (
                  <article className="min-w-0 overflow-hidden rounded-xl border border-slate-200 bg-white" key={`module-${moduleIndex}`}>
                    <div className="flex items-center gap-3 border-b border-slate-100 p-3 sm:p-4">
                      <button
                        aria-controls={`module-content-${moduleIndex}`}
                        aria-expanded={module.isExpanded !== false}
                        className="flex min-h-11 min-w-0 flex-1 items-center gap-3 rounded-lg text-left"
                        onClick={() => updateModule(moduleIndex, 'isExpanded', module.isExpanded === false)}
                        type="button"
                      >
                        <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-sm font-bold text-blue-700">
                          {moduleIndex + 1}
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block text-xs font-medium text-slate-500">Módulo {moduleIndex + 1}</span>
                          <span className="block truncate text-sm font-semibold text-slate-900">
                            {module.title || 'Nuevo módulo'}
                          </span>
                        </span>
                        <span className="hidden shrink-0 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600 sm:inline-flex">
                          {module.topics.length} {module.topics.length === 1 ? 'tema' : 'temas'}
                        </span>
                        <ChevronDown
                          aria-hidden="true"
                          className={`shrink-0 text-muted transition-transform ${
                            module.isExpanded === false ? '-rotate-90' : ''
                          }`}
                          size={18}
                        />
                      </button>
                      <button
                        aria-label={`Eliminar módulo ${moduleIndex + 1}`}
                        className="flex size-10 shrink-0 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:border-red-200 hover:bg-red-50 hover:text-red-600"
                        onClick={() =>
                          setValues((current) => ({
                            ...current,
                            contentModules: current.contentModules.filter((_, index) => index !== moduleIndex),
                          }))
                        }
                        type="button"
                      >
                        <Trash2 size={17} />
                      </button>
                    </div>

                    {module.isExpanded !== false && (
                      <div className="space-y-4 p-4 sm:p-5" id={`module-content-${moduleIndex}`}>
                        <div className="grid min-w-0 gap-4">
                          <label className={labelClassName} htmlFor={`moduleTitle-${moduleIndex}`}>
                            Título del módulo
                            <input
                              className={`${inputClassName} rounded-xl`}
                              disabled={saving}
                              id={`moduleTitle-${moduleIndex}`}
                              onChange={(event) => updateModule(moduleIndex, 'title', event.target.value)}
                              required
                              value={module.title}
                            />
                          </label>
                        </div>
                        <div className="space-y-3 border-t border-slate-100 pt-4">
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <div>
                          <h4 className="text-sm font-semibold text-slate-900">Temas del módulo</h4>
                          <p className="mt-0.5 text-xs text-slate-500">Agrega los contenidos de este módulo.</p>
                        </div>
                        <button
                          className="inline-flex min-h-10 items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50"
                          onClick={() => updateModule(moduleIndex, 'topics', [...module.topics, ''])}
                          type="button"
                        >
                          <Plus aria-hidden="true" size={16} />
                          Agregar tema
                        </button>
                      </div>
                      {module.topics.map((topic, topicIndex) => (
                        <div className="flex min-w-0 items-start gap-2" key={`topic-${moduleIndex}-${topicIndex}`}>
                          <label className="sr-only" htmlFor={`topic-${moduleIndex}-${topicIndex}`}>
                            Tema {topicIndex + 1} del módulo {moduleIndex + 1}
                          </label>
                          <span aria-hidden="true" className="mt-2 flex size-6 shrink-0 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-600">
                            {topicIndex + 1}
                          </span>
                          <input
                            className={`${inputClassName} mt-0 min-h-11 flex-1 rounded-xl`}
                            id={`topic-${moduleIndex}-${topicIndex}`}
                            onChange={(event) => updateModuleTopic(moduleIndex, topicIndex, event.target.value)}
                            placeholder={`Tema ${topicIndex + 1}`}
                            value={topic}
                          />
                          <button
                            aria-label={`Eliminar tema ${topicIndex + 1} del módulo ${moduleIndex + 1}`}
                            className="flex size-11 shrink-0 items-center justify-center rounded-lg border border-slate-200 text-slate-500 transition hover:border-red-200 hover:bg-red-50 hover:text-red-600"
                            onClick={() => updateModule(moduleIndex, 'topics', module.topics.filter((_, index) => index !== topicIndex))}
                            type="button"
                          >
                            <Minus aria-hidden="true" size={17} />
                          </button>
                        </div>
                      ))}
                    </div>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            </section>

            {error && (
              <p className="rounded-lg bg-red-50 px-4 py-3 text-sm text-danger" role="alert">
                No se pudo guardar el curso: {error}
              </p>
            )}
            </div>
            </div>
          </div>

          <div className="flex shrink-0 flex-col-reverse gap-3 border-t border-border bg-white px-5 py-4 sm:flex-row sm:justify-end sm:px-8">
            <button
              className="min-h-11 rounded-lg border border-border px-5 py-2.5 text-sm font-semibold text-ink hover:bg-slate-50 disabled:opacity-60"
              disabled={saving}
              onClick={requestClose}
              type="button"
            >
              Cancelar
            </button>
            {!isEditing && (
              <button
                className="min-h-11 rounded-lg border border-amber-300 bg-amber-50 px-5 py-2.5 text-sm font-semibold text-amber-900 transition hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={saving}
                name="status"
                type="submit"
                value="BORRADOR"
              >
                Guardar borrador
              </button>
            )}
            <button
              className="min-h-11 rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
              disabled={saving}
              name="status"
              type="submit"
              value="PUBLICADO"
            >
              {isEditing ? 'Guardar cambios' : 'Crear curso'}
            </button>
          </div>
        </form>
      </section>
      {saveStatus && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/55 p-4 backdrop-blur-sm">
          <section
            aria-labelledby="course-save-status-title"
            aria-modal="true"
            className="w-full max-w-sm rounded-2xl border border-slate-200 bg-white p-7 text-center shadow-xl"
            role={saveStatus === 'success' ? 'dialog' : 'alertdialog'}
          >
            {saveStatus === 'saving' ? (
              <>
                <LoaderCircle
                  aria-hidden="true"
                  className="mx-auto animate-spin text-primary"
                  size={34}
                />
                <h3 className="mt-4 text-lg font-bold text-ink" id="course-save-status-title">
                  {isEditing ? 'Actualizando curso' : 'Creando curso'}
                </h3>
                <p aria-live="polite" className="mt-2 text-sm text-muted" role="status">
                  Estamos procesando la solicitud. Esto puede tardar unos momentos.
                </p>
              </>
            ) : (
              <>
                <CheckCircle2 aria-hidden="true" className="mx-auto text-emerald-600" size={38} />
                <h3 className="mt-4 text-lg font-bold text-ink" id="course-save-status-title">
                  {isEditing
                    ? 'Curso actualizado con éxito'
                    : savedAsDraft
                      ? 'Borrador guardado con éxito'
                      : 'Curso creado con éxito'}
                </h3>
                <p className="mt-2 text-sm text-muted">
                  {savedAsDraft
                    ? 'El curso quedó inactivo y no se mostrará a los estudiantes.'
                    : 'Los cambios se guardaron correctamente.'}
                </p>
                <button
                  autoFocus
                  className="mt-6 min-h-11 w-full rounded-lg bg-primary px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-primary-hover"
                  onClick={finishSuccessfulSave}
                  type="button"
                >
                  Continuar
                </button>
              </>
            )}
          </section>
        </div>
      )}
      {showCloseConfirmation && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-950/50 p-4 backdrop-blur-sm"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setShowCloseConfirmation(false)
          }}
          role="presentation"
        >
          <section
            aria-describedby="discard-course-changes-description"
            aria-labelledby="discard-course-changes-title"
            aria-modal="true"
            className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl"
            role="alertdialog"
          >
            <h3 className="text-lg font-bold text-ink" id="discard-course-changes-title">
              ¿Descartar cambios?
            </h3>
            <p
              className="mt-2 text-sm leading-6 text-muted"
              id="discard-course-changes-description"
            >
              Hay cambios sin guardar. Si cierras el formulario ahora, perderás el avance realizado.
            </p>
            <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                autoFocus
                className="min-h-11 rounded-lg border border-border px-4 py-2 text-sm font-semibold text-ink transition hover:bg-slate-50"
                onClick={() => setShowCloseConfirmation(false)}
                type="button"
              >
                Seguir editando
              </button>
              <button
                className="min-h-11 rounded-lg bg-danger px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-700"
                onClick={onClose}
                type="button"
              >
                Descartar cambios
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  )
}

export default CourseFormModal
