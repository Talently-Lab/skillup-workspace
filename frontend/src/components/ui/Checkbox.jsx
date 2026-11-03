import { useId } from 'react'

export default function Checkbox({ label, className = '', ...props }) {
  const id = useId()

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <input
        id={id}
        type="checkbox"
        className="h-4 w-4 rounded border-border accent-primary"
        {...props}
      />
      <label htmlFor={id} className="text-xs text-ink">
        {label}
      </label>
    </div>
  )
}