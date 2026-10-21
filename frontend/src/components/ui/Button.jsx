import { Link } from 'react-router-dom'

const base =
  'inline-flex items-center justify-center gap-1 transition-colors disabled:cursor-not-allowed'

const sizes = {
  sm: 'px-4 py-2 text-sm font-medium',
  md: 'px-4 py-2 text-base font-semibold',
}

const variants = {
  primary:
    'rounded-button bg-primary text-white hover:bg-primary-hover disabled:bg-primary-disabled',
  secondary:
    'rounded-button border border-primary bg-white text-primary hover:bg-secondary-hover hover:text-primary-hover disabled:border-border disabled:bg-disabled disabled:text-muted',
  text: 'text-sm font-medium text-primary underline-offset-4 hover:text-primary-hover hover:underline disabled:text-muted disabled:no-underline',
}

export default function Button({
  to,
  variant = 'primary',
  size = 'md',
  className = '',
  children,
  ...props
}) {
  const sizeClasses = variant === 'text' ? '' : sizes[size]
  const classes = `${base} ${variants[variant]} ${sizeClasses} ${className}`

  if (to) {
    return (
      <Link to={to} className={classes} {...props}>
        {children}
      </Link>
    )
  }

  return (
    <button className={classes} {...props}>
      {children}
    </button>
  )
}