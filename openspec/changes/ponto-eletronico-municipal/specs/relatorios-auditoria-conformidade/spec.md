## ADDED Requirements

### Requirement: Relatórios e dashboards de frequência
O sistema SHALL oferecer relatórios e dashboards de frequência (absenteísmo, atrasos, horas extras, banco de horas) por unidade/secretaria e período, com exportação (PDF/CSV).

#### Scenario: Relatório de absenteísmo por unidade
- **WHEN** o RH solicita o relatório de absenteísmo de uma secretaria em um período
- **THEN** o sistema apresenta os indicadores consolidados e permite exportar o resultado

### Requirement: Trilha de auditoria imutável
O sistema SHALL registrar trilha de auditoria imutável de ações relevantes (registro, tratamento, aprovação, fechamento, acesso a dados sensíveis), contendo autor, data/hora, origem e dado alterado.

#### Scenario: Registro de alteração de apuração
- **WHEN** um usuário trata uma ocorrência na apuração
- **THEN** o sistema grava na trilha quem alterou, quando, de onde e o antes/depois

#### Scenario: Trilha não editável
- **WHEN** qualquer usuário tenta alterar ou apagar um evento da trilha de auditoria
- **THEN** o sistema impede a operação

### Requirement: Geração de AFD e AEJ (Portaria MTP 671/2021)
Para vínculos celetistas em modo REP-P, o sistema SHALL gerar o Arquivo Fonte de Dados (AFD) e o Arquivo Eletrônico de Jornada (AEJ) no layout vigente, com integridade garantida por assinatura.

#### Scenario: Exportação do AFD
- **WHEN** o RH solicita o AFD de um período para um conjunto de vínculos celetistas
- **THEN** o sistema gera o arquivo no layout da Portaria 671 com NSR e assinatura de integridade

### Requirement: Conformidade IN 008/2021 (controle de frequência)
O sistema SHALL produzir evidências do controle de frequência exigido pela IN 008/2021 (Atos de Pessoal) para uso da Controladoria Interna.

#### Scenario: Evidência para o Controle Interno
- **WHEN** a controladoria solicita o relatório de controle de frequência de um período
- **THEN** o sistema fornece o relatório com registros, tratamentos e responsáveis, apto a compor o controle interno

### Requirement: Controles LGPD
O sistema SHALL registrar o consentimento para uso de biometria quando exigido, aplicar prazos de retenção/descarte e atender às solicitações de direitos do titular (acesso, correção, eliminação).

#### Scenario: Consentimento de biometria
- **WHEN** um servidor é habilitado para registro biométrico
- **THEN** o sistema registra o consentimento (ou base legal aplicável) antes de coletar a biometria

#### Scenario: Solicitação de eliminação de dado pessoal
- **WHEN** um titular solicita a eliminação de dados pessoais elegíveis
- **THEN** o sistema executa o descarte dos dados elegíveis preservando o que a lei exige reter e registra a operação
