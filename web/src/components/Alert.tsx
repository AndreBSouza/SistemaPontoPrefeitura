import { ReactNode } from 'react'
import { ApiError } from '../api/client'

type AlertVariant = 'info' | 'success' | 'danger' | 'warning'

const ICONS: Record<AlertVariant, string> = {
  info: 'ℹ️',
  success: '✅',
  danger: '❌',
  warning: '⚠️',
}

const LABELS: Record<AlertVariant, string> = {
  info: 'Informação',
  success: 'Sucesso',
  danger: 'Erro',
  warning: 'Atenção',
}

interface AlertProps {
  variant: AlertVariant
  children: ReactNode
}

export function Alert({ variant, children }: AlertProps) {
  return (
    <div
      className={`br-alert br-alert--${variant}`}
      role="alert"
      aria-label={LABELS[variant]}
    >
      <span className="br-alert__icon" aria-hidden="true">{ICONS[variant]}</span>
      <div>{children}</div>
    </div>
  )
}

/** Exibe mensagem de erro de ApiError ou Error genérico */
export function ErrorAlert({ error }: { error: unknown }) {
  const msg =
    error instanceof ApiError
      ? error.message
      : error instanceof Error
      ? error.message
      : 'Ocorreu um erro inesperado.'
  return <Alert variant="danger">{msg}</Alert>
}
