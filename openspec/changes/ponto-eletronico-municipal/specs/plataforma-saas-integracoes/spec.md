## ADDED Requirements

### Requirement: Onboarding de tenant
O sistema SHALL permitir o provisionamento de um novo ente (tenant) com seus parâmetros iniciais (dados do ente, organograma básico, perfis e administrador), isolando seus dados desde a criação.

#### Scenario: Provisionamento de novo ente
- **WHEN** o operador cria um novo ente e define o administrador inicial
- **THEN** o sistema provisiona o tenant isolado e habilita o acesso do administrador

### Requirement: Planos e faturamento (assinatura)
O sistema SHALL associar cada ente a um plano (ex.: por servidor ativo/mês) e SHALL apurar o consumo para faturamento, registrando o ciclo de cobrança.

#### Scenario: Apuração de servidores ativos
- **WHEN** encerra-se o ciclo de faturamento de um ente
- **THEN** o sistema calcula a quantidade de servidores ativos no período e gera os dados de cobrança do plano

### Requirement: Integração com folha de pagamento
O sistema SHALL exportar os insumos de frequência apurados (faltas, horas extras, adicionais, abonos) para o sistema de folha do ente, por conector ou arquivo padrão (CSV/posicional).

#### Scenario: Exportação para folha
- **WHEN** o RH exporta a apuração fechada de uma competência
- **THEN** o sistema gera o arquivo/integração no formato configurado para o sistema de folha do ente

### Requirement: Integração com eSocial
Para eventos aplicáveis (vínculos celetistas), o sistema SHALL disponibilizar os dados de jornada necessários ao eSocial conforme o leiaute vigente.

#### Scenario: Dados de jornada para eSocial
- **WHEN** o RH solicita os dados de jornada para um período de vínculos celetistas
- **THEN** o sistema disponibiliza os dados no formato exigido pelo eSocial

### Requirement: Notificações
O sistema SHALL enviar notificações (push e e-mail) para lembretes de registro, pendências de aprovação e alertas de inconsistência, respeitando as preferências do usuário.

#### Scenario: Pendência de aprovação notificada
- **WHEN** uma solicitação de justificativa fica pendente para uma chefia
- **THEN** o sistema notifica a chefia sobre a pendência
