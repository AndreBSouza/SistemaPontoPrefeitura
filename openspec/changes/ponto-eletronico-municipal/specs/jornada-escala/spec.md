## ADDED Requirements

### Requirement: Definição de jornadas de trabalho
O sistema SHALL permitir definir jornadas configuráveis por tenant, incluindo jornada fixa, flexível, 12x36, plantão e carga horária de magistério, com carga diária/semanal e tolerâncias.

#### Scenario: Criação de jornada 12x36
- **WHEN** o RH cria uma jornada do tipo 12x36 com horários e folgas
- **THEN** o sistema valida a consistência (12h trabalho / 36h descanso) e disponibiliza a jornada para atribuição

### Requirement: Intervalo intrajornada
O sistema SHALL contemplar intervalo intrajornada (ex.: almoço) na jornada, considerando-o na apuração das horas.

#### Scenario: Jornada com intervalo obrigatório
- **WHEN** uma jornada define intervalo mínimo e o servidor não registra a saída/retorno de intervalo
- **THEN** o sistema sinaliza ocorrência de intervalo não cumprido na apuração

### Requirement: Atribuição de escala ao vínculo
O sistema SHALL atribuir jornadas/escalas a vínculos por período de vigência, permitindo escalas rotativas e troca de turnos.

#### Scenario: Vigência de escala
- **WHEN** o RH atribui uma escala a um vínculo com data de início e fim
- **THEN** a apuração utiliza a escala vigente em cada dia do período

#### Scenario: Sobreposição de escalas
- **WHEN** o RH tenta atribuir uma escala que se sobrepõe a outra vigente para o mesmo vínculo
- **THEN** o sistema rejeita a atribuição e informa o conflito de vigência
