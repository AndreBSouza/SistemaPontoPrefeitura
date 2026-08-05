# Prompt do Stitch — Painel web (admin/gestor) — conceito "Livro-Ponto / Mesa de Registro"

> Cole no Stitch (modo Web/Desktop). Mesma família visual do app (registro oficial), mas adaptada
> a uma ferramenta densa e profissional — sem cara de dashboard genérico de IA. Há também, ao
> final, uma variante leve para a **tela pública do servidor** (bater ponto pela web).

---

## PROMPT (copie a partir daqui)

Desenhe um **painel administrativo web (desktop, 1440px)** chamado **Ponto Municipal**, usado por
**RH, gestores e controladoria** de prefeituras para administrar o ponto eletrônico dos
servidores. É uma ferramenta de trabalho densa em dados: priorize **clareza, leitura de tabelas e
confiança**, com uma identidade autoral (descrita abaixo). Não use o visual padrão de dashboard
SaaS nem clichês de template.

**Conceito / direção de arte: "Livro-Ponto / Mesa de Registro".** O painel é a mesa de um
**cartório digital**: livros de registro, dossiês, carimbos. Estética **tinta sobre papel**, com
tabelas em estilo livro-razão (pautas), números em monoespaçada e selos para status oficiais.
Sóbrio, governamental, eficiente — sem ser frio nem genérico.

**Paleta (use exatamente):**
- Papel (fundo de conteúdo): `#F7F4EE`; painéis/cartões em branco `#FFFFFF`.
- Tinta (texto/escuro e barra lateral): `#16202E`.
- Azul institucional (ação/links/seleção, gov.br): `#1351B4`.
- Selo (carimbo — status oficial "fechado/assinado", parcimônia): terracota `#9A3324`.
- Verde-registro `#1F6E5C` (ok/conforme) · Âmbar `#B7791F` (atenção) · Vermelho-tinta `#A52A2A`
  (pendência) — todos foscos, como tinta, **não** néon.
- Neutros: sépia-cinza `#6B6256` (secundário), fio/hairline `#E2DCCF`.

**Tipografia:** **Rawline/Inter** na interface; **IBM Plex Mono** em todos os números, horários,
NSR, totais e matrículas (assinatura visual — sensação de livro-razão e relógio de ponto).
Cabeçalhos com peso forte; corpo confortável (15–16px); tabelas com numerais tabulares.

**Layout:**
- **Barra lateral esquerda cor de tinta** (`#16202E`), navegação em estilo **abas de dossiê** com
  um pequeno brasão no topo; item ativo com marca de tinta e fundo levemente mais claro.
- **Barra de comando superior**: seletor de **ente** e de **competência** (mês) em mono, busca, e
  o usuário logado. Fina, com um fio inferior.
- **Conteúdo** sobre papel, com títulos de seção e **fios finos** organizando blocos. Respiro
  estruturado; nada de cards flutuando soltos com sombra pesada.

**Motivos visuais (assinatura):**
- **Tabelas-livro:** linhas separadas por fios finos (não zebra colorida), numerais em mono,
  cabeçalho discreto, status como **etiquetas de tinta** pequenas (não badges berrantes).
- **Selo circular de carimbo** para estados oficiais ("Competência fechada", "AFD assinado").
- **Comprovantes/AFD** exibidos como documento (mono, picote no topo).
- Métricas do painel como **estatística tipográfica** (número grande em mono + rótulo), com no
  máximo um sparkline fino — **sem** gráficos chamativos 3D/donut coloridos.

**Voz/microcopy:** institucional, direta e respeitosa. Ex.: "Competência 06/2026 — 142 servidores,
3 pendências." Botões com verbos claros: "Fechar competência", "Gerar AFD", "Aprovar".

**EVITE (para não parecer template/IA):** gradientes roxo/índigo; glassmorphism; dark-mode neon;
gráficos donut multicoloridos e "big number" com setinhas verdes/vermelhas genéricas; ícones
emoji; sombras pesadas em tudo; ilustrações 3D; heros com blob. **Prefira** tinta-sobre-papel,
fios finos, tabelas legíveis, mono nos números, etiquetas discretas, densidade calma.

Telas:

1. **Painel (visão do ente).** Cabeçalho "Prefeitura de [Cidade] — Competência 06/2026".
Faixa de estatística tipográfica: servidores ativos, registros no mês, pendências, % de
conformidade — números grandes em mono + rótulo, com um sparkline fino. Abaixo, um **livro de
pendências** (tabela): servidor, órgão, ocorrência, ação. Lateral com "Próximos fechamentos".

2. **Órgãos (regras de ponto).** Lista de órgãos (tabela-livro: nome, sigla, jornada padrão,
tolerância, banco de horas, geofence). Editor em painel lateral/modal estilo **ficha de
documento**: jornada padrão, tolerância, banco de horas (com teto), **geofence** (lat/long/raio +
política bloquear×sinalizar) e **"Exigir verificação na batida"** (interruptor). Salvar = carimbo
"Regras atualizadas".

3. **Servidores e vínculos.** Busca + tabela-livro (matrícula em mono, nome, regime, órgão,
situação). Detalhe do servidor como **dossiê**: dados, vínculos, **gerar código de ativação**
(exibido grande em mono + copiar), dispositivos ativos (listar/revogar).

4. **Espelho / Fechamento.** Seleção de servidor + competência. Espelho estilo **livro-ponto**:
dias em pauta, marcações e ocorrências (etiquetas de tinta), totais em mono. Ação "Fechar
competência" com selo "Competência fechada".

5. **Relatórios e Conformidade.** Geração de **AFD/AEJ** (Portaria 671) e relatórios. O AFD
aparece como **documento** (mono, picote, hash), com botão "Baixar .txt" e um selo de status de
assinatura (Assinado ICP-Brasil / Pendente). Quadro de conformidade (IN 008) como ficha.

6. **Auditoria.** Trilha de eventos em tabela-livro (data/hora em mono, ator, ação, recurso). Fios
finos, filtros discretos. Eventos sensíveis com etiqueta de tinta.

7. **(opcional) Folha/Integrações, Billing, Jornadas/Escalas** — seguindo a mesma linguagem de
tabela-livro e fichas de documento.

Mantenha em todas as telas: barra lateral cor de tinta, mono nos números, tabelas-livro com fios
finos, etiquetas/selos discretos, densidade calma e o padrão gov.br.

## (fim do prompt)

---

### Variante leve — tela pública do servidor na web (rota `/servidor`)
Para a página em que o **próprio servidor** bate ponto pela web (computador do trabalho ou
navegador do celular), use o **mesmo conceito do app** ("Carteira de Ponto"), porém numa única
tela centralizada e enxuta: fundo papel, cartão de documento, **botão-carimbo grande "REGISTRAR"**
(estampa um selo ao registrar), saudação "Olá, [Nome]", e — quando ainda não ativado — apenas o
campo do **código de ativação** em mono + "Ativar este navegador". Nada de barra lateral nem
tabelas; é só o ato de registrar, com a mesma estética tinta/selo/mono.
