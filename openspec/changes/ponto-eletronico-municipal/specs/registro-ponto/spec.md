## ADDED Requirements

### Requirement: Registro de ponto multicanal
O sistema SHALL permitir o registro de ponto (entrada/saída/intervalo) por aplicativo mobile, web e totem (PWA). Cada registro MUST receber um Número Sequencial de Registro (NSR) imutável e gerar comprovante ao servidor.

#### Scenario: Registro com comprovante
- **WHEN** o servidor efetua um registro válido pelo app
- **THEN** o sistema persiste o registro com NSR e disponibiliza o comprovante (recibo) ao servidor

#### Scenario: Imutabilidade do registro
- **WHEN** qualquer usuário tenta editar ou excluir um registro já efetivado
- **THEN** o sistema impede a alteração do registro original, admitindo apenas tratamento por ocorrência/justificativa auditável

### Requirement: Registro offline com sincronização
O sistema SHALL permitir o registro sem conexão, armazenando-o de forma cifrada no dispositivo e sincronizando ao reconectar, sem duplicar registros (idempotência).

#### Scenario: Registro sem internet
- **WHEN** o servidor registra o ponto sem conexão
- **THEN** o app grava o registro localmente e o marca como pendente de sincronização

#### Scenario: Sincronização sem duplicidade
- **WHEN** a conexão é restabelecida e o app sincroniza um registro pendente
- **THEN** o servidor consolida o registro uma única vez, mesmo que a sincronização seja reenviada

### Requirement: Validação por geolocalização (geofencing)
O sistema SHALL validar a localização do registro contra a(s) cerca(s) geográfica(s) do local de trabalho quando o geofencing estiver habilitado para o vínculo.

#### Scenario: Registro fora da cerca
- **WHEN** o servidor registra ponto fora da área permitida com geofencing obrigatório
- **THEN** o sistema bloqueia ou marca o registro como "fora de área" para tratamento, conforme a política do ente

### Requirement: Validação por biometria facial com prova de vida
O sistema SHALL validar a identidade no registro por reconhecimento facial com detecção de prova de vida (liveness), comparando contra a referência cadastrada. Imagens/biometria MUST ser tratadas como dado sensível (minimização e cifragem).

#### Scenario: Face não corresponde
- **WHEN** a verificação facial não corresponde à referência do servidor
- **THEN** o sistema recusa o registro biométrico e oferece fallback supervisionado conforme política do ente

#### Scenario: Tentativa de burlar com foto estática
- **WHEN** o liveness detecta ausência de prova de vida (ex.: foto de foto)
- **THEN** o sistema recusa o registro
