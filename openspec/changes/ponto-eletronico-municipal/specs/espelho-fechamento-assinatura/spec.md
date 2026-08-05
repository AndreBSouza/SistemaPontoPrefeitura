## ADDED Requirements

### Requirement: Espelho de ponto mensal
O sistema SHALL gerar o espelho de ponto por vínculo e competência, consolidando marcações, ocorrências, abonos e saldos, disponível ao servidor e ao RH.

#### Scenario: Geração do espelho
- **WHEN** o RH gera o espelho de uma competência para um vínculo
- **THEN** o sistema apresenta marcações, ocorrências, abonos e totais do período

### Requirement: Ciência/assinatura eletrônica do servidor
O sistema SHALL registrar a ciência do servidor sobre o espelho por assinatura eletrônica, capturando evidências (data/hora, autenticação, dispositivo, hash do documento).

#### Scenario: Servidor dá ciência
- **WHEN** o servidor assina eletronicamente o espelho
- **THEN** o sistema registra a assinatura com as evidências e vincula o hash do documento assinado

#### Scenario: Recusa de ciência
- **WHEN** o servidor recusa dar ciência ao espelho
- **THEN** o sistema registra a recusa com data/hora e mantém o espelho disponível para tratamento pelo RH

### Requirement: Fechamento de competência
O sistema SHALL permitir o fechamento da competência, após o qual marcações e apurações do período MUST ficar bloqueadas para edição, admitindo apenas reabertura controlada e auditada.

#### Scenario: Bloqueio após fechamento
- **WHEN** o RH fecha a competência de um período
- **THEN** o sistema impede novas alterações nas marcações/apurações daquele período

#### Scenario: Reabertura auditada
- **WHEN** um administrador reabre uma competência fechada
- **THEN** o sistema registra a reabertura na trilha de auditoria com responsável e motivo
