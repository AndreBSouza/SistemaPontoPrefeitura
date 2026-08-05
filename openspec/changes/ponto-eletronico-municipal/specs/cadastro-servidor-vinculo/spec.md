## ADDED Requirements

### Requirement: Cadastro de servidor
O sistema SHALL manter o cadastro de servidores com dados de identificação (nome, CPF, matrícula) e dados funcionais. O CPF MUST ser único por tenant.

#### Scenario: Cadastro com CPF duplicado
- **WHEN** o RH cadastra um servidor com CPF já existente no ente
- **THEN** o sistema rejeita o cadastro e informa a duplicidade

#### Scenario: Cadastro válido
- **WHEN** o RH informa dados obrigatórios válidos e salva
- **THEN** o sistema cria o servidor e o disponibiliza para vinculação de jornada

### Requirement: Múltiplos vínculos por servidor
O sistema SHALL permitir que um servidor possua mais de um vínculo/matrícula (ex.: professor com dois cargos), cada vínculo com regime (estatutário, celetista, comissionado), cargo e carga horária próprios.

#### Scenario: Servidor com dois vínculos
- **WHEN** o RH adiciona um segundo vínculo a um servidor existente
- **THEN** o sistema registra o vínculo separadamente, permitindo jornada e apuração independentes por vínculo

### Requirement: Lotação e organograma
O sistema SHALL associar cada vínculo a uma lotação (secretaria/unidade) dentro do organograma do ente, sustentando a hierarquia de aprovação.

#### Scenario: Definição da chefia para aprovação
- **WHEN** um vínculo é lotado em uma unidade que possui chefia definida
- **THEN** as solicitações desse servidor são roteadas para a chefia da unidade

### Requirement: Importação de servidores em lote
O sistema SHALL permitir importação inicial de servidores por planilha ou integração com a folha, reportando erros por linha sem interromper as demais.

#### Scenario: Importação com linhas inválidas
- **WHEN** o RH importa um arquivo com algumas linhas inválidas
- **THEN** o sistema importa as linhas válidas e gera um relatório com os erros das linhas rejeitadas
