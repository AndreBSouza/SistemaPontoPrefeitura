## ADDED Requirements

### Requirement: Isolamento de dados por tenant
O sistema SHALL isolar os dados de cada ente (tenant) de modo que nenhum usuário acesse dados de outro ente. Toda entidade persistida MUST conter identificação de tenant e o acesso MUST ser filtrado pelo tenant do usuário autenticado (via RLS no banco e contexto de aplicação).

#### Scenario: Usuário tenta acessar dado de outro ente
- **WHEN** um usuário autenticado no ente A requisita um registro pertencente ao ente B
- **THEN** o sistema retorna "não encontrado/negado" e registra a tentativa na trilha de auditoria

#### Scenario: Consulta sempre escopada ao tenant
- **WHEN** um usuário lista servidores
- **THEN** o sistema retorna apenas servidores do tenant do usuário, sem vazamento de outros entes

### Requirement: Autenticação e perfis de acesso (RBAC)
O sistema SHALL autenticar usuários via OIDC (Keycloak) e SHALL aplicar perfis: servidor, gestor/chefia, RH, controladoria, administrador do ente e operador (super admin). Cada perfil MUST ter permissões específicas; ações não autorizadas MUST ser bloqueadas.

#### Scenario: Servidor sem permissão administrativa
- **WHEN** um usuário com perfil "servidor" tenta abrir o cadastro de jornadas
- **THEN** o sistema nega o acesso e não exibe a funcionalidade

#### Scenario: RH acessa apuração
- **WHEN** um usuário com perfil "RH" acessa a apuração de frequência
- **THEN** o sistema permite o acesso conforme as permissões do perfil

### Requirement: MFA para perfis administrativos
O sistema SHALL exigir segundo fator de autenticação (MFA) para perfis administrativos (RH, controladoria, administrador do ente e operador).

#### Scenario: Login administrativo exige MFA
- **WHEN** um usuário com perfil administrativo realiza login com usuário e senha corretos
- **THEN** o sistema exige a confirmação do segundo fator antes de conceder a sessão

### Requirement: Federação de login gov.br (opcional)
O sistema SHALL permitir, quando habilitado pelo ente, autenticação federada via gov.br mantendo o vínculo do usuário ao tenant.

#### Scenario: Login via gov.br habilitado
- **WHEN** o ente habilita gov.br e o servidor escolhe entrar com gov.br
- **THEN** o sistema autentica via gov.br e associa a sessão ao tenant correto
