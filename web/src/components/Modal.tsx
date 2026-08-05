import { ReactNode, useEffect, useRef } from 'react'

interface ModalProps {
  title: string
  children: ReactNode
  footer?: ReactNode
  onClose: () => void
}

const SELETOR_FOCAVEL =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'

export function Modal({ title, children, footer, onClose }: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null)
  // id único por instância (evita colisão se dois modais coexistirem)
  const tituloId = useRef(`modal-title-${Math.random().toString(36).slice(2, 9)}`).current
  // onClose costuma ser uma arrow inline (nova identidade a cada render do pai). Guardar em ref
  // permite rodar o efeito UMA vez por abertura — senão, um re-render do pai (ex.: o timer que
  // limpa a mensagem de sucesso) refocaria o primeiro campo no meio da digitação.
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    const gatilho = document.activeElement as HTMLElement | null
    const el = dialogRef.current

    // Move o foco PARA DENTRO do diálogo ao abrir (a11y): primeiro campo ou o próprio diálogo.
    const primeiroCampo = el?.querySelector<HTMLElement>('input, select, textarea')
    ;(primeiroCampo ?? el)?.focus()

    // Trava o scroll do fundo enquanto o modal está aberto.
    const overflowAnterior = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        // No Firefox, Esc para fechar o dropdown de um <select> borbulha até aqui — fechar o
        // modal nesse caso descartaria o formulário inteiro que o usuário estava preenchendo.
        if (e.target instanceof HTMLSelectElement) return
        onCloseRef.current()
        return
      }
      // Focus trap: Tab/Shift+Tab circulam dentro do diálogo, não escapam para o fundo.
      if (e.key === 'Tab' && el) {
        const itens = Array.from(el.querySelectorAll<HTMLElement>(SELETOR_FOCAVEL))
        if (itens.length === 0) {
          e.preventDefault()
          return
        }
        const primeiro = itens[0]
        const ultimo = itens[itens.length - 1]
        if (e.shiftKey && document.activeElement === primeiro) {
          e.preventDefault()
          ultimo.focus()
        } else if (!e.shiftKey && document.activeElement === ultimo) {
          e.preventDefault()
          primeiro.focus()
        }
      }
    }
    document.addEventListener('keydown', handleKey)

    return () => {
      document.removeEventListener('keydown', handleKey)
      document.body.style.overflow = overflowAnterior
      gatilho?.focus?.() // devolve o foco ao elemento que abriu o modal
    }
  }, [])

  return (
    <div
      className="modal-overlay"
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div
        className="modal"
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={tituloId}
        tabIndex={-1}
      >
        <div className="modal__header">
          <h2 className="modal__title" id={tituloId}>{title}</h2>
          <button
            className="modal__close"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            ×
          </button>
        </div>
        <div className="modal__body">{children}</div>
        {footer && <div className="modal__footer">{footer}</div>}
      </div>
    </div>
  )
}
