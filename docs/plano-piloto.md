# Plano de Piloto — Ponto Municipal

Plano para o **piloto em 1 ente municipal**: escopo, critérios de sucesso, suporte e métricas.
Objetivo: validar o produto em produção real, com risco contido, antes do rollout geral.

> Premissa regulatória: aderência à **IN 008/2021 do TCM-GO**, suporte ao **REP-P
> (Portaria MTP 671/2021)** e à **LGPD** (ver `docs/dpia-ripd.md`). Nuvem: AWS sa-east-1.

---

## 1. Objetivo e duração

- **Objetivo:** comprovar que o Ponto Municipal registra frequência de forma confiável,
  legal e usável em um município real, gerando os relatórios exigidos pelo controle interno.
- **Duração:** **8 semanas** (2 de implantação + 6 de operação assistida).
- **Critério de encerramento:** atingir os critérios de sucesso (§4) por **2 ciclos de
  fechamento mensal** consecutivos sem incidente crítico.

---

## 2. Seleção do ente piloto

Perfil desejado:
- Porte **pequeno/médio** (≈ 200–800 servidores) — volume suficiente para validar, risco contido.
- Patrocínio do gestor de RH e do **Controle Interno** (sponsor).
- Diversidade de cenários: servidores estatutários e celetistas, mais de um local de trabalho
  (para validar **geofencing**), e ao menos um setor com escala/turno.
- Disposição para reuniões semanais e fornecimento de dados de teste.

---

## 3. Escopo

### Dentro do escopo
- Cadastro de servidores, lotações e escalas do ente.
- Marcação de ponto: **app mobile** (Android) com **biometria facial + geolocalização** e **web/totem**.
- Regras de jornada, tolerâncias, banco de horas e ocorrências.
- **Upload de atestado** médico (com fluxo de aprovação).
- Espelho de ponto e **relatórios para o Controle Interno (IN 008/2021)**.
- Integração de identidade via **OIDC/Keycloak** (SSO do ente, se houver).
- LGPD: termo de consentimento de biometria, exportação e eliminação de dados do titular.

### Fora do escopo (piloto)
- iOS (depende de build em macOS — fase posterior).
- Integração automática com a folha de pagamento (export manual no piloto).
- Multi-ente / federação (piloto é **1 tenant**).
- Customizações específicas não previstas no contrato de piloto.

---

## 4. Critérios de sucesso (go / no-go para rollout)

| # | Critério | Meta |
|---|---|---|
| C1 | Disponibilidade do serviço (mensal) | **≥ 99,5%** |
| C2 | Marcações registradas sem erro do usuário | **≥ 98%** das tentativas |
| C3 | Falsa rejeição biométrica (FRR) | **≤ 3%** |
| C4 | Falsa aceitação biométrica (FAR) | **≤ 0,1%** |
| C5 | Latência p95 do registro de ponto | **≤ 1,5 s** |
| C6 | Fechamento mensal gerado e aceito pelo Controle Interno | **2 ciclos** sem retrabalho manual relevante |
| C7 | Incidentes críticos (perda/duplicação de marcação) | **0** |
| C8 | Satisfação dos usuários (CSAT) | **≥ 4,0 / 5** |
| C9 | Conformidade LGPD (consentimento, exportação, eliminação) | **100%** dos fluxos funcionando |
| C10 | Tempo médio de resolução de chamado (sev2) | **≤ 1 dia útil** |

**No-go** se qualquer incidente crítico recorrente (C7), ou C1/C6/C9 abaixo da meta ao fim do piloto.

---

## 5. Modelo de suporte durante o piloto

- **Canais:** chat dedicado (horário comercial) + e-mail + telefone para sev1/sev2.
- **Operação assistida:** SRE/produto acompanham os 2 primeiros fechamentos junto ao RH.
- **SLA do piloto:**

| Severidade | Definição | 1ª resposta | Resolução alvo |
|---|---|---|---|
| Sev1 | Serviço indisponível / sem registrar ponto | 30 min | 4 h |
| Sev2 | Função crítica degradada (relatório, upload) | 2 h | 1 dia útil |
| Sev3 | Bug não bloqueante / dúvida | 1 dia útil | próximo release |

- **Plantão:** on-call durante a janela de fechamento mensal.
- **Treinamento:** sessão para RH/gestores (web) + material rápido para servidores (app).

---

## 6. Métricas e instrumentação

Coletadas continuamente e revisadas semanalmente:
- **Adoção:** % de servidores ativos, marcações/dia, canais usados (app vs web/totem).
- **Confiabilidade:** disponibilidade, erro 5xx, latência p50/p95, falhas de fila.
- **Biometria:** FRR/FAR, taxa de *liveness* reprovado, fallback para registro manual.
- **Negócio:** ocorrências por tipo, atestados enviados/aprovados, horas extras, ajustes manuais.
- **Suporte:** volume de chamados por categoria, TMA, reincidência.
- **LGPD:** nº de consentimentos, solicitações de exportação/eliminação atendidas no prazo.

Relatório semanal de piloto consolidando essas métricas + status dos critérios (§4).

---

## 7. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Baixa adesão dos servidores | Treinamento, comunicação do gestor, fallback web/totem |
| Falha biométrica em campo (luz, câmera) | Ajuste de threshold, registro manual auditado, suporte rápido |
| Conectividade ruim em alguns locais | Marcação offline com sincronização posterior |
| Resistência do Controle Interno | Validar o relatório IN 008/2021 cedo, com dados reais |
| Incidente com dado sensível (biometria) | Plano de resposta LGPD + DPO acionável (ver `docs/dpia-ripd.md`) |

---

## 8. Saída do piloto

- Reunião de **lições aprendidas** com o ente.
- Decisão **go / no-go** para rollout com base nos critérios (§4).
- Backlog priorizado de ajustes pré-rollout.
- Caso de sucesso / depoimento (com autorização do ente).

> Documento vivo — ajustar critérios e metas conforme aprendizados do piloto.
