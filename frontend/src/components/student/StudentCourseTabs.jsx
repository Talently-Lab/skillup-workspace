const COURSE_TABS = [
  { id: 'ACTIVE', label: 'Cursando' },
  { id: 'COMPLETED', label: 'Finalizado' },
]

export default function StudentCourseTabs({ activeTab, onChange }) {
  return (
    <div aria-label="Filtrar mis cursos" className="flex gap-1.5" role="group">
      {COURSE_TABS.map(({ id, label }) => (
        <button
          aria-pressed={activeTab === id}
          className={`min-h-7 rounded-full px-3 text-[11px] font-semibold transition lg:text-xs ${
            activeTab === id
              ? 'bg-primary text-white'
              : 'bg-white text-slate-600 ring-1 ring-inset ring-slate-200 hover:bg-slate-50'
          }`}
          key={id}
          onClick={() => onChange(id)}
          type="button"
        >
          {label}
        </button>
      ))}
    </div>
  )
}
