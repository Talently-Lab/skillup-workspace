const variants = {
  default: 'bg-secondary text-primary',
  inProgress: 'bg-secondary text-primary-hover',
  completed: 'bg-success/15 text-success',
  upcoming: 'bg-secondary text-primary/70',
}

export default function Badge({ variant = 'default', children, className = '' }) {
  return (
    <span
      className={`inline-block rounded-full px-3 py-1 text-xs font-medium ${variants[variant]} ${className}`}
    >
      {children}
    </span>
  )
}