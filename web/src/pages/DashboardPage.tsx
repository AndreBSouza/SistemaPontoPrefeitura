import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Breadcrumb } from '../components/Breadcrumb'
import { api } from '../api/client'
import type { LotacaoResponse, ServidorResponse, JornadaResponse } from '../api/types'

interface DashStats {
  orgaos: number
  servidores: number
  jornadas: number
}

export default function DashboardPage() {
  const [stats, setStats] = useState<DashStats>({ orgaos: 0, servidores: 0, jornadas: 0 })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      try {
        const [orgaos, servidores, jornadas] = await Promise.allSettled([
          api.get<LotacaoResponse[]>('/api/lotacoes'),
          api.get<ServidorResponse[]>('/api/servidores'),
          api.get<JornadaResponse[]>('/api/jornadas'),
        ])
        setStats({
          orgaos: orgaos.status === 'fulfilled' ? orgaos.value.length : 0,
          servidores: servidores.status === 'fulfilled' ? servidores.value.length : 0,
          jornadas: jornadas.status === 'fulfilled' ? jornadas.value.length : 0,
        })
      } finally {
        setLoading(false)
      }
    }
    void load()
  }, [])

  const QUICK_LINKS = [
    { to: '/orgaos', icon: '🏛️', label: 'Órgãos' },
    { to: '/servidores', icon: '👥', label: 'Servidores' },
    { to: '/jornadas', icon: '🕐', label: 'Jornadas' },
    { to: '/escalas', icon: '📅', label: 'Escalas' },
    { to: '/atestados', icon: '📋', label: 'Atestados' },
    { to: '/espelho', icon: '📊', label: 'Espelho' },
    { to: '/banco-horas', icon: '⏱️', label: 'Banco de Horas' },
    { to: '/relatorios', icon: '📈', label: 'Relatórios' },
    { to: '/conformidade', icon: '✔️', label: 'Conformidade' },
    { to: '/totem', icon: '📟', label: 'Totem' },
  ]

  return (
    <>
      <Breadcrumb items={[{ label: 'Início' }]} />
      <h1 className="page-title">Dashboard</h1>
      <p className="page-subtitle">Visão geral do sistema de ponto eletrônico</p>

      {/* Stat cards */}
      <div className="stat-grid">
        <div className="stat-card">
          <p className="stat-card__label">Órgãos / Lotações</p>
          <p className="stat-card__value">{loading ? '…' : stats.orgaos}</p>
          <p className="stat-card__sub">cadastrados</p>
        </div>
        <div className="stat-card">
          <p className="stat-card__label">Servidores</p>
          <p className="stat-card__value">{loading ? '…' : stats.servidores}</p>
          <p className="stat-card__sub">ativos</p>
        </div>
        <div className="stat-card">
          <p className="stat-card__label">Jornadas</p>
          <p className="stat-card__value">{loading ? '…' : stats.jornadas}</p>
          <p className="stat-card__sub">configuradas</p>
        </div>
        <div className="stat-card">
          <p className="stat-card__label">Competência atual</p>
          <p className="stat-card__value" style={{ fontSize: 20 }}>
            {new Date().toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}
          </p>
        </div>
      </div>

      {/* Atalhos rápidos */}
      <div className="br-card">
        <div className="br-card__header">
          <h2 className="br-card__title">Acesso rápido</h2>
        </div>
        <div className="quick-links">
          {QUICK_LINKS.map((l) => (
            <Link key={l.to} to={l.to} className="quick-link">
              <span className="quick-link__icon" aria-hidden="true">{l.icon}</span>
              {l.label}
            </Link>
          ))}
        </div>
      </div>
    </>
  )
}
