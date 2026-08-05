/**
 * HomePage — redireciona para o dashboard admin.
 * Mantido para compatibilidade com qualquer link externo para "/".
 */
import { Navigate } from 'react-router-dom'

export default function HomePage() {
  return <Navigate to="/dashboard" replace />
}
