## ADDED Requirements

### Requirement: Tratamento de marcações e ocorrências
O sistema SHALL apurar as marcações contra a escala vigente e gerar ocorrências automáticas: atraso, saída antecipada, falta, hora extra e adicional noturno, conforme regras configuráveis do tenant.

#### Scenario: Atraso identificado
- **WHEN** o servidor registra entrada após a tolerância da jornada
- **THEN** o sistema gera a ocorrência de atraso com a duração calculada

#### Scenario: Adicional noturno
- **WHEN** há horas trabalhadas no período noturno definido
- **THEN** o sistema apura o adicional noturno conforme a regra do ente

### Requirement: Banco de horas e compensação
O sistema SHALL manter banco de horas por vínculo, creditando e debitando saldos conforme regras (limites, prazo de compensação/prescrição) configuráveis.

#### Scenario: Crédito de horas extras no banco
- **WHEN** o servidor cumpre horas além da jornada em regime de banco de horas
- **THEN** o sistema credita o saldo respeitando os limites configurados

#### Scenario: Prescrição de saldo
- **WHEN** um saldo do banco de horas atinge o prazo de compensação sem ser usado
- **THEN** o sistema trata o saldo conforme a regra definida (expira ou converte) e registra o evento

### Requirement: Abonos e justificativas com aprovação
O sistema SHALL permitir que o servidor solicite abono/justificativa (falta, atraso, licença, atestado) com anexo de documento, e MUST submeter a solicitação a workflow de aprovação pela chefia/RH.

#### Scenario: Solicitação com atestado
- **WHEN** o servidor envia uma justificativa de falta com atestado anexado
- **THEN** o sistema registra a solicitação como pendente e a roteia para aprovação da chefia

#### Scenario: Aprovação ajusta a apuração
- **WHEN** a chefia aprova uma justificativa de falta
- **THEN** o sistema atualiza a apuração do dia, neutralizando a ocorrência de falta e mantendo o histórico

#### Scenario: Rejeição de solicitação
- **WHEN** a chefia rejeita uma justificativa
- **THEN** o sistema mantém a ocorrência original e notifica o servidor com o motivo
