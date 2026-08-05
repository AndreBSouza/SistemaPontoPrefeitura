import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { iniciarSessaoOidc } from './auth/oidc'
import './index.css'

// Rearma a renovação do token OIDC a cada carga da página (o UserManager vive em memória e se
// perde no reload pós-login). Não faz nada quando o OIDC está desligado ou não há sessão.
iniciarSessaoOidc()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
