# DPIA / RIPD — Ponto Municipal

Relatório de Impacto à Proteção de Dados Pessoais (RIPD/DPIA) do tratamento de
**biometria facial** e **geolocalização** no registro de ponto.

## 1. Controlador e operador
- **Controlador:** o ente municipal (cada tenant).
- **Operador:** o fornecedor do SaaS "Ponto Municipal".
- **Encarregado (DPO):** definido em contrato por ente.

## 2. Dados tratados
| Dado | Categoria | Finalidade |
|---|---|---|
| Nome, CPF, matrícula, e-mail | Pessoal | Identificação funcional |
| Geolocalização do registro | Pessoal | Validar local de trabalho (geofencing) |
| Biometria facial (template/hash) | **Sensível** (art. 5º, II, LGPD) | Autenticar o registro (antifraude) |

## 3. Base legal
- Cumprimento de obrigação legal e exercício regular de competência do poder público
  (controle de frequência — IN 008/2021 TCM-GO; Portaria MTP 671/2021 p/ celetistas).
- **Consentimento específico** para biometria quando exigido (registrado em `/api/lgpd/consentimento`).

## 4. Princípios e medidas
- **Minimização:** preferência por *template/embedding* facial on-device em vez da imagem crua.
- **Segurança:** cifragem em trânsito (TLS) e repouso; RLS por tenant; RBAC; trilha de auditoria imutável.
- **Retenção/descarte:** dados pessoais elimináveis são anonimizados sob solicitação
  (`/api/lgpd/titular/{id}/eliminar`); CPF e marcações são **retidos por obrigação legal**.
- **Direitos do titular:** acesso/exportação (`/api/lgpd/titular/{id}/exportar`) e eliminação.

## 5. Riscos e mitigações
| Risco | Mitigação |
|---|---|
| Vazamento de biometria | Minimização (hash), cifragem, acesso segregado e auditado |
| Uso indevido de geolocalização | Coleta restrita ao ato de registro; finalidade limitada |
| Reidentificação após eliminação | Anonimização irreversível dos campos pessoais elegíveis |
| Acesso indevido entre entes | Isolamento por RLS + filtro de aplicação por tenant |

## 6. Pendências antes do go-live
- Liveness/anti-spoofing validado; política de retenção por tipo de dado formalizada;
  contrato de operador + nomeação de DPO; teste de intrusão (pentest, task 10.2).

> Documento vivo — revisar a cada mudança no tratamento de dados sensíveis.
